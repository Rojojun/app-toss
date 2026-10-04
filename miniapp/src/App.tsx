import { Navigate, Route, Routes, useSearchParams } from "react-router-dom";
import { Button, Result, Skeleton } from "@toss/tds-mobile";
import { useSession } from "./session";
import { useCurrentHousehold } from "./hooks";
import { StartPage } from "./pages/StartPage";
import { CreateHouseholdPage } from "./pages/CreateHouseholdPage";
import { HomePage } from "./pages/HomePage";
import { ItemDetailPage } from "./pages/ItemDetailPage";
import { ItemFormPage } from "./pages/ItemFormPage";
import { MembersPage } from "./pages/MembersPage";
import { InviteAcceptPage } from "./pages/InviteAcceptPage";

function Loading() {
  return (
    <div className="pad" style={{ paddingTop: 80 }}>
      <Skeleton pattern="topList" />
    </div>
  );
}

function Landing() {
  const { loading, household } = useCurrentHousehold();
  if (loading) return <Loading />;
  return <Navigate to={household ? "/items" : "/start"} replace />;
}

function Invite() {
  const [params] = useSearchParams();
  const token = params.get("token");
  return token ? <InviteAcceptPage token={token} /> : <MembersPage openInvite />;
}

export function App() {
  const session = useSession();

  if (session.status === "loading") return <Loading />;
  if (session.status === "error")
    return (
      <Result
        title="연결하지 못했어요"
        description={session.message}
        button={<Button onClick={session.retry}>다시 시도</Button>}
      />
    );

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/start" element={<StartPage />} />
      <Route path="/create" element={<CreateHouseholdPage />} />
      <Route path="/items" element={<HomePage />} />
      <Route path="/items/new" element={<ItemFormPage />} />
      <Route path="/items/:itemId" element={<ItemDetailPage />} />
      <Route path="/items/:itemId/edit" element={<ItemFormPage />} />
      <Route path="/members" element={<MembersPage />} />
      <Route path="/invite" element={<Invite />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
