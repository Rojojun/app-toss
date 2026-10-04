import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Badge, BottomCTA, ListRow, Tab, Top, useBottomSheet } from "@toss/tds-mobile";
import type { Item } from "../api/types";
import { CategoryIcon } from "../components/CategoryIcon";
import { QuantitySpinner } from "../components/QuantitySpinner";
import { useShop } from "../lib/shop";
import { useCurrentHousehold, useItems, useStockMutation } from "../hooks";

function PeopleIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden fill="#4e5968">
      <circle cx="9" cy="8" r="3.4" />
      <circle cx="17" cy="9.5" r="2.6" opacity=".7" />
      <path d="M2.5 19c0-3.3 2.9-5.4 6.5-5.4s6.5 2.1 6.5 5.4z" />
      <path d="M16 14.2c3 0 5.5 1.7 5.5 4.8H17c0-1.8-.4-3.4-1-4.8z" opacity=".7" />
    </svg>
  );
}

export function HomePage() {
  const navigate = useNavigate();
  const { household, households, select } = useCurrentHousehold();
  const items = useItems(household?.id);
  const stock = useStockMutation();
  const sheet = useBottomSheet();
  const shop = useShop();
  const [tab, setTab] = useState(0);

  const all = items.data ?? [];
  const low = all.filter((item) => item.lowStock);
  const rest = all.filter((item) => !item.lowStock);

  const row = (item: Item) => (
    <ListRow
      key={item.id}
      onClick={() => navigate(`/items/${item.id}`)}
      left={<CategoryIcon category={item.category} />}
      contents={
        <ListRow.Texts
          type="2RowTypeA"
          top={<span className="row"><span style={{ fontWeight: 600 }}>{item.name}</span>{item.lowStock && <Badge size="small" color="red" variant="weak">부족</Badge>}</span>}
          bottom={
            <span style={{ color: "var(--adaptiveGrey600, #6b7684)" }}>
              부족 기준 {item.lowStockThreshold}{item.unit}
              {item.lowStock && (
                <button
                  type="button"
                  className="shop-link"
                  onClick={(event) => {
                    event.stopPropagation();
                    void shop(item.name);
                  }}
                >
                  사러 가기
                </button>
              )}
            </span>
          }
        />
      }
      right={
        <div onClick={(event) => event.stopPropagation()}>
          <QuantitySpinner
            size="small"
            title={`${item.name} 수량`}
            number={item.quantity}
            onNumberChange={(quantity) => stock.mutate({ item, quantity })}
          />
        </div>
      }
    />
  );

  return (
    <div className="page">
      <Top
        title={
          <button
            type="button"
            className="title-btn"
            onClick={() =>
              sheet.open({
                children: (
                  <div style={{ paddingBottom: 16 }}>
                    <h3 className="sheet-title">내 공간</h3>
                    {households.map((entry) => (
                      <ListRow
                        key={entry.id}
                        onClick={() => { select(entry.id); sheet.close(); }}
                        contents={<ListRow.Texts type="1RowTypeA" top={entry.name} />}
                        right={entry.id === household?.id ? <span aria-hidden style={{ color: "#3182f6", fontWeight: 700 }}>✓</span> : undefined}
                      />
                    ))}
                    <ListRow
                      onClick={() => { sheet.close(); navigate("/create"); }}
                      contents={<ListRow.Texts type="1RowTypeA" top={<span style={{ color: "#3182f6" }}>+ 새 공간 만들기</span>} />}
                    />
                  </div>
                ),
                onClose: sheet.close,
              })
            }
          >
            {household?.name ?? "우리집"}<i>▼</i>
          </button>
        }
        right={
          <span className="top-right">
            <button type="button" className="member-btn" aria-label="멤버" onClick={() => navigate("/members")}>
              <PeopleIcon />
            </button>
          </span>
        }
      />
      <div className="hero-card">
        <b>지금 사야 할 것 {low.length}개</b>
        <span>전체 물품 {all.length}개</span>
      </div>
      <Tab onChange={setTab}>
        <Tab.Item selected={tab === 0}>부족 {low.length}</Tab.Item>
        <Tab.Item selected={tab === 1}>전체 {all.length}</Tab.Item>
      </Tab>
      {tab === 0 ? (
        <>
          {low.map(row)}
          {low.length === 0 && !items.isPending && <p className="note" style={{ textAlign: "center" }}>부족한 물품이 없어요.</p>}
          {rest.length > 0 && <div className="section-label">여유 있어요</div>}
          {rest.map(row)}
        </>
      ) : (
        <>
          {all.map(row)}
          {all.length === 0 && !items.isPending && <p className="note" style={{ textAlign: "center" }}>아직 등록한 물품이 없어요.</p>}
        </>
      )}
      <BottomCTA.Single fixed onClick={() => navigate("/items/new")}>+ 물품 추가</BottomCTA.Single>
    </div>
  );
}
