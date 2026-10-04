import { defineConfig } from "@apps-in-toss/web-framework/config";

// appName은 앱인토스 콘솔에 등록한 값과 반드시 같아야 한다. 빌드할 때 AIT_APP_NAME 환경변수로 넘긴다.
const appName = process.env.AIT_APP_NAME;

if (!appName) {
  throw new Error("AIT_APP_NAME 환경변수에 콘솔에 등록한 appName을 지정하세요.");
}

export default defineConfig({
  appName,
  brand: {
    displayName: "살림잇다",
    primaryColor: "#3182F6",
  },
  webView: {},
  permissions: [],
  webBundleDir: "dist",
});
