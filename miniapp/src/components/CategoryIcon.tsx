import type { ItemCategory } from "../api/types";
import logoUrl from "../assets/logo.svg";

const STYLE: Record<ItemCategory, { readonly bg: string; readonly fg: string; readonly glyph: JSX.Element }> = {
  TOILETRIES: {
    bg: "#E8F3FF",
    fg: "#3182F6",
    glyph: <path d="M12 3.5c3 3.8 5.5 6.6 5.5 10a5.5 5.5 0 0 1-11 0c0-3.4 2.5-6.2 5.5-10z" />,
  },
  CLEANING: {
    bg: "#E9F9EE",
    fg: "#1FBF5B",
    glyph: <path d="M12 3l2.1 6.9L21 12l-6.9 2.1L12 21l-2.1-6.9L3 12l6.9-2.1z" />,
  },
  LAUNDRY: {
    bg: "#F3ECFF",
    fg: "#8A4DFF",
    glyph: (
      <>
        <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="3" />
        <path d="M7.5 13c1.5-2 3-2 4.5 0s3 2 4.5 0" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" />
      </>
    ),
  },
  KITCHEN: {
    bg: "#FFF3E0",
    fg: "#FF9F00",
    glyph: (
      <>
        <path d="M4 11h16a8 8 0 0 1-16 0z" />
        <rect x="9" y="5" width="6" height="3" rx="1.5" />
      </>
    ),
  },
  OTHER: {
    bg: "#F2F4F6",
    fg: "#8B95A1",
    glyph: (
      <>
        <circle cx="5.5" cy="12" r="2.4" />
        <circle cx="12" cy="12" r="2.4" />
        <circle cx="18.5" cy="12" r="2.4" />
      </>
    ),
  },
};

export function CategoryIcon({ category, size = 40 }: { readonly category: ItemCategory; readonly size?: number }) {
  const { bg, fg, glyph } = STYLE[category];

  return (
    <span
      aria-hidden
      style={{
        width: size,
        height: size,
        borderRadius: size * 0.3,
        background: bg,
        color: fg,
        display: "grid",
        placeItems: "center",
        flex: "none",
      }}
    >
      <svg width={size * 0.55} height={size * 0.55} viewBox="0 0 24 24" fill="currentColor">
        {glyph}
      </svg>
    </span>
  );
}

// 사람마다 다른 색으로 보이는 동그란 이니셜
const AVATAR_COLORS: ReadonlyArray<readonly [string, string]> = [
  ["#E8F3FF", "#1B64DA"],
  ["#E9F9EE", "#12A150"],
  ["#F3ECFF", "#7A3CF0"],
  ["#FFF3E0", "#D98200"],
  ["#FFEEEE", "#D93B49"],
];

export function Avatar({ name, seed, size = 40 }: { readonly name: string; readonly seed: number; readonly size?: number }) {
  const [bg, fg] = AVATAR_COLORS[seed % AVATAR_COLORS.length];

  return (
    <span
      aria-hidden
      style={{
        width: size,
        height: size,
        borderRadius: "50%",
        background: bg,
        color: fg,
        display: "grid",
        placeItems: "center",
        fontWeight: 700,
        fontSize: size * 0.42,
        flex: "none",
      }}
    >
      {name.slice(0, 1)}
    </span>
  );
}

// 확정한 앱 아이콘(design/icons/logo.svg)을 그대로 쓴다.
export function LogoMark({ size = 112 }: { readonly size?: number }) {
  return <img src={logoUrl} alt="살림잇다" width={size} height={size} style={{ borderRadius: size * 0.22, display: "block", boxShadow: "0 8px 22px rgba(27,107,239,.28)" }} />;
}
