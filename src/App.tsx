import { useCallback, useEffect, useRef, useState } from "react";
import { PhoneGlyph } from "@/components/PhoneGlyph";
import { animations, COUNT } from "@/glyph/animations";
import { cn } from "@/utils/cn";

const ACCENT = "#ff2d2d";

const steps = [
  {
    title: "Установите APK",
    body: "Скачайте GlyphPay-release.apk (из GitHub Actions или кнопкой выше), откройте файл на телефоне и разрешите установку из этого источника.",
  },
  {
    title: "Выдайте три доступа",
    body: "В приложении нажмите «Включить» у «Доступа к уведомлениям» и найдите Glyph Pay в списке. Затем «Поверх других окон» и «Работа в фоне → Без ограничений». Все три индикатора должны стать зелёными.",
    note: "Если переключатель уведомлений неактивен: Настройки → Приложения → Glyph Pay → ⋮ (справа вверху) → «Разрешить ограниченные настройки».",
  },
  {
    title: "Выберите анимацию",
    body: "Нажмите на любую карточку — анимация сразу проиграется на Glyph. Яркость регулируется ползунком.",
  },
  {
    title: "Проверьте кнопкой теста",
    body: "«▶ Проверить анимацию» показывает её прямо сейчас. Если Glyph не загорелся — смотрите раздел «Если не работает» ниже (нужна одна команда ADB).",
  },
  {
    title: "Проверьте работу из фона",
    body: "Нажмите «Тест как при оплате», сверните приложение или заблокируйте экран — через 5 секунд Glyph должен загореться. Так вы проверите всю цепочку целиком.",
  },
  {
    title: "Оплатите телефоном",
    body: "Приложите телефон к терминалу через СберПэй, Mir Pay или Google Кошелёк. Как только банк пришлёт уведомление об оплате — Glyph засветится. Если вашего банка нет в списке, добавьте его через «Выбрать приложения оплаты».",
  },
];

const faq = [
  {
    q: "Glyph не загорается даже на кнопке «Проверить»",
    a: "Если у вас Nothing OS 4.0 / Android 16 — ключ не нужен, просто проверьте, что Glyph включён в настройках. На более старой версии включите отладку Glyph с компьютера: «adb shell settings put global nt_glyph_interface_debug_enable 1». Придёт уведомление об активации. Режим выключается сам через 48 часов — команду нужно повторить.",
  },
  {
    q: "Кнопка работает, но при оплате ничего не происходит",
    a: "Откройте блок «Журнал» внизу приложения. Если там строка «пропущено (не похоже на оплату)» — банк прислал текст без слов «оплата/покупка/списание»: выключите переключатель «Только уведомления об оплате». Если журнал пустой — проверьте, что в вашем банке включены push-уведомления об операциях и что приложение банка добавлено в «Выбрать приложения оплаты».",
  },
  {
    q: "Тест из фона не срабатывает",
    a: "Проверьте разрешение «Поверх других окон» и режим батареи «Без ограничений». В журнале появится строка с причиной.",
  },
  {
    q: "Почему анимация идёт не в момент касания терминала?",
    a: "Android не сообщает обычным приложениям, что NFC-оплата прошла. Единственный честный сигнал — уведомление от банка или кошелька. Оно приходит через 1–3 секунды после касания, тогда и стартует анимация.",
  },
  {
    q: "Анимация на миг перекрывает приложение банка",
    a: "Glyph SDK работает только из активного приложения, поэтому на 3 секунды открывается полностью прозрачное окно без касаний и фокуса. Визуально ничего не меняется, а затем оно закрывается само.",
  },
];

const apps = [
  "СберБанк Онлайн / СберПэй",
  "Mir Pay",
  "Т-Банк",
  "ВТБ",
  "Альфа-Банк",
  "Райффайзен",
  "Газпромбанк",
  "Яндекс Банк",
  "Google Кошелёк",
  "Samsung Pay",
  "PayPal",
  "Revolut",
];

function Section({ id, eyebrow, title, children }: { id?: string; eyebrow: string; title: string; children: React.ReactNode }) {
  return (
    <section id={id} className="mx-auto w-full max-w-6xl px-5 py-16 sm:py-20">
      <p className="font-mono text-xs uppercase tracking-[0.25em]" style={{ color: ACCENT }}>
        {eyebrow}
      </p>
      <h2 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">{title}</h2>
      <div className="mt-8">{children}</div>
    </section>
  );
}

