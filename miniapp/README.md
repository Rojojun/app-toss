# 살림잇다 미니앱

토스 앱에서 실행되는 React + TDS 미니앱입니다. 서버(`../src`)의 API를 호출하고, 로그인 화면 없이 토스가 주는 일회용 코드로 사용자를 식별합니다.

## 화면

시작, 공간 만들기, 홈(부족 목록), 물품 상세·추가·수정, 멤버(행동 시트, 소유권 넘기기, 초대 링크), 초대 수락. 설계는 `design/wireframes.html`에 있습니다.

## 구조

| 위치 | 내용 |
|---|---|
| `src/api/` | API 클라이언트와 타입 (규격은 `../docs/openapi.yaml`) |
| `src/session.tsx` | 토스 식별 코드 교환과 로그인 상태 |
| `src/hooks.ts` | 데이터 조회 훅과 수량 변경 처리(충돌 안내 포함) |
| `src/pages/` | 화면 |
| `src/components/` | 아이콘, 수량 입력 조절기 |
| `design/` | 와이어프레임과 앱 아이콘 |

## 실행

```bash
npm install
cp .env.example .env
AIT_APP_NAME=<앱 이름> VITE_API_BASE=http://localhost:8080/api/v1 VITE_DEV_CODE=<임의 문자열> npm run dev   # 브라우저 개발
AIT_APP_NAME=<앱 이름> VITE_API_BASE=https://<서버 주소>/api/v1 npm run build                          # .ait 번들
```

## 환경변수

| 이름 | 설명 |
|---|---|
| `AIT_APP_NAME` | 콘솔의 앱 이름(필수, 한 번 정하면 바꿀 수 없다) |
| `VITE_API_BASE` | 서버 주소, `/api/v1`까지 포함 |
| `VITE_DEV_CODE` | 토스 앱 밖 개발용 고정 로그인 코드. 운영 번들에는 넣지 않는다 |
| `VITE_SHOP_URL` | "사러 가기"가 열 주소(선택, `{q}`는 물품 이름). 비우면 안내만 한다 |

번들 업로드와 테스트 절차는 [docs/04-connectivity-test.md](../docs/04-connectivity-test.md)를 참고하세요.
