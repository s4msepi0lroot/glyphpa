import { A1, A2, B1, C1, D1, E1, RING } from "@/glyph/animations";

type Seg =
  | { kind: "path"; idx: number; d: string; n: number; k: number; w: number }
  | { kind: "dot"; idx: number; cx: number; cy: number; r: number };

const A1_D = "M 36 62 C 26 100 26 140 40 194";
const A2_D = "M 108 96 C 120 128 116 162 100 194";
const B1_D = "M 232 54 L 264 98";
const C1_D = "M 216 540 L 112 540 Q 44 540 44 472 L 44 242 Q 44 216 64 208";
const RING_D = "M 212 360 A 62 62 0 1 1 88 360 A 62 62 0 1 1 212 360";
const D1_D = "M 254 470 L 254 270";

const segments: Seg[] = [
  { kind: "path", idx: A1, d: A1_D, n: 1, k: 0, w: 4 },
  { kind: "path", idx: A2, d: A2_D, n: 1, k: 0, w: 4 },
  { kind: "path", idx: B1, d: B1_D, n: 1, k: 0, w: 4 },
  ...C1.map((idx, k): Seg => ({ kind: "path", idx, d: C1_D, n: 16, k, w: 5 })),
  ...RING.map((idx, k): Seg => ({ kind: "path", idx, d: RING_D, n: 5, k, w: 5 })),
  ...D1.map((idx, k): Seg => ({ kind: "path", idx, d: D1_D, n: 8, k, w: 5 })),
  { kind: "dot", idx: E1, cx: 254, cy: 506, r: 4.5 },
];

function dash(s: Extract<Seg, { kind: "path" }>) {
  if (s.n === 1) return {};
  const len = s.n === 5 ? 0.86 : 0.8;
  return {
    pathLength: s.n,
    strokeDasharray: `${len} ${s.n - len}`,
    strokeDashoffset: -(s.k + (1 - len) / 2),
  };
}

/** Спина Nothing Phone (2) с 33 сегментами Glyph. levels — яркости 0..1 (после гаммы). */
export function PhoneGlyph({ levels }: { levels: number[] }) {
  const renderSeg = (s: Seg, layer: "base" | "glow" | "core") => {
    const v = levels[s.idx] ?? 0;
    if (layer !== "base" && v < 0.01) return null;
    const stroke =
      layer === "base" ? "#2b2b30" : "#ffffff";
    const opacity = layer === "base" ? 1 : v;
    const extra = layer === "glow" ? 3 : 0;

    if (s.kind === "dot") {
      return (
        <circle
          key={`${layer}-${s.idx}`}
          cx={s.cx}
          cy={s.cy}
          r={s.r + extra}
          fill={stroke}
          opacity={opacity}
        />
      );
    }
    return (
      <path
        key={`${layer}-${s.idx}`}
        d={s.d}
        fill="none"
        stroke={stroke}
        strokeWidth={s.w + extra * 2}
        strokeLinecap={s.n === 1 ? "round" : "butt"}
        opacity={opacity}
        {...dash(s)}
      />
    );
  };

  return (
    <svg viewBox="0 0 300 600" className="h-full w-full" role="img" aria-label="Glyph на Nothing Phone (2)">
      <defs>
        <linearGradient id="body" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#1b1b1f" />
          <stop offset="1" stopColor="#0a0a0c" />
        </linearGradient>
        <filter id="blur" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="6" />
        </filter>
      </defs>

      <rect x="10" y="10" width="280" height="580" rx="46" fill="url(#body)" stroke="#2e2e33" strokeWidth="2" />

      {/* камеры */}
      <rect x="46" y="50" width="56" height="140" rx="28" fill="#060607" stroke="#2c2c31" strokeWidth="2" />
      <circle cx="74" cy="88" r="17" fill="#0e0e12" stroke="#3a3a40" strokeWidth="3" />
      <circle cx="74" cy="148" r="17" fill="#0e0e12" stroke="#3a3a40" strokeWidth="3" />
      <circle cx="118" cy="72" r="4" fill="#3a3a40" />

      {/* катушка зарядки */}
      <circle cx="150" cy="360" r="40" fill="none" stroke="#1e1e22" strokeWidth="2" />

      <text x="150" y="580" textAnchor="middle" fontSize="9" fill="#3c3c42" letterSpacing="3" fontFamily="ui-monospace, monospace">
        NOTHING
      </text>

      <g>{segments.map((s) => renderSeg(s, "base"))}</g>
      <g filter="url(#blur)">{segments.map((s) => renderSeg(s, "glow"))}</g>
      <g>{segments.map((s) => renderSeg(s, "core"))}</g>
    </svg>
  );
}