function Code({ children }: { children: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <div className="group relative">
      <pre className="overflow-x-auto rounded-2xl border border-white/10 bg-white/[0.04] p-4 pr-20 font-mono text-[13px] leading-relaxed text-zinc-200">
        {children}
      </pre>
      <button
        onClick={() => {
          navigator.clipboard?.writeText(children);
          setCopied(true);
          setTimeout(() => setCopied(false), 1500);
        }}
        className="absolute right-3 top-3 rounded-full border border-white/15 px-3 py-1 text-xs text-zinc-300 transition hover:bg-white/10"
      >
        {copied ? "Скопировано" : "Копировать"}
      </button>
    </div>
  );
}

export default function App() {
  const [animId, setAnimId] = useState(animations[0].id);
  const [brightness, setBrightness] = useState(100);
  const [playing, setPlaying] = useState(false);
  const [levels, setLevels] = useState<number[]>(() => new Array(COUNT).fill(0));
  const [open, setOpen] = useState<number | null>(0);

  const raf = useRef(0);
  const gain = useRef(1);
  gain.current = brightness / 100;

  const stop = useCallback(() => {
    cancelAnimationFrame(raf.current);
    setPlaying(false);
    setLevels(new Array(COUNT).fill(0));
  }, []);

  const play = useCallback((id: string) => {
    cancelAnimationFrame(raf.current);
    const anim = animations.find((a) => a.id === id) ?? animations[0];
    const start = performance.now();
    setPlaying(true);
    const tick = (now: number) => {
      const t = now - start;
      if (t >= anim.durationMs) {
        setLevels(new Array(COUNT).fill(0));
        setPlaying(false);
        return;
      }
      const buf: number[] = new Array(COUNT).fill(0);
      anim.render(t, buf);
      // та же гамма и яркость, что и в приложении
      setLevels(buf.map((v) => Math.min(1, Math.max(0, v)) ** 2 * gain.current));
      raf.current = requestAnimationFrame(tick);
    };
    raf.current = requestAnimationFrame(tick);
  }, []);

  useEffect(() => {
    const t = setTimeout(() => play(animations[0].id), 700);
    return () => {
      clearTimeout(t);
      cancelAnimationFrame(raf.current);
    };
  }, [play]);

  const current = animations.find((a) => a.id === animId) ?? animations[0];

  return (
    <div className="min-h-screen bg-[#08080a] font-sans text-white antialiased selection:bg-[#ff2d2d]/40">
      {/* nav */}
      <header className="sticky top-0 z-30 border-b border-white/5 bg-[#08080a]/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3">
          <a href="#top" className="flex items-center gap-2 font-mono text-sm tracking-widest">
            <span className="inline-block h-2.5 w-2.5 rounded-full" style={{ background: ACCENT }} />
            GLYPH PAY
          </a>
          <nav className="hidden gap-6 text-sm text-zinc-400 sm:flex">
            <a className="hover:text-white" href="#demo">Анимации</a>
            <a className="hover:text-white" href="#guide">Инструкция</a>
            <a className="hover:text-white" href="#build">Сборка</a>
            <a className="hover:text-white" href="#faq">Если не работает</a>
          </nav>
        </div>
      </header>

      {/* hero + demo */}
      <main id="top">
        <section id="demo" className="mx-auto grid w-full max-w-6xl items-center gap-10 px-5 pb-10 pt-12 lg:grid-cols-[1fr_380px] lg:pt-20">
          <div>
            <p className="inline-flex items-center gap-2 rounded-full border border-white/10 px-3 py-1 font-mono text-xs text-zinc-400">
              <span className="h-1.5 w-1.5 rounded-full" style={{ background: ACCENT }} />
              Android · Nothing Phone (2)
            </p>
            <h1 className="mt-5 text-5xl font-semibold leading-[1.05] tracking-tight sm:text-6xl">
              Платите телефоном —<br />
              <span style={{ color: ACCENT }}>Glyph</span> празднует.
            </h1>
            <p className="mt-5 max-w-xl text-lg text-zinc-400">
              Приложение следит за уведомлениями СберПэй, Mir Pay, Google Кошелька и других. Как только оплата прошла — задняя панель
              Nothing Phone (2) загорается красивой анимацией.
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              <a
                href="./GlyphPay.apk"
                download
                className="rounded-full px-6 py-3 text-sm font-medium text-white transition hover:brightness-110"
                style={{ background: ACCENT }}
              >
                Скачать APK
              </a>
              <a href="#guide" className="rounded-full border border-white/15 px-6 py-3 text-sm font-medium transition hover:bg-white/5">
                Как пользоваться
              </a>
            </div>

            {/* animation picker */}
            <div className="mt-10">
              <p className="font-mono text-xs uppercase tracking-[0.25em] text-zinc-500">Выберите анимацию</p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {animations.map((a) => (
                  <button
                    key={a.id}
                    onClick={() => {
                      setAnimId(a.id);
                      play(a.id);
                    }}
                    className={cn(
                      "rounded-2xl border p-4 text-left transition",
                      a.id === animId ? "border-[#ff2d2d] bg-[#ff2d2d]/10" : "border-white/10 bg-white/[0.03] hover:bg-white/[0.06]",
                    )}
                  >
                    <div className="flex items-center gap-2 font-medium">
                      <span className={a.id === animId ? "text-[#ff2d2d]" : "text-zinc-600"}>{a.id === animId ? "●" : "○"}</span>
                      {a.title}
                    </div>
                    <p className="mt-1 text-sm text-zinc-500">{a.description}</p>
                  </button>
                ))}
              </div>

              <div className="mt-5 flex flex-wrap items-center gap-4">
                <button
                  onClick={() => (playing ? stop() : play(current.id))}
                  className="rounded-full bg-white px-6 py-3 text-sm font-medium text-black transition hover:bg-zinc-200"
                >
                  {playing ? "■  Остановить" : "▶  Проверить анимацию"}
                </button>
                <label className="flex items-center gap-3 text-sm text-zinc-400">
                  Яркость {brightness}%
                  <input
                    type="range"
                    min={5}
                    max={100}
                    value={brightness}
                    onChange={(e) => setBrightness(Number(e.target.value))}
                    className="w-32 accent-[#ff2d2d]"
                  />
                </label>
              </div>
              <p className="mt-3 text-xs text-zinc-600">
                Это симулятор в браузере. В приложении такая же кнопка «Проверить анимацию» зажигает настоящий Glyph.
              </p>
            </div>
          </div>

          {/* phone */}
          <div className="relative mx-auto h-[600px] w-[300px] sm:h-[640px] sm:w-[320px]">
            <div
              className="pointer-events-none absolute inset-0 -z-0 rounded-full opacity-40 blur-3xl transition-opacity"
              style={{
                background: "radial-gradient(circle, rgba(255,255,255,0.18), transparent 60%)",
                opacity: playing ? 0.5 : 0.1,
              }}
            />
            <div className="relative h-full w-full drop-shadow-[0_30px_60px_rgba(0,0,0,0.8)]">
              <PhoneGlyph levels={levels} />
            </div>
          </div>
        </section>

        {/* how it works */}
        <Section eyebrow="Как это устроено" title="Три шага от касания до света">
          <div className="grid gap-4 md:grid-cols-3">
            {[
              ["01", "Оплата", "Вы платите телефоном через СберПэй, Mir Pay или кошелёк. Банк присылает push с суммой."],
              ["02", "Уведомление", "Glyph Pay получает его (NotificationListener), проверяет приложение и слова «оплата/покупка/списание»."],
              ["03", "Glyph", "Открывается невидимое окно, и 33 сегмента Glyph проигрывают выбранную анимацию 2–3 секунды."],
            ].map(([n, t, d]) => (
              <div key={n} className="rounded-3xl border border-white/10 bg-white/[0.03] p-6">
                <div className="font-mono text-sm" style={{ color: ACCENT }}>{n}</div>
                <h3 className="mt-3 text-xl font-medium">{t}</h3>
                <p className="mt-2 text-zinc-400">{d}</p>
              </div>
            ))}
          </div>
        </Section>

        {/* guide */}
        <Section id="guide" eyebrow="Инструкция" title="Как пользоваться за 5 минут">
          <ol className="grid gap-4 md:grid-cols-2">
            {steps.map((s, i) => (
              <li key={s.title} className="flex gap-4 rounded-3xl border border-white/10 bg-white/[0.03] p-6">
                <span
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full font-mono text-sm"
                  style={{ background: `${ACCENT}22`, color: ACCENT }}
                >
                  {i + 1}
                </span>
                <div>
                  <h3 className="text-lg font-medium">{s.title}</h3>
                  <p className="mt-1.5 text-zinc-400">{s.body}</p>
                  {s.note && <p className="mt-3 rounded-xl bg-white/5 p-3 text-sm text-zinc-400">💡 {s.note}</p>}
                </div>
              </li>
            ))}
          </ol>

          <div className="mt-10 rounded-3xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="text-lg font-medium">Какие приложения отслеживаются по умолчанию</h3>
            <div className="mt-4 flex flex-wrap gap-2">
              {apps.map((a) => (
                <span key={a} className="rounded-full border border-white/10 px-3 py-1.5 text-sm text-zinc-300">
                  {a}
                </span>
              ))}
            </div>
            <p className="mt-4 text-sm text-zinc-500">Список можно изменить в приложении: любое приложение с уведомлениями об оплатах можно добавить.</p>
          </div>
        </Section>

        {/* build */}
        <Section id="build" eyebrow="Сборка" title="APK собирается в GitHub Actions">
          <div className="grid gap-8 lg:grid-cols-2">
            <div>
              <h3 className="text-lg font-medium">1. Загрузите проект на GitHub</h3>
              <p className="mt-2 mb-4 text-zinc-400">Папка <code className="text-zinc-200">android/</code> — это готовый Android-проект, а <code className="text-zinc-200">.github/workflows/android.yml</code> — сборка.</p>
              <Code>{`git init
git add .
git commit -m "Glyph Pay"
git branch -M main
git remote add origin https://github.com/<ваш-ник>/glyph-pay.git
git push -u origin main`}</Code>

              <h3 className="mt-8 text-lg font-medium">2. Скачайте APK</h3>
              <p className="mt-2 text-zinc-400">
                GitHub → вкладка <b className="text-zinc-200">Actions</b> → «Build Android APK» → в самом низу запуска блок <b className="text-zinc-200">Artifacts → GlyphPay-apk</b>.
                Сборка занимает около 3 минут.
              </p>
            </div>
            <div>
              <h3 className="text-lg font-medium">Релиз с готовым APK (по желанию)</h3>
              <p className="mt-2 mb-4 text-zinc-400">Создайте тег — APK автоматически появится в разделе Releases:</p>
              <Code>{`git tag v1.0.0
git push --tags`}</Code>

              <h3 className="mt-8 text-lg font-medium">Локальная сборка</h3>
              <p className="mt-2 mb-4 text-zinc-400">Нужны JDK 17 и Android SDK 36 (или просто откройте папку android/ в Android Studio):</p>
              <Code>{`cd android
./gradlew assembleRelease
# app/build/outputs/apk/release/GlyphPay-release.apk`}</Code>
            </div>
          </div>
          <div className="mt-8 rounded-3xl border border-white/10 bg-white/[0.03] p-6 text-zinc-400">
            <b className="text-zinc-200">Уже собрано.</b> Проект собран на этой машине (Gradle 8.11, AGP 8.9, Android 16 SDK) без ошибок — APK весит около 5 МБ и подписан отладочным ключом, поэтому устанавливается без дополнительных шагов.
          </div>
        </Section>

        {/* faq */}
        <Section id="faq" eyebrow="Если не работает" title="Частые вопросы и ограничения">
          <div className="divide-y divide-white/10 overflow-hidden rounded-3xl border border-white/10">
            {faq.map((f, i) => (
              <div key={f.q} className="bg-white/[0.02]">
                <button
                  onClick={() => setOpen(open === i ? null : i)}
                  className="flex w-full items-center justify-between gap-4 px-6 py-5 text-left text-lg font-medium transition hover:bg-white/[0.04]"
                >
                  {f.q}
                  <span className={cn("text-2xl text-zinc-500 transition-transform", open === i && "rotate-45")}>+</span>
                </button>
                {open === i && <p className="px-6 pb-6 text-zinc-400">{f.a}</p>}
              </div>
            ))}
          </div>
          <Code>{`adb shell settings put global nt_glyph_interface_debug_enable 1`}</Code>
        </Section>
      </main>

      <footer className="border-t border-white/5 px-5 py-10 text-center text-sm text-zinc-600">
        Glyph Pay — неофициальное приложение. Использует официальный Glyph Developer Kit от Nothing. Данные уведомлений не покидают телефон.
      </footer>
    </div>
  );
}
