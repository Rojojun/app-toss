import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { User } from "@apps-in-toss/web-framework";
import { exchange } from "./api/endpoints";
import { isApiError, setAccessToken, setTokenRefresher } from "./api/client";
import { DEV_CODE } from "./config";

type Session =
  | { readonly status: "loading" }
  | { readonly status: "ready"; readonly userId: string }
  | { readonly status: "error"; readonly message: string };

type SessionContextValue = Session & { readonly retry: () => void };

const SessionContext = createContext<SessionContextValue>({ status: "loading", retry: () => undefined });

// 토스 앱 안에서는 SDK가 일회용 코드를 준다. 로컬 개발(VITE_DEV_CODE)에서는 SDK 목업이 매번 새 코드를 만들어
// 사용자가 계속 새로 생기므로, 고정 개발 코드를 우선 쓴다. 운영 빌드에는 이 값이 비어 있다.
const issueCode = async (): Promise<string> =>
  DEV_CODE ?? User.createAnonymousKeyAuthCode().then((issued) => issued.code);

const signIn = async (): Promise<string> => {
  const response = await exchange(await issueCode());
  setAccessToken(response.accessToken);
  return response.user.id;
};

const describe = (error: unknown): string =>
  isApiError(error) ? error.message : "토스 앱에서 열어 주세요. 사용자를 확인하지 못했어요.";

export function SessionProvider({ children }: { readonly children: ReactNode }) {
  const [session, setSession] = useState<Session>({ status: "loading" });

  const start = useCallback(() => {
    setSession({ status: "loading" });
    signIn()
      .then((userId) => setSession({ status: "ready", userId }))
      .catch((error: unknown) => setSession({ status: "error", message: describe(error) }));
  }, []);

  useEffect(() => {
    setTokenRefresher(async () => {
      await signIn();
    });
    start();
    return () => setTokenRefresher(null);
  }, [start]);

  const value = useMemo<SessionContextValue>(() => ({ ...session, retry: start }), [session, start]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export const useSession = (): SessionContextValue => useContext(SessionContext);
