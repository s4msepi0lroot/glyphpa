import { SEGMENTS } from "../glyph";

interface Props {
  levels: number[];
  nfcActive?: boolean;
}

export default function PhoneSim({ levels, nfcActive }: Props) {
  return (
    <div className="relative mx-auto w-[260px] sm:w-[300px]">
      {/* свечение под телефоном */}
      <div
        className="pointer-events-none absolute inset-0 -z-10 rounded-[3rem] blur-3xl transition-opacity duration-150"
        style={{
          background: "radial-gradient(circle at 50% 55%, rgba(255,255,255,0.25), transparent 65%)",
          opacity: Math.min(1, levels.reduce((a, b) => a + b, 0) / 8),
        }}
      />
      <svg viewBox="0 0 300 600" className="w-full drop-shadow-2xl">
        <defs>
          <linearGradient id="body" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#2b2b2e" />
            <stop offset="1" stopColor="#141416" />
          </linearGradient>
          <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="4" result="b" />
            <feMerge>
              <feMergeNode in="b" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        {/* корпус */}
        <rect x="6" y="6" width="288" height="588" rx="44" fill="url(#body)" stroke="#3a3a3e" strokeWidth="2" />
        <rect x="14" y="14" width="272" height="572" rx="38" fill="#18181a" opacity="0.65" />

        {/* камеры */}
        <circle cx="92" cy="92" r="30" fill="#0b0b0c" stroke="#333" strokeWidth="2" />
        <circle cx="92" cy="92" r="17" fill="#050506" stroke="#444" strokeWidth="1.5" />
        <circle cx="92" cy="92" r="6" fill="#1b2a44" />
        <circle cx="150" cy="60" r="11" fill="#0b0b0c" stroke="#333" strokeWidth="2" />

        {/* катушка беспроводной зарядки */}
        <circle cx="150" cy="350" r="70" fill="none" stroke="#222226" strokeWidth="1.5" />

        {/* NFC-волны */}
        {nfcActive && (
          <g fill="none" stroke="#7dd3fc" strokeLinecap="round">
            <circle cx="92" cy="92" r="40" strokeWidth="1.5" className="nfc-ring" />
            <circle cx="92" cy="92" r="40" strokeWidth="1.5" className="nfc-ring" style={{ animationDelay: "0.5s" }} />
          </g>
        )}

        {/* выключенные сегменты */}
        <g fill="none" stroke="#2c2c30" strokeWidth="7" strokeLinecap="round">
          {SEGMENTS.map((s) => (
            <path key={`off-${s.ch}`} d={s.d} />
          ))}
        </g>

        {/* включённые сегменты */}
        <g fill="none" stroke="#ffffff" strokeWidth="7" strokeLinecap="round" filter="url(#glow)">
          {SEGMENTS.map((s) => (
            <path key={`on-${s.ch}`} d={s.d} opacity={levels[s.ch] ?? 0} />
          ))}
        </g>

        <text x="150" y="448" textAnchor="middle" fontSize="9" fill="#55555a" fontFamily="monospace" letterSpacing="3">
          NOTHING
        </text>
      </svg>
    </div>
  );
}
