-- 초기 스키마. 엔티티(JPA)에서 뽑은 MySQL DDL에 조회용 인덱스를 더했다.
-- 이후 변경은 V2, V3 … 새 파일로 추가한다(이 파일은 고치지 않는다).

create table app_user (
    id binary(16) not null,
    생성일 datetime(6),
    `익명 유저 키` varchar(255) not null,
    `유저 상태` enum ('ACTIVE','INACTIVE') not null,
    primary key (id),
    constraint uk_app_user_anon_key unique (`익명 유저 키`)
) engine=InnoDB;

create table household (
    id binary(16) not null,
    생성일 datetime(6),
    created_by binary(16) not null,
    name varchar(100) not null,
    primary key (id),
    constraint fk_household_created_by foreign key (created_by) references app_user (id)
) engine=InnoDB;

create table household_member (
    household_id binary(16) not null,
    user_id binary(16) not null,
    joined_at datetime(6) not null,
    nickname varchar(20) not null,
    role enum ('MEMBER','OWNER') not null,
    primary key (household_id, user_id),
    constraint fk_household_member_household foreign key (household_id) references household (id),
    constraint fk_household_member_user foreign key (user_id) references app_user (id)
) engine=InnoDB;
-- 내가 속한 공간 목록(GET /me)
create index idx_household_member_user on household_member (user_id);

create table invitation (
    id binary(16) not null,
    생성일 datetime(6),
    household_id binary(16) not null,
    token_hash varchar(64) not null,
    max_uses integer not null,
    used_count integer not null,
    expires_at datetime(6) not null,
    revoked_at datetime(6),
    version bigint not null,
    primary key (id),
    constraint uk_invitation_token_hash unique (token_hash)
) engine=InnoDB;
create index idx_invitation_household on invitation (household_id);

create table item (
    id binary(16) not null,
    생성일 datetime(6),
    household_id binary(16) not null,
    name varchar(100) not null,
    category enum ('CLEANING','KITCHEN','LAUNDRY','OTHER','TOILETRIES') not null,
    unit varchar(20) not null,
    quantity integer not null,
    low_stock_threshold integer not null,
    version bigint not null,
    primary key (id)
) engine=InnoDB;
-- 공간의 물품 목록
create index idx_item_household on item (household_id);

create table stock_change (
    id binary(16) not null,
    생성일 datetime(6),
    item_id binary(16) not null,
    actor_user_id binary(16) not null,
    actor_nickname varchar(20) not null,
    before_quantity integer not null,
    after_quantity integer not null,
    primary key (id)
) engine=InnoDB;
-- 물품의 변경 이력(최근순)
create index idx_stock_change_item_created on stock_change (item_id, 생성일);

create table alert_preference (
    household_id binary(16) not null,
    user_id binary(16) not null,
    enabled bit not null,
    primary key (household_id, user_id)
) engine=InnoDB;

create table low_stock_event (
    id binary(16) not null,
    생성일 datetime(6),
    household_id binary(16) not null,
    item_id binary(16) not null,
    item_version bigint not null,
    primary key (id)
) engine=InnoDB;
create index idx_low_stock_event_household on low_stock_event (household_id);

create table out_of_stock_alert (
    id binary(16) not null,
    생성일 datetime(6),
    household_id binary(16) not null,
    item_id binary(16) not null,
    item_name varchar(255) not null,
    actor_user_id binary(16) not null,
    due_at datetime(6) not null,
    reminders_sent integer not null,
    primary key (id)
) engine=InnoDB;
-- 보낼 때가 된 알림 찾기, 물품·공간 단위 삭제
create index idx_out_of_stock_alert_due on out_of_stock_alert (due_at);
create index idx_out_of_stock_alert_item on out_of_stock_alert (item_id);
create index idx_out_of_stock_alert_household on out_of_stock_alert (household_id);
