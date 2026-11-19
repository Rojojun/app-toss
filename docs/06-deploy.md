# Debian 홈서버 배포

백엔드는 Dockerfile로 이미지를 만들고 `scripts/deploy-home.sh`가 `docker run`으로 실행한다. PostgreSQL 17은 홈서버의 기존 `postgres` 컨테이너를 사용한다. Compose는 사용하지 않는다. AWS Terraform과 state는 별도 정리 시점까지 보존한다.

## 서버 파일과 설정

현재 서버 경로:

- `/home/rojojun/houselink/houselink/.env`
- `/home/rojojun/houselink/certs/toss.crt`
- `/home/rojojun/houselink/certs/toss.key`

`.env.example`을 기준으로 기존 `.env`의 아래 항목을 수정한다. 실제 비밀번호와 JWT 키, 인증서는 Git에 넣지 않는다. 기존 JWT 키는 유지한다.

```dotenv
DB_URL=jdbc:postgresql://postgres:5432/family_share
DB_USERNAME=familyshare
DB_PASSWORD=PostgreSQL에서_설정한_값
TOSS_APP_NAME=실제_콘솔_appName
SPRING_PROFILES_ACTIVE=toss-mtls
TOSS_MTLS_CERTIFICATE=file:/certs/toss.crt
TOSS_MTLS_PRIVATE_KEY=file:/certs/toss.key
JWT_SECRET_BASE64=기존_값_유지
ALERT_TEMPLATE_SET_CODE=
SLACK_WEBHOOK_URL=
```

`MYSQL_ROOT_PASSWORD`, `MTLS_DIR`은 새 배포에서 사용하지 않는다. `ALERT_TEMPLATE_SET_CODE`가 비어 있으면 실제 토스 알림은 발송하지 않는다. `SLACK_WEBHOOK_URL`은 가입 알림을 사용할 때 설정한다.

Runner는 해당 `.env`와 인증서를 읽을 수 있는 사용자로 실행한다. 기본 경로는 runner 사용자의 `$HOME/houselink` 기준이므로 다른 계정을 사용하면 아래 Actions 변수를 지정한다. 컨테이너도 배포 사용자와 같은 UID/GID로 실행해 권한 600인 키를 읽는다. 디렉터리는 700, 비밀 파일은 600으로 보관한다.

## 최초 수동 실행

저장소를 준비한 홈서버에서:

```bash
docker build -t houselink:local .
APP_PULL=false bash scripts/deploy-home.sh houselink:local
```

`APP_PULL=false`는 로컬 이미지를 사용하고 레지스트리에서 가져오지 않는 설정이다. 실행 후 상태를 확인한다.

```bash
curl --fail http://127.0.0.1:8080/api/v1/health
```

배포 스크립트는 `houselink` 네트워크를 만들고 기존 `postgres` 컨테이너를 연결한다. 앱만 교체하며 DB 컨테이너·볼륨·데이터는 삭제하지 않는다. 기본 앱 이름은 `houselink-app`, 포트는 `127.0.0.1:8080`이다. 외부 접근은 별도 HTTPS 프록시/터널에서 이 포트로 연결한다. 토스 앱의 `VITE_API_BASE`도 새 HTTPS 주소로 변경해 번들을 다시 빌드해야 한다.

## GitHub Actions

기존 CI는 유지한다. PostgreSQL 17 서비스에서 Flyway/JPA 검증을 추가했다. `source-is-main`과 기존 `ci` 필수 체크 이름도 유지하므로 기존 브랜치 룰셋을 바꿀 필요가 없다.

배포 흐름:

```text
feature PR → main: 백엔드 테스트·프론트 타입/빌드 검사
main PR → prod-back: 기존 검사 및 source-is-main
prod-back push: 백엔드 테스트 → 이미지 빌드/GHCR 업로드 → 홈서버 교체/상태 확인
```

`.github/workflows/deploy-backend.yml`은 Linux amd64/arm64 이미지를 `ghcr.io/rojojun/app-toss-backend:<commit SHA>`에 올린다. 배포할 때는 태그 대신 해당 빌드의 digest를 사용한다. GHCR 인증은 workflow의 `GITHUB_TOKEN`으로 처리한다. 기존 패키지가 별도로 생성되어 있으면 패키지 Settings → Manage Actions access에서 이 저장소에 접근 권한을 부여해야 한다.

