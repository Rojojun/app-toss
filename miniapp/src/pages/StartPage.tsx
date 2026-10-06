import { useNavigate } from "react-router-dom";
import { BottomCTA } from "@toss/tds-mobile";
import { LogoMark } from "../components/CategoryIcon";

export function StartPage() {
  const navigate = useNavigate();
  return (
    <div className="page">
      <div className="center">
        <div className="hero">
          <LogoMark size={112} />
          <h1>함께 쓰는 생필품,<br />남은 수량을 같이 봐요</h1>
          <p>공간을 만들어 가족이나 룸메이트를 초대하면<br />집에 뭐가 남았는지 모두가 바로 알 수 있어요.</p>
        </div>
      </div>
      <BottomCTA.Single fixed onClick={() => navigate("/create")}>우리집 공간 만들기</BottomCTA.Single>
      <div className="foot-link" style={{ position: "fixed", left: 0, right: 0, bottom: 96, zIndex: 1, color: "#191f28", fontSize: 14, fontWeight: 600 }}>
        초대를 받았다면 받은 링크로 다시 열어 주세요
      </div>
    </div>
  );
}
