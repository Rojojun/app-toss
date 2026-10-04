# 토스 통신 확인 절차

미니앱을 콘솔에 등록하고 토스 앱(QR 테스트)에서 서버와 실제로 통신되는지 확인하는 순서다. 코드로 할 수 있는 준비(`/health`, 통신 확인용 미니앱 `miniapp/`, 오류 진단 응답)는 끝나 있고, 아래 1~3은 콘솔에서 직접 해야 한다.

## 1. 콘솔 등록

### 먼저 알아둘 것: 사업자 없이 진행한다

앱 등록은 사업자 없이 할 수 있다. 토스 로그인, 인앱 결제, 토스페이, 프로모션은 사업자 등록이 필수이지만, 이 서비스는 **사용자 식별키(`anonKey`)** 방식을 쓰므로 해당하지 않는다(문서의 사업자 필수 목록에 없다). 사용자는 로그인 화면이나 약관 동의 화면 없이 바로 진입한다.

다만 **mTLS 인증서를 사업자 없이 발급받을 수 있는지**는 문서에서 확인하지 못했다. 콘솔에 인증서 발급 메뉴가 열려 있는지 등록하면서 확인한다.

### 등록 순서와 입력값

1. [앱인토스 콘솔](https://apps-in-toss.toss.im/) 가입: 토스 비즈니스 회원, 만 19세 이상, 본인 명의 토스 앱 로그인.
2. 워크스페이스 생성(사업자당 1개). 제작자 이름은 `Rojojun` 같은 10자 이내 한글, 영문, 숫자다. QR 테스트는 **워크스페이스 멤버**만 할 수 있다.
3. 앱 등록('앱' 메뉴 > 등록하기). 개발이 끝나지 않아도 먼저 등록할 수 있다.

| 항목 | 입력값 |
|---|---|
| 앱 이름 | 살림잇다 (나중에 변경 가능) |
| 영문명 | SalimLink |
| appName | `salim-link` (**등록 후 변경 불가**, 중복이면 `salim-link-app` 등) |
| 앱 유형 | 게임이 아님 |

4. 앱 정보(임시 저장 가능):

| 항목 | 입력값 |
|---|---|
| 부제 | 함께 쓰는 생필품 재고 공유 |
| 상세 설명 | 우리집 공간을 만들고 초대 링크를 보내면 가족이나 룸메이트가 같은 공간에 참여해요. 치약, 세제 같은 물품의 남은 수량을 등록하고 바꾸면 모두에게 바로 보이고, 부족 기준 이하가 되면 '부족'으로 표시돼요. 누가 언제 수량을 바꿨는지도 이력으로 확인할 수 있어요. |
| 사용 연령 | 만 19세 이상(플랫폼 고정) |
| 고객문의 이메일 | 직접 입력 |

상세 설명에는 부족 알림을 넣지 않았다. 알림은 문구 검수 승인 전에는 켜지 않는 기능이다.

5. 토스 로그인 설정과 연결 끊기 콜백은 필요하지 않다(식별키 방식).

### AI로 콘솔 사용하기 (선택)

콘솔 MCP를 연결하면 Claude Code에서 워크스페이스 조회, 미니앱 생성(`miniapp_create`), 번들 업로드 같은 작업을 할 수 있다. 연결은 직접 해야 하고(로그인 창에서 인증), 되돌리기 어려운 작업(특히 appName 확정)은 실행 전에 입력값을 확인한다.

```bash
claude mcp add apps-in-toss-console --transport http https://mcp.toss.im/adapters/apps-in-toss-console/mcp --client-id mcp-gateway
```

## 2. mTLS 인증서 (직접)

콘솔에서 인증서와 개인 키 파일을 발급받는다. 문서에는 이 파일들이 필요하다는 것까지만 있고 콘솔의 정확한 메뉴 위치와 파일 형식은 없다. 서버는 PEM을 읽고, 개인 키는 PKCS#8(`-----BEGIN PRIVATE KEY-----`)이어야 한다. `BEGIN RSA PRIVATE KEY`로 시작하면 변환한다.

```bash
openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pkcs8.pem
```

## 3. 서버 실행과 공개 HTTPS 주소

토스 앱의 웹뷰는 HTTPS만 허용한다. 로컬 서버를 터널로 공개한다(예: cloudflared).

```bash
brew install cloudflared
cloudflared tunnel --url http://localhost:8080     # 출력되는 https://....trycloudflare.com 이 서버 주소
```

터널 주소는 실행할 때마다 바뀔 수 있다. 바뀔 때마다 미니앱을 다시 빌드해야 하므로, 오래 테스트한다면 고정 도메인을 쓰는 편이 낫다.

서버 환경변수:

| 변수 | 값 |
|---|---|
| `TOSS_APP_NAME` | 콘솔의 appName (CORS 허용 origin 생성에 사용) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL |
| `JWT_SECRET_BASE64` | `openssl rand -base64 32` |
| `SPRING_PROFILES_ACTIVE` | **인증서가 있을 때만** `toss-mtls`. 없으면 비워 둔다(서버는 뜨지만 토스 호출은 503) |
| `TOSS_MTLS_CERTIFICATE`, `TOSS_MTLS_PRIVATE_KEY` | `toss-mtls` 프로필일 때만 필요. `file:/절대/경로/cert.pem`, `file:/절대/경로/key.pkcs8.pem` (PEM 본문도 가능) |

```bash
docker run -d --name fs-mysql -e MYSQL_ROOT_PASSWORD=pw -e MYSQL_DATABASE=family_share -p 3306:3306 mysql:8.4
DB_URL=jdbc:mysql://localhost:3306/family_share DB_USERNAME=root DB_PASSWORD=<DB 비밀번호> \
TOSS_APP_NAME=<appName> JWT_SECRET_BASE64=$(openssl rand -base64 32) \
./gradlew bootRun

# 인증서를 발급받은 뒤에는 프로필을 켜고 경로를 준다
SPRING_PROFILES_ACTIVE=toss-mtls TOSS_MTLS_CERTIFICATE=file:/path/cert.pem TOSS_MTLS_PRIVATE_KEY=file:/path/key.pkcs8.pem \
DB_URL=... (위와 같은 변수) ./gradlew bootRun
```

인증서 없이 켠 상태에서는 미니앱의 버튼 1(서버 연결)과 CORS까지 확인할 수 있고, 버튼 2(식별키 교환)는 503 "토스 서버와 통신하지 못했습니다"가 정상이다.

`ddl-auto: create`라서 서버를 재시작할 때마다 데이터가 초기화된다.

## 4. 서버 단독 점검 (미니앱 없이)

```bash
BASE=https://<터널 주소>/api/v1

# 연결과 HTTPS
curl -i $BASE/health                                    # 200 {"status":"UP"}

# CORS (QR 테스트 origin)
curl -i -X OPTIONS $BASE/health \
  -H "Origin: https://<appName>.private-apps.tossmini.com" \
  -H "Access-Control-Request-Method: GET"               # 200 + Access-Control-Allow-Origin

# mTLS와 앱 등록 확인: 일부러 틀린 인증 코드로 식별키 교환
curl -s -X POST $BASE/auth/toss/exchange -H 'Content-Type: application/json' \
  -d '{"code":"invalid-on-purpose"}'
```

마지막 호출의 응답으로 서버와 토스 사이의 통신 상태를 판단한다.

| 응답 | 의미 |
|---|---|
| 401 `TOSS_AUTH_FAILED` | **정상.** mTLS 핸드셰이크를 통과했고 토스가 코드만 거부했다(`4011`) |
| 503 `토스 서버와 통신하지 못했습니다` | mTLS 인증서나 키 경로·형식 오류, 방화벽(아웃바운드 `apps-in-toss-api.toss.im` 443), 네트워크. 서버 로그 `토스 서버 통신 실패`의 스택 확인 |
| 503 `토스 서버가 요청을 처리하지 못했습니다` | 토스가 `4011`이 아닌 FAIL로 응답했다(예: `4095` 요청 한도 초과, 등록되지 않은 미니앱). appName과 인증서가 같은 앱의 것인지 확인 |

## 5. 미니앱 빌드와 QR 테스트

```bash
cd miniapp
npm install
AIT_APP_NAME=<appName> VITE_API_BASE=https://<터널 주소>/api/v1 npm run build   # <appName>.ait 생성
```

콘솔의 앱 번들 업로드에 `.ait`를 올리거나 CLI(`npx ait deploy`, 콘솔 '키' 메뉴에서 API 키 발급 필요)로 업로드한 뒤 '토스앱 테스트'의 QR을 토스 앱으로 스캔한다. 테스트 스킴은 `intoss-private://appsintoss?_deploymentId=...`이며 업로드할 때마다 `deploymentId`가 바뀐다. 최신 토스 앱이 필요하다.

화면의 버튼을 순서대로 누른다.

| 버튼 | 기대 결과 | 실패 시 |
|---|---|---|
| 1. 서버 연결 확인 | 200 `{"status":"UP"}` | "네트워크 오류": CORS(`TOSS_APP_NAME` 불일치, 서버 로그의 preflight 403), HTTPS 아님, 터널 주소 오류, 서버 꺼짐 |
| 2. 식별키 교환 | 첫 진입 `201`, 이후 `200`과 `accessToken` | 응답 해석은 4번 표와 같다. 코드는 일회용(5분)이라 실패하면 다시 누른다. SDK 3.6.0 이상, 최신 토스 앱이 필요하다 |
| 3. 내 정보 | 200 `{"user":..., "households":[]}` | 401이면 2번의 토큰이 없거나 만료(1시간) |

문제가 있으면 미니앱 오른쪽 위 더보기 메뉴의 **웹 디버그 툴즈**(Log, Network 탭)로 요청과 응답을 확인한다. 이 도구는 QR 또는 `intoss-private://`로 실행한 미니앱에서 쓸 수 있다.

## 이 절차로도 확인되지 않는 것

- `User.createAnonymousKeyAuthCode()`가 실제 토스 앱에서 어떤 최소 앱 버전부터 동작하는지
- 사업자 없이 mTLS 인증서를 발급받을 수 있는지
- 토스 서버 인증서가 공인 CA 체인인지(아니면 SSL bundle에 truststore 추가 필요)
- 토스 서버가 chunked 요청 본문을 받는지(JDK 클라이언트는 본문을 chunked로 보낸다)
- 출시 검수 기준
