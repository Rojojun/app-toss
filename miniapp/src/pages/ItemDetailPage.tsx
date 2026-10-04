import { useNavigate, useParams } from "react-router-dom";
import { Badge, Button, ListRow, Top } from "@toss/tds-mobile";
import { useShop } from "../lib/shop";
import { QuantitySpinner } from "../components/QuantitySpinner";
import { categoryLabel, formatWhen } from "../lib/format";
import { useCurrentHousehold, useItems, useStockChanges, useStockMutation } from "../hooks";

export function ItemDetailPage() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const { household } = useCurrentHousehold();
  const item = useItems(household?.id).data?.find((entry) => entry.id === itemId);
  const history = useStockChanges(household?.id, itemId);
  const stock = useStockMutation();
  const shop = useShop();

  if (!item) return <div className="page"><Top title={<Top.TitleParagraph>물품을 찾는 중…</Top.TitleParagraph>} /></div>;

  return (
    <div className="page">
      <Top
        title={<Top.TitleParagraph>{item.name}</Top.TitleParagraph>}
        subtitleBottom={<div style={{ display: "flex" }}><Badge size="small" color="elephant" variant="weak">{categoryLabel(item.category)}</Badge></div>}
        right={
          <span className="top-right">
            <button type="button" className="member-btn" aria-label="수정" onClick={() => navigate(`/items/${item.id}/edit`)}>✎</button>
          </span>
        }
      />
      <div className="qty-hero">
        <span className="qty-label">지금 남은 수량</span>
        <span className="big-qty">{item.quantity}<small>{item.unit}</small></span>
        {item.lowStock && <Badge size="small" color="red" variant="weak">부족</Badge>}
        <div className="qty-spinner">
          <QuantitySpinner
            size="large"
            title="남은 수량"
            number={item.quantity}
            onNumberChange={(quantity) => stock.mutate({ item, quantity })}
          />
        </div>
        {item.lowStock && (
          <div className="qty-done">
            <Button display="block" size="medium" onClick={() => shop(item.name)}>
              {item.name} 사러 가기
            </Button>
          </div>
        )}
        <div className="qty-done">
          <Button display="block" size="medium" variant="weak" disabled={item.quantity === 0 || stock.isPending} onClick={() => stock.mutate({ item, quantity: 0 })}>
            다 썼어요
          </Button>
        </div>
      </div>
      <div className="threshold-row">
        <span>부족 기준</span>
        <b>{item.lowStockThreshold}{item.unit}</b>
      </div>
      <div className="section-label">변경 이력</div>
      {(history.data ?? []).map((change) => (
        <ListRow
          key={change.id}
          contents={<ListRow.Texts type="2RowTypeA" top={change.actorNickname} bottom={formatWhen(change.createdAt)} />}
          right={<b>{change.beforeQuantity} → {change.afterQuantity}</b>}
        />
      ))}
      {history.data?.length === 0 && <p className="note">아직 변경 이력이 없어요.</p>}
    </div>
  );
}
