// Порт анимаций Android-приложения (Animations.kt) для живого предпросмотра на сайте.
// Индексы каналов совпадают с официальной раскладкой Glyph у Nothing Phone (2).

export const COUNT = 33;
export const A1 = 0;
export const A2 = 1;
export const B1 = 2;
export const E1 = 24;
export const C1 = Array.from({ length: 16 }, (_, i) => 3 + i);
export const RING = Array.from({ length: 5 }, (_, i) => 19 + i);
export const D1 = Array.from({ length: 8 }, (_, i) => 25 + i);

export type GlyphAnimation = {
  id: string;
  title: string;
  description: string;
  durationMs: number;
  render: (t: number, o: number[]) => void;
};

const TWO_PI = Math.PI * 2;
const clamp01 = (x: number) => (x < 0 ? 0 : x > 1 ? 1 : x);
const tri = (x: number) => (x < 0 || x > 1 ? 0 : 1 - Math.abs(2 * x - 1));
const lit = (o: number[], i: number, v: number) => {
  if (v > o[i]) o[i] = v;
};
const litAll = (o: number[], v: number) => {
  for (let i = 0; i < o.length; i++) lit(o, i, v);
};
const scale = (o: number[], f: number) => {
  for (let i = 0; i < o.length; i++) o[i] *= f;
};
const ringComet = (pos: number, k: number, n: number, tail: number) => {
  const d = (((pos - k) % n) + n) % n;
  return d < tail ? 1 - d / tail : 0;
};

const wave: GlyphAnimation = {
  id: "wave",
  title: "Волна оплаты",
  description: "Касание → волна по кольцу → шкала заполняется → двойная вспышка успеха",
  durationMs: 2900,
  render: (t, o) => {
    if (t < 1900) {
      if (t < 1000) lit(o, E1, 0.5 - 0.5 * Math.cos((TWO_PI * t) / 500));
      else lit(o, E1, 0.35);
      RING.forEach((idx, i) => lit(o, idx, tri((t - (250 + i * 140)) / 450)));
      lit(o, A1, tri((t - 500) / 400));
      lit(o, A2, tri((t - 600) / 400));
      lit(o, B1, tri((t - 700) / 400));
      if (t >= 900) {
        const n = ((t - 900) / 1000) * 8;
        D1.forEach((idx, i) => lit(o, idx, clamp01(n - i)));
        const head = ((t - 900) / 1000) * 21;
        C1.forEach((idx, k) => {
          const d = head - k;
          if (d >= 0 && d < 5) lit(o, idx, 1 - d / 5);
        });
      }
    } else {
      const u = (t - 1900) / 1000;
      const s = (0.5 + 0.5 * Math.cos(3 * Math.PI * u)) * (1 - u * 0.5);
      litAll(o, clamp01(s) * clamp01((2900 - t) / 400));
    }
  },
};

const comet: GlyphAnimation = {
  id: "comet",
  title: "Комета",
  description: "Светящаяся комета бегает по периметру, шкала и кольцо подхватывают",
  durationMs: 3200,
  render: (t, o) => {
    const head = ((t % 1600) / 1600) * 16;
    C1.forEach((idx, k) => lit(o, idx, ringComet(head, k, 16, 6)));
    const headD = ((t % 800) / 800) * 8;
    D1.forEach((idx, i) => lit(o, idx, ringComet(headD, 7 - i, 8, 4)));
    RING.forEach((idx, i) => {
      const d = (((t / 200 - i) % 5) + 5) % 5;
      lit(o, idx, clamp01(1 - d / 1.5));
    });
    const blink = 0.5 - 0.5 * Math.cos((TWO_PI * t) / 400);
    lit(o, A1, blink);
    lit(o, A2, 1 - blink);
    lit(o, B1, 0.4);
    scale(o, clamp01(t / 250) * clamp01((3200 - t) / 500));
  },
};

const fill: GlyphAnimation = {
  id: "fill",
  title: "Заряд",
  description: "Обе линии заполняются как индикатор зарядки, затем три вспышки",
  durationMs: 2800,
  render: (t, o) => {
    if (t < 1500) {
      const n = clamp01(t / 1200);
      C1.forEach((idx, k) => lit(o, idx, clamp01(n * 16 - k)));
      D1.forEach((idx, i) => lit(o, idx, clamp01(n * 8 - i)));
      RING.forEach((idx, i) => lit(o, idx, clamp01((t - 1000 - i * 70) / 120)));
      lit(o, E1, clamp01((t - 400) / 200));
      lit(o, A1, clamp01((t - 1100) / 150));
      lit(o, A2, clamp01((t - 1200) / 150));
      lit(o, B1, clamp01((t - 1300) / 150));
    } else {
      const u = (t - 1500) / 1300;
      const s = (0.5 + 0.5 * Math.cos(TWO_PI * 3 * u)) * (1 - u);
      litAll(o, clamp01(s));
    }
  },
};

const sparks: GlyphAnimation = {
  id: "sparks",
  title: "Искры",
  description: "Случайные яркие вспышки по всему Glyph, как фейерверк",
  durationMs: 2800,
  render: (t, o) => {
    for (let i = 0; i < COUNT; i++) {
      for (let k = 0; k < 3; k++) {
        const h = ((i * 7919 + k * 104729 + 13) % 997) / 997;
        const start = h * 2100;
        if (t >= start) lit(o, i, Math.exp(-(t - start) / 170));
      }
    }
  },
};

const breath: GlyphAnimation = {
  id: "breath",
  title: "Дыхание",
  description: "Мягкое «дыхание» всей подсветки с бегущей фазой — спокойный вариант",
  durationMs: 3200,
  render: (t, o) => {
    const level = (off: number) => 0.5 - 0.5 * Math.cos(TWO_PI * (t / 1600 - off));
    lit(o, A1, level(0));
    lit(o, A2, level(0.03));
    lit(o, B1, level(0.06));
    C1.forEach((idx, k) => lit(o, idx, level(0.02 + k * 0.006)));
    RING.forEach((idx, i) => lit(o, idx, level(0.1 + i * 0.02)));
    lit(o, E1, level(0.2));
    D1.forEach((idx, i) => lit(o, idx, level(0.12 + i * 0.01)));
    scale(o, clamp01(t / 400) * clamp01((3200 - t) / 300));
  },
};

export const animations: GlyphAnimation[] = [wave, comet, fill, sparks, breath];
