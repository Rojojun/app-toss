import { useCallback, useSyncExternalStore } from "react";
import { useMutation, useQuery, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { useToast } from "@toss/tds-mobile";
import { changeStock, getHousehold, getMe, listItems, listStockChanges } from "./api/endpoints";
import { isApiError } from "./api/client";
import type { Item } from "./api/types";
import { useSession } from "./session";

export const useMe = () => {
  const session = useSession();
  return useQuery({ queryKey: ["me"], queryFn: getMe, enabled: session.status === "ready" });
};

export const useItems = (householdId: string | undefined) =>
  useQuery({
    queryKey: ["items", householdId],
    queryFn: () => listItems(householdId as string),
    enabled: Boolean(householdId),
  });

export const useHousehold = (householdId: string | undefined) =>
  useQuery({
    queryKey: ["household", householdId],
    queryFn: () => getHousehold(householdId as string),
    enabled: Boolean(householdId),
  });

export const useStockChanges = (householdId: string | undefined, itemId: string | undefined) =>
  useQuery({
    queryKey: ["stock-changes", householdId, itemId],
    queryFn: () => listStockChanges(householdId as string, itemId as string),
    enabled: Boolean(householdId && itemId),
  });

// ---- 지금 보고 있는 공간: 화면 여러 곳이 같은 값을 보도록 작은 외부 저장소로 둔다.
const STORAGE_KEY = "salim.currentHousehold";

const readStored = (): string | null => {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
};

let currentId: string | null = readStored();
const listeners = new Set<() => void>();

export const selectHousehold = (id: string): void => {
  currentId = id;
  try {
    localStorage.setItem(STORAGE_KEY, id);
  } catch {
    // 저장소를 못 쓰는 환경에서는 이번 실행 동안만 기억한다.
  }
  listeners.forEach((listener) => listener());
};

const subscribe = (listener: () => void): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const useCurrentHousehold = () => {
  const me = useMe();
  const storedId = useSyncExternalStore(subscribe, () => currentId);
  const households = me.data?.households ?? [];
  const household = households.find((entry) => entry.id === storedId) ?? households[0];
  const select = useCallback((id: string) => selectHousehold(id), []);

  return { loading: me.isPending, household, households, select };
};

// ---- 캐시 갱신: 서버가 돌려준 최신 물품으로 목록을 바꾼다(충돌 응답의 currentItem 포함).
export const replaceItem = (queryClient: QueryClient, item: Item): void => {
  queryClient.setQueryData<ReadonlyArray<Item>>(["items", item.householdId], (items) =>
    items?.map((entry) => (entry.id === item.id ? item : entry)),
  );
};

// 수량 바꾸기(목록의 -/+ 와 상세의 큰 스피너, "다 썼어요"가 함께 쓴다).
// 다른 가족이 먼저 바꿨다면(409) 서버가 준 최신 물품으로 맞추고 알려 준다.
export const useStockMutation = () => {
  const queryClient = useQueryClient();
  const { openToast } = useToast();

  return useMutation({
    mutationFn: ({ item, quantity }: { item: Item; quantity: number }) =>
      changeStock(item.householdId, item.id, item.version, quantity),
    onSuccess: ({ item }) => {
      replaceItem(queryClient, item);
      void queryClient.invalidateQueries({ queryKey: ["stock-changes", item.householdId, item.id] });
    },
    onError: (error) => {
      const current = isApiError(error) ? (error.details as { currentItem?: Item }).currentItem : undefined;
      if (!current) return openToast("수량을 바꾸지 못했어요. 잠시 후 다시 시도해 주세요.");
      replaceItem(queryClient, current);
      openToast(`방금 다른 가족이 수량을 바꿨어요. 최신 수량 ${current.quantity}${current.unit}로 맞췄어요.`);
    },
  });
};
