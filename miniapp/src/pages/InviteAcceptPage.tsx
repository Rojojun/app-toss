import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BottomCTA, Result, TextField } from "@toss/tds-mobile";
import { acceptInvitation, previewInvitation } from "../api/endpoints";
import { LogoMark } from "../components/CategoryIcon";
import { selectHousehold } from "../hooks";

const ClockFigure = (
  <svg width="56" height="56" viewBox="0 0 24 24" aria-hidden>
    <circle cx="12" cy="12" r="8.5" fill="none" stroke="#8b95a1" strokeWidth="2.4" />
    <path d="M12 7.5V12.5l3 2" fill="none" stroke="#8b95a1" strokeWidth="2.4" strokeLinecap="round" />
  </svg>
);

export function InviteAcceptPage({ token }: { readonly token: string }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [nickname, setNickname] = useState("");
  const preview = useQuery({ queryKey: ["invitation", token], queryFn: () => previewInvitation(token) });

  const accept = useMutation({
    mutationFn: () => acceptInvitation(token, nickname.trim()),
    onSuccess: async ({ householdId }) => {
      selectHousehold(householdId);
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      navigate("/items", { replace: true });
    },
  });

  if (preview.isPending) return <div className="page" />;

  if (preview.isError || !preview.data.available)
    return (
      <Result
        figure={ClockFigure}
        title="사용할 수 없는 초대예요"
        description="만료됐거나 이미 사용된 초대일 수 있어요. 초대한 사람에게 새 링크를 받아 주세요."
        button={<BottomCTA.Single fixed onClick={() => navigate("/", { replace: true })}>홈으로</BottomCTA.Single>}
      />
    );

  return (
    <div className="page">
      <div className="hero" style={{ paddingTop: 72, paddingBottom: 28 }}>
        <LogoMark size={112} />
        <h1>'{preview.data.householdName}'에<br />초대받았어요</h1>
        <p>참여하면 집에 있는 물품과<br />남은 수량을 함께 볼 수 있어요.</p>
      </div>
      <TextField
        variant="box"
        label="이 공간에서 쓸 내 이름"
        labelOption="sustain"
        placeholder="예시) 나"
        value={nickname}
        maxLength={20}
        hasError={accept.isError}
        help={accept.isError ? "참여하지 못했어요. 다시 시도해 주세요." : undefined}
        onChange={(e) => setNickname(e.target.value)}
      />
      <BottomCTA.Single fixed disabled={!nickname.trim() || accept.isPending} loading={accept.isPending} onClick={() => accept.mutate()}>
        참여하기
      </BottomCTA.Single>
    </div>
  );
}