### 최초 한 번 필요한 설정

1. GitHub 저장소 Settings → Actions → Runners → New self-hosted runner에서 홈서버 OS/CPU에 맞는 명령을 실행한다. 추가 라벨은 `houselink`로 지정한다. runner를 서비스로 설치하고 시작한다.
2. runner 계정이 `docker info`를 실행할 수 있어야 한다. Debian에 `curl`, `flock`도 필요하다. runner는 외부 HTTPS 연결로 작업을 수신하므로 SSH 배포는 사용하지 않는다.
3. Settings → Environments에 `home-production`을 생성한다. 경로 기본값이 맞으면 추가 변수나 서버 비밀값을 GitHub에 등록할 필요가 없다. 필요하면 환경 Variables에 `APP_ENV_FILE`, `APP_CERTS_DIR`, `APP_HOST_PORT`를 지정한다.
4. 변경을 main에 반영한 뒤 main → prod-back PR을 병합한다. 수동 실행은 Actions → Deploy backend to home server → Run workflow에서 `prod-back`을 선택한다. 다른 브랜치에서는 배포하지 않는다.

CI는 GitHub 호스팅 runner, 실제 교체 job만 홈서버 runner에서 실행한다. Terraform apply/destroy, SSH, AWS 인증은 workflow에 포함하지 않는다.

## 실패 시 동작과 제한

- 교체 전 `.env`, 인증서 읽기 권한, PostgreSQL 상태, 새 이미지 다운로드 및 컨테이너 생성을 확인한다.
- 이전 앱을 중지하고 새 앱 하나를 실행한다. 알림 스케줄러 중복 실행을 피하기 위한 방식이며 짧은 중단 시간이 있다.
- `/api/v1/health`를 최대 약 2분간 확인한다. 실패하면 새 앱을 제거하고 이전 앱을 다시 시작한다. 최초 배포에는 되돌릴 앱이 없으므로 실패 상태로 종료한다.
- DB 마이그레이션은 자동 롤백하지 않는다. 향후 스키마 변경은 이전 앱과 호환되게 작성해야 한다. 이미지 재시작만으로 DB 변경이 되돌아가지는 않는다.
- `houselink-app-next` 또는 `houselink-app-previous`가 남아 있으면 내용을 확인하도록 배포를 중단한다. 전체 이미지 정리/prune은 실행하지 않는다.
- 실패한 새 앱의 로그는 runner 사용자의 `~/.local/state/houselink/last-failed-deploy.log`에 권한 600으로 보관한다. 실행 중인 앱 로그는 홈서버에서 `docker logs --tail 100 houselink-app`으로 확인한다. 비밀값이 포함될 수 있어 그대로 공개하지 않는다.
- 앱은 `ddl-auto: validate`로 검증하고 Flyway가 버전별 SQL을 적용한다. 이미 적용한 PostgreSQL V1은 고치지 않고 V2부터 추가한다.
- 운영 MySQL 이관은 [데이터 이관 문서](07-data-migration.md)를 참고한다. MySQL Flyway 이력을 PostgreSQL로 복사하지 않는다.
- 데이터 백업은 `pg_dump` 등을 통해 별도로 유지한다. 이 배포는 DB 백업이나 HTTPS/토스 실기기 검증을 대신하지 않는다.

## 이번 변경의 로컬 검증

- Java 25에서 전체 백엔드 테스트 6개 통과. PostgreSQL 17에 실제 Flyway V1 적용, Hibernate validate와 UUID/사용자 상태 저장·조회 포함.
- Docker 이미지 빌드와 실제 앱 기동 통과. 이관 데이터 사본으로 HTTP health, 사용자 10명의 인증된 내 정보 조회, 멤버십 4개의 물품 수량/버전, 모든 물품의 이력 건수를 확인했다.
- 배포 스크립트의 최초 배포·정상 교체·상태 확인 실패 시 복구·잘못된 설정 사전 차단 테스트 4개 통과. 실제 홈서버 교체 테스트는 아직 하지 않았다.
- Actions actionlint와 배포 스크립트 ShellCheck 통과. GHCR 업로드, amd64/arm64 원격 빌드, runner 배포, 토스 mTLS/HTTPS 실연동은 실제 Actions/홈서버에서 확인해야 한다.
