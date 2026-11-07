// Порт Kotlin-анимаций (GlyphAnimations.kt) для браузерного симулятора.
// Индексы каналов Phone (2): A1=0, A2=1, B1=2, C1_1..C1_16=3..18, C2..C6=19..23, E1=24, D1_1..D1_8=25..32

export const CHANNELS = 33;

const PATH: number[] = (() => {
  const p: number[] = [0, 1, 2];
  for (let i = 18; i >= 3; i--) p.push(i);
  for (let i = 19; i <= 23; i++) p.push(i);
  for (let i = 32; i >= 25; i--) p.push(i);
  p.push(24);
  return p;
})();

export type StyleId = "PAY" | "COMET" | "BREATH" | "SPARKLE";

export const STYLES: { id: StyleId; title: string; short: string; duration: number }[] = [
  { id: "PAY", title: "Оплата прошла", short: "волны + галочка", duration: 3700 },
  { id: "COMET", title: "Комета", short: "двойной пролёт", duration: 2800 },
  { id: "BREATH", title: "Дыхание", short: "плавные вдохи", duration: 3600 },
  { id: "SPARKLE", title: "Искры", short: "мерцание", duration: 3200 },
];

const gauss = (x: number, w: number) => Math.exp(-(x * x) / (w * w));

function pay(pos: number, t: number): number {
  let v = 0;
  if (t < 1900) {
    for (let k = 0; k <= 1; k++) {
      const local = t - k * 750;
      if (local < 0) continue;
      const head = (local / 800) * 1.3 - 0.1;
      const peak = k === 0 ? 1 : 0.75;
      v = Math.max(v, peak * gauss(pos - head, 0.11));
    }
  }
  if (t >= 1700 && t < 2400) {
    const f = ((t - 1700) / 600) * 1.25;
    v = Math.max(v, Math.min(1, Math.max(0, (f - pos) / 0.25)));
  }
  if (t >= 2400 && t < 2900) {
    const x = (t - 2400) / 500;
    v = Math.max(v, 0.65 + 0.35 * Math.cos(x * Math.PI * 4));
  }
  if (t >= 2900 && t < 3700) {
    const f = 1 - (t - 2900) / 800;
    v = Math.max(v, Math.min(1, Math.max(0, f * 1.3 - pos * 0.3)));
  }
  return v;
}

function comet(pos: number, t: number): number {
  const tail = (d: number) => (d < 0 ? gauss(d, 0.03) : Math.exp(-d / 0.14));
  const h1 = (t / 1500) * 1.5 - 0.2;
  let v = tail(h1 - pos);
  const t2 = t - 900;
  if (t2 >= 0) {
    const h2 = (t2 / 1500) * 1.5 - 0.2;
    v = Math.max(v, tail(h2 - (1 - pos)));
  }
  return v < 0.02 ? 0 : v;
}

function breath(pos: number, t: number): number {
  const x = t / 1500 - pos * 0.35;
  if (x < 0 || x > 2) return 0;
  const s = Math.sin(Math.PI * x);
  return s * s;
}

function sparkle(idx: number, t: number): number {
  let v = 0;
  const starts = [(idx * 7919) % 2000, 800 + ((idx * 6007) % 1600)];
  for (const s of starts) {
    const x = (t - s) / 700;
    if (x >= 0 && x <= 1) {
      const rise = Math.min(1, x * 8);
      v = Math.max(v, rise * (1 - x) * (1 - x));
    }
  }
  return v;
}

/** Возвращает яркость 0..1 для каждого из 33 каналов. */
export function render(style: StyleId, t: number): number[] {
  const out = new Array<number>(CHANNELS).fill(0);
  const n = PATH.length;
  for (let i = 0; i < n; i++) {
    const pos = i / (n - 1);
    let v = 0;
    if (style === "PAY") v = pay(pos, t);
    else if (style === "COMET") v = comet(pos, t);
    else if (style === "BREATH") v = breath(pos, t);
    else v = sparkle(i, t);
    out[PATH[i]] = Math.pow(Math.min(1, Math.max(0, v)), 1.8);
  }
  return out;
}

// ---------- геометрия схематичного Phone (2) ----------

export interface Seg {
  ch: number;
  d: string;
  dot?: { x: number; y: number };
}

const rad = (a: number) => (a * Math.PI) / 180;

function arc(cx: number, cy: number, r: number, a0: number, a1: number): string {
  const x0 = cx + r * Math.cos(rad(a0));
  const y0 = cy + r * Math.sin(rad(a0));
  const x1 = cx + r * Math.cos(rad(a1));
  const y1 = cy + r * Math.sin(rad(a1));
  const large = Math.abs(a1 - a0) > 180 ? 1 : 0;
  const sweep = a1 > a0 ? 1 : 0;
  return `M${x0.toFixed(1)},${y0.toFixed(1)} A${r},${r} 0 ${large} ${sweep} ${x1.toFixed(1)},${y1.toFixed(1)}`;
}

export const SEGMENTS: Seg[] = (() => {
  const segs: Seg[] = [];
  // A1, A2 — дуги вокруг камеры
  segs.push({ ch: 0, d: arc(92, 92, 62, 175, 232) });
  segs.push({ ch: 1, d: arc(92, 92, 62, 240, 297) });
  // B1 — диагональ справа сверху
  segs.push({ ch: 2, d: "M212,62 L248,134" });
  // C1_1..C1_16: от нижнего правого угла кольца до верхнего левого
  const cx = 150, cy = 350, R = 96;
  for (let k = 0; k < 16; k++) {
    const start = 52 + k * 12;
    segs.push({ ch: 3 + k, d: arc(cx, cy, R, start, start + 9) });
  }
  // C2..C6 — остаток кольца (против часовой стрелки от C1_1)
  for (let j = 0; j < 5; j++) {
    const centre = 24 - j * 32;
    segs.push({ ch: 19 + j, d: arc(cx, cy, R, centre + 12, centre - 12) });
  }
  // D1_1..D1_8 — вертикальная линия внизу (D1_1 — самый нижний)
  for (let k = 0; k < 8; k++) {
    const y = 548 - k * 11;
    segs.push({ ch: 25 + k, d: `M196,${y} L196,${y - 7}` });
  }
  // E1 — точка
  segs.push({ ch: 24, d: "M196,567 L196,567.1" });
  return segs;
})();
