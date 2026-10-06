terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = ">= 5.0, < 7.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
}

resource "aws_vpc" "main" {
  cidr_block = "10.42.0.0/20"
  enable_dns_support = true
  enable_dns_hostnames = true

  tags = {
    Name = "houselink"
  }
}

resource "aws_subnet" "app" {
  vpc_id = aws_vpc.main.id
  cidr_block = "10.42.1.0/24"

  tags = {
    Name = "houselink-app"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "houselink"
  }
}

resource "aws_route_table" "public" {
  vpc_id         = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "houselink-public"
  }
}

resource "aws_route_table_association" "app" {
  subnet_id = aws_subnet.app.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "web" {
  name = "houselink-web"
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "houselink-web"
  }
}

resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.web.id
  cidr_ipv4 = "0.0.0.0/0"
  from_port = 80
  to_port = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.web.id
  cidr_ipv4 = "0.0.0.0/0"
  from_port = 443
  to_port = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "outbound" {
  security_group_id = aws_security_group.web.id
  cidr_ipv4 = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_iam_role" "ec2_ssm" {
  name = "houselink-ec2-ssm"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })
}
정
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2_ssm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "houselink-ec2"
  role = aws_iam_role.ec2_ssm.name
}

data "aws_ssm_parameter" "al2023_arm" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

resource "aws_instance" "app" {
  ami = data.aws_ssm_parameter.al2023_arm.value
  instance_type = "t4g.small" // 12년 31일에 변경 예정
  subnet_id = aws_subnet.app.id
  vpc_security_group_ids = [aws_security_group.web.id]
  iam_instance_profile = aws_iam_instance_profile.ec2.name

  associate_public_ip_address = false

  root_block_device {
    volume_type = "gp3"
    volume_size = 8
    encrypted = true
  }

  credit_specification {
    cpu_credits = "standard"
  }

  metadata_options {
    http_tokens = "required"
  }

  depends_on = [aws_route_table_association.app]

  tags = {
    Name = "houselink"
  }
}

resource "aws_eip_association" "app" {
  allocation_id = "eipalloc-0ad7a4247d3542d98"
  instance_id = aws_instance.app.id
}