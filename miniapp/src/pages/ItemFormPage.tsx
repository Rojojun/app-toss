import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { BottomCTA, TextField, Top } from "@toss/tds-mobile";
import { createItem, updateItem } from "../api/endpoints";
import type { ItemCategory } from "../api/types";
import { CategoryIcon } from "../components/CategoryIcon";
import { QuantitySpinner } from "../components/QuantitySpinner";
import { CATEGORIES } from "../lib/format";
import { useCurrentHousehold, useItems } from "../hooks";

const UNITS = ["개", "롤", "팩", "병", "통", "봉", "장"] as const;

export function ItemFormPage() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { household } = useCurrentHousehold();
  const existing = useItems(household?.id).data?.find((entry) => entry.id === itemId);

  const [name, setName] = useState(existing?.name ?? "");
  const [category, setCategory] = useState<ItemCategory>(existing?.category ?? "TOILETRIES");
  const [unit, setUnit] = useState(existing?.unit ?? "개");
  const [customUnit, setCustomUnit] = useState(Boolean(existing?.unit && !UNITS.includes(existing.unit as typeof UNITS[number])));
  const [quantity, setQuantity] = useState(1);
  const [threshold, setThreshold] = useState(existing?.lowStockThreshold ?? 1);

  const unitInvalid = /\d/.test(unit);

  const save = useMutation({
    mutationFn: () =>
      existing
        ? updateItem(existing.householdId, existing.id, {
            expectedVersion: existing.version,
            name: name.trim(),
            category,
            unit: unit.trim(),
            lowStockThreshold: threshold,
          })
        : createItem(household!.id, { name: name.trim(), category, unit: unit.trim(), quantity, lowStockThreshold: threshold }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["items", household?.id] });
      navigate(-1);
    },
  });

  return (
    <div className="page">
      <Top title={<Top.TitleParagraph>{existing ? "물품 수정" : "물품 추가"}</Top.TitleParagraph>} />
      <div className="stack">
        <TextField variant="box" label="이름" labelOption="sustain" placeholder="예시) 치약" value={name} maxLength={30} onChange={(e) => setName(e.target.value)} />
        <div className="pad stack" style={{ gap: 8 }}>
          <span className="muted">아이콘</span>
          <div className="icon-grid">
            {CATEGORIES.map((entry) => (
              <button key={entry.value} type="button" className="icon-tile" aria-pressed={category === entry.value} onClick={() => setCategory(entry.value)}>
                <CategoryIcon category={entry.value} size={44} />
                <span>{entry.label}</span>
              </button>
            ))}
          </div>
        </div>
        <div className="pad stack" style={{ gap: 8 }}>
          <span className="muted">단위 (수량 뒤에 붙는 말)</span>
          <div className="chips">
            {UNITS.map((entry) => (
              <button key={entry} type="button" className="chip" aria-pressed={!customUnit && unit === entry} onClick={() => { setCustomUnit(false); setUnit(entry); }}>
                {entry}
              </button>
            ))}
            <button type="button" className="chip" aria-pressed={customUnit} onClick={() => { setCustomUnit(true); if (UNITS.includes(unit as typeof UNITS[number])) setUnit(""); }}>
              직접 입력
            </button>
          </div>
        </div>
        {customUnit && <TextField
          variant="box"
          label="단위 직접 입력"
          labelOption="sustain"
          placeholder="예시) 묶음, 상자"
          value={unit}
          maxLength={10}
          hasError={unitInvalid}
          help={unitInvalid ? "단위에는 숫자를 쓸 수 없어요. 수량은 아래에서 정해요." : undefined}
          onChange={(e) => setUnit(e.target.value)}
        />}
        {!existing && (
          <div className="kv">
            <span style={{ color: "inherit", fontWeight: 600 }}>지금 수량</span>
            <QuantitySpinner size="small" title="지금 수량" number={quantity} onNumberChange={setQuantity} />
          </div>
        )}
        <div className="kv">
          <span style={{ color: "inherit" }}>
            <b>부족 기준</b>
            <div className="muted" style={{ fontSize: 12 }}>이 값 이하가 되면 '부족'으로 표시돼요</div>
          </span>
          <QuantitySpinner size="small" title="부족 기준" number={threshold} onNumberChange={setThreshold} />
        </div>
      </div>
      <BottomCTA.Single fixed disabled={!name.trim() || !unit.trim() || unitInvalid || save.isPending} loading={save.isPending} onClick={() => save.mutate()}>
        저장
      </BottomCTA.Single>
    </div>
  );
}
