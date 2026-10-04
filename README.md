# 살림잇다 (SalimLink)

가족이나 룸메이트가 집에 있는 생필품(휴지, 세제, 치약 등)의 남은 수량을 함께 보는 토스 미니앱(앱인토스)입니다.
수량이 부족해지면 한눈에 보이고, 다 떨어진 채로 시간이 지나면 다른 구성원에게 알림을 보냅니다.

- 로그인 화면 없이 열자마자 쓸 수 있습니다. 토스의 익명 사용자 식별키(anonKey)로 사용자를 구분합니다.
- 공간(집) 단위로 물품을 공유하고, 초대 링크로 구성원을 추가합니다.
- 두 사람이 동시에 수량을 바꾸면 늦은 쪽에 최신 수량을 알려 줍니다.

## 구성

한 저장소에 백엔드와 앱이 함께 있습니다. 두 쪽이 `docs/openapi.yaml`의 같은 API 규격을 쓰기 때문입니다.

| 위치 | 내용 |
|---|---|
| `src/` | 백엔드 (Spring Boot 4, Java 25, JPA, MySQL 8.4, Flyway) |
| `miniapp/` | 토스 미니앱 (React 18, TypeScript, Vite, 토스 디자인 시스템 TDS) |
| `miniapp/design/` | 화면 와이어프레임과 앱 아이콘 |
| `docs/` | 기획, API 규격, 연동 테스트, 알림 템플릿, 배포 문서 |

## 백엔드 실행

필요한 것: JDK 25, MySQL 8.4

```bash
export DB_URL='jdbc:mysql://localhost:3306/family_share?serverTimezone=UTC'
export DB_USERNAME=<사용자> DB_PASSWORD=<비밀번호>
export TOSS_APP_NAME=<콘솔의 앱 이름>
export JWT_SECRET_BASE64=$(head -c32 /dev/urandom | base64)

./gradlew bootRun     # 서버 시작. 스키마는 Flyway가 만든다
./gradlew test        # 테스트
```

- 서버 주소는 `http://localhost:8080/api/v1` 입니다. `GET /api/v1/health`로 상태를 확인합니다.
- 토스와 통신(로그인 코드 교환)하려면 mTLS 인증서가 필요합니다. 아래 "운영 정보"를 보세요.
- 인증서가 없을 때는 토스 호출이 503으로 실패합니다. 화면만 확인하려면 가짜 토스 서버를 `FAMILY_SHARE_TOSS_BASE_URL`로 가리키면 됩니다.

## 앱 실행

필요한 것: Node 20 이상

```bash
cd miniapp
npm install
cp .env.example .env        # 서버 주소 등을 채운다

# 브라우저에서 개발 (토스 앱 밖이라 고정 개발 코드로 로그인)
AIT_APP_NAME=<앱 이름> VITE_API_BASE=http://localhost:8080/api/v1 VITE_DEV_CODE=<임의 문자열> npm run dev

# 토스 앱에서 테스트할 번들(.ait) 만들기
AIT_APP_NAME=<앱 이름> VITE_API_BASE=https://<서버 주소>/api/v1 npm run build
```

`VITE_DEV_CODE`는 토스 앱 밖에서만 쓰는 개발용 값입니다. 운영용 번들에는 넣지 않습니다.

## 운영 정보

저장소에는 비밀값을 올리지 않습니다. 아래는 `.gitignore`로 제외하고, 별도 장소에 보관합니다.

- 환경변수 파일(`.env`), DB 비밀번호, 토큰 서명 키(`JWT_SECRET_BASE64`)
- 토스 mTLS 인증서와 개인 키(`*.crt`, `*.key`, `*.pem`)
- 서버 주소가 박힌 앱 번들(`*.ait`)

자리표시자만 있는 `.env.example`(백엔드 루트, `miniapp/`)만 올라갑니다. 서버를 띄우는 방법은 [docs/06-deploy.md](docs/06-deploy.md)를 보세요.

## 문서

- [기획](docs/01-product-plan.md), [API 규격](docs/openapi.yaml), [API 계약 설명](docs/03-api-contract.md)
- [토스 연동 테스트](docs/04-connectivity-test.md), [알림 템플릿](docs/05-alert-template.md), [배포](docs/06-deploy.md)
- 화면 설계: `miniapp/design/wireframes.html`을 브라우저로 열면 됩니다.
