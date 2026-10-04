# 살림잇다 통신 확인용 미니앱

토스 앱(QR 테스트)에서 서버와 실제로 통신되는지 확인하기 위한 최소 미니앱이다. 화면의 버튼 3개로 서버 연결, 사용자 식별키 교환(로그인 없음), 내 정보 조회를 확인하고 결과를 화면에 그대로 보여 준다.

빌드, 업로드, 결과 해석은 [docs/04-connectivity-test.md](../docs/04-connectivity-test.md)를 따른다.

```bash
npm install
AIT_APP_NAME=<콘솔 appName> VITE_API_BASE=https://<서버 주소>/api/v1 npm run build
```

로컬 브라우저에서는 `npm run dev`로 실행할 수 있고, 이때 `createAnonymousKeyAuthCode`의 동작은 `@apps-in-toss/devtools`를 따른다. 서버의 CORS 허용 origin에 `http://localhost:5173`이 없으면 이 경우 요청이 막힌다. 실제 통신 확인은 토스 앱(QR 테스트)에서 한다.
