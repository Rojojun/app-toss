# 배포 (컨테이너)

서버는 컨테이너 이미지 하나로 배포한다. `Dockerfile`은 JDK 25로 빌드하고 JRE 25에서 일반 사용자로 실행한다.
비밀값(DB 비밀번호, JWT 키, mTLS 인증서·키)은 이미지에 넣지 않고 환경변수와 마운트로 받는다.

```bash
docker build -t family-share .
docker run -d -p 8080:8080 \
  -e DB_URL='jdbc:mysql://<host>:3306/family_share?serverTimezone=UTC' -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e TOSS_APP_NAME=salim-link -e JWT_SECRET_BASE64=<base64 32바이트 이상> \
  -e SPRING_PROFILES_ACTIVE=toss-mtls \
  -e TOSS_MTLS_CERTIFICATE=file:/certs/toss.crt -e TOSS_MTLS_PRIVATE_KEY=file:/certs/toss.key \
  -v /경로/certs:/certs:ro \
  family-share
```

## 환경변수

| 이름 | 필수 | 설명 |
|---|---|---|
| `DB_URL` `DB_USERNAME` `DB_PASSWORD` | O | MySQL 8.4 |
| `TOSS_APP_NAME` | O | 콘솔의 앱 이름(CORS 허용 도메인 `https://<appName>.apps.tossmini.com`, `.private-apps.tossmini.com`) |
| `JWT_SECRET_BASE64` | O | 토큰 서명 키. 바꾸면 모든 로그인이 풀린다 |
| `SPRING_PROFILES_ACTIVE=toss-mtls` | 운영 O | 이 프로필이 없으면 토스 호출이 실패한다(503) |
| `TOSS_MTLS_CERTIFICATE` `TOSS_MTLS_PRIVATE_KEY` | 운영 O | PEM(`file:/절대경로` 또는 본문). 키는 PKCS#8 |
| `ALERT_TEMPLATE_SET_CODE` | | 승인된 알림 템플릿 코드. 비우면 알림을 보내지 않고 기록만 한다 |

## 운영 전에 정할 것

- **스키마(완료)**: Flyway가 `src/main/resources/db/migration`의 `V1__init.sql`부터 순서대로 적용한다. 서버는 시작할 때 엔티티와 스키마가 맞는지만 확인(`ddl-auto: validate`)하고 테이블을 만들거나 지우지 않는다. 컬럼·테이블을 바꿀 때는 **새 파일(`V2__…sql`)을 추가**하고 이미 적용된 파일은 고치지 않는다.
- **인스턴스 수**: 알림 발송 스케줄러(`OutOfStockAlertDispatcher`)는 1분마다 대기 건을 처리한다. 인스턴스를 여러 개 띄우면 같은 알림이 중복될 수 있어, 지금은 1개만 띄운다(여러 개가 필요하면 발송 잠금을 추가해야 한다).
- **인증서 만료**: mTLS 인증서는 2027-10-27에 만료된다. 교체 절차를 정해 둔다.
- **HTTPS 주소**: 토스 앱은 HTTPS 도메인만 호출한다. 앱 번들 빌드 때 `VITE_API_BASE=https://<도메인>/api/v1`로 넣는다.

## 로컬 개발에서의 변화

- 예전에는 서버를 켤 때마다 DB가 초기화됐지만, 이제는 데이터가 남는다. 처음부터 다시 하려면 DB를 지우고 다시 만든다(`drop database family_share; create database family_share character set utf8mb4`).
- 예전 방식(`ddl-auto: create`)으로 만든 DB에 새 서버를 붙이면 Flyway가 "기록 없는 비어 있지 않은 스키마"라며 멈춘다. 그때도 DB를 새로 만들면 된다.
- 테스트는 H2 + `create-drop`을 쓰고 Flyway는 끈다(`src/test/resources/application.yaml`).

## 작은 VM 한 대로 운영하기 (시범 운영)

서버와 MySQL 을 한 VM 에서 `docker-compose.yml` 로 함께 돌린다. 메모리 제한이 걸려 있어(앱 600MB, DB 450MB) **RAM 2GB 급을 권장**하고, 1GB 는 스왑을 두면 시범 운영 정도는 가능하다.

```bash
cp .env.example .env        # 값 채우기 (JWT 키, DB 비밀번호, 인증서 폴더 MTLS_DIR)
docker compose up -d --build
curl localhost:8080/api/v1/health
```

- 컨테이너 포트는 `127.0.0.1:8080` 에만 열린다. 밖에서는 HTTPS 터널/프록시(예: Tailscale Funnel, Caddy)로 들어온다.
- mTLS 인증서(`toss.crt`, `toss.key`)는 VM 의 `MTLS_DIR` 폴더에 직접 올린다(권한 600, 저장소에 넣지 않는다).
- DB 데이터는 `dbdata` 볼륨에 저장된다. **백업 없이 운영하지 않는다**: 예) 매일 `mysqldump` 를 VM 밖에 보관.
- 이 구성은 아직 실제 VM 에서 시험하지 않았다(compose 문법 검증만 완료).
