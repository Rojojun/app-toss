// 서버 주소(공개 HTTPS, context-path 포함). 빌드할 때 VITE_API_BASE로 넘긴다.
export const API_BASE: string = import.meta.env.VITE_API_BASE ?? "";
export const APP_NAME: string = import.meta.env.VITE_APP_NAME ?? "salim-link";
// 토스 앱 밖(로컬 브라우저)에서 개발할 때만 쓰는 고정 인증 코드. 운영 빌드에서는 비워 둔다.
export const DEV_CODE: string | undefined = import.meta.env.VITE_DEV_CODE || undefined;
// "사러 가기"가 열 주소(선택). 비워 두면 이동하지 않고 안내만 한다.
// 토스 앱 안의 쇼핑으로 바로 가는 공식 방법이 없어(supertoss:// 딥링크는 정책상 불가, toss.im 공유 링크는 복귀 문제) 기본은 비워 둔다.
// 공식 검색 주소가 생기면 이 값으로 연결한다({q} 자리에 물품 이름이 들어간다).
export const SHOP_URL: string | undefined = import.meta.env.VITE_SHOP_URL || undefined;
