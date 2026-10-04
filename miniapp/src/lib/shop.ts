import { useCallback } from "react";
import { openURL } from "@apps-in-toss/web-framework";
import { useDialog } from "@toss/tds-mobile";
import { SHOP_URL } from "../config";

const copyText = (text: string): Promise<boolean> =>
  navigator.clipboard.writeText(text).then(
    () => true,
    () => false,
  );

// "사러 가기": 물품 이름을 복사하고, 토스쇼핑에서 찾는 방법을 안내한다.
// 앱 밖(웹페이지)으로는 보내지 않는다. SHOP_URL 이 설정돼 있을 때만 그 주소를 연다.
export const useShop = () => {
  const { openAlert } = useDialog();

  return useCallback(
    async (itemName: string) => {
      const copied = await copyText(itemName);
      await openAlert({
        title: `'${itemName}' 사러 가볼까요?`,
        description: copied
          ? `이름을 복사했어요. 토스 앱 아래의 '쇼핑' 탭에서 붙여넣어 검색해 보세요.`
          : `토스 앱 아래의 '쇼핑' 탭에서 '${itemName}'을 검색해 보세요.`,
        alertButton: "확인",
      });
      if (SHOP_URL) await openURL(SHOP_URL.replace("{q}", encodeURIComponent(itemName)));
    },
    [openAlert],
  );
};
