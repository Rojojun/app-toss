import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import aitDevtools from "@apps-in-toss/devtools/unplugin";

export default defineConfig({
  plugins: [aitDevtools.vite(), react()],
  define: {
    // 앱 이름은 빌드할 때 AIT_APP_NAME 하나로 맞춘다(콘솔 appName, 딥링크 주소에 같이 쓰인다).
    "import.meta.env.VITE_APP_NAME": JSON.stringify(process.env.AIT_APP_NAME ?? "salim-link"),
  },
});
