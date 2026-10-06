import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { BottomCTA, TextField, Top } from "@toss/tds-mobile";
import { createHousehold } from "../api/endpoints";
import { selectHousehold } from "../hooks";

export function CreateHouseholdPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");

  const create = useMutation({
    mutationFn: () => createHousehold(name.trim(), nickname.trim()),
    onSuccess: async (household) => {
      selectHousehold(household.id);
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      navigate("/items", { replace: true });
    },
  });

  return (
    <div className="page">
      <Top title={<Top.TitleParagraph>공간 이름을<br />정해 주세요</Top.TitleParagraph>} />
      <div className="stack">
        <TextField variant="box" label="공간 이름" labelOption="sustain" placeholder="예시) 우리집" value={name} maxLength={30} onChange={(e) => setName(e.target.value)} />
        <TextField
          variant="box"
          label="이 공간에서 쓸 내 이름"
          labelOption="sustain"
          placeholder="예시) 나"
          value={nickname}
          maxLength={20}
          hasError={create.isError}
          help={create.isError ? "만들지 못했어요. 다시 시도해 주세요." : "멤버들에게 이 이름으로 보여요. 나중에 바꿀 수 있어요."}
          onChange={(e) => setNickname(e.target.value)}
        />
      </div>
      <BottomCTA.Single fixed disabled={!name.trim() || !nickname.trim() || create.isPending} loading={create.isPending} onClick={() => create.mutate()}>
        만들기
      </BottomCTA.Single>
    </div>
  );
}
