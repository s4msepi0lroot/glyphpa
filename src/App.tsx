import { useCallback, useEffect, useRef, useState } from "react";
import PhoneSim from "./components/PhoneSim";
import { CHANNELS, STYLES, render, type StyleId } from "./glyph";

const ZERO = new Array<number>(CHANNELS).fill(0);

function CodeBlock({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <div className="group relative mt-3 overflow-hidden rounded-xl border border-white/10 bg-white/[0.03]">
      <pre className="overflow-x-auto p-4 pr-20 font-dot text-[13px] leading-relaxed text-emerald-300">{code}</pre>
      <button
        onClick={() => {
          navigator.clipboard?.writeText(code).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
          });
        }}
        className="absolute right-2 top-2 rounded-md border border-white/10 bg-black/60 px-2.5 py-1 text-xs text-white/70 transition hover:text-white"
      >
        {copied ? "Скопировано" : "Копировать"}
      </button>
    </div>
  );
}

function Step({ n, title, children }: { n: number; title: string; children: React.ReactNode }) {
  return (
    <li className="relative flex gap-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
      <div className="font-dot flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#e8232b] text-sm font-bold">
        {n}
      </div>
      <div className="min-w-0 flex-1">
        <h3 className="text-lg font-semibold">{title}</h3>
        <div className="mt-2 space-y-2 text-[15px] leading-relaxed text-white/70">{children}</div>
      </div>
    </li>
  );
}

function Tag({ children }: { children: React.ReactNode }) {
  return <code className="rounded bg-white/10 px-1.5 py-0.5 font-dot text-[13px] text-white">{children}</code>;
}

export default function App() {
  const [style, setStyle] = useState<StyleId>("PAY");
  const [levels, setLevels] = useState<number[]>(ZERO);
  const [playing, setPlaying] = useState(false);
  const [nfc, setNfc] = useState(false);
  const [notif, setNotif] = useState(false);
  const raf = useRef<number | null>(null);
  const timers = useRef<number[]>([]);

  const stopAll = useCallback(() => {
    if (raf.current) cancelAnimationFrame(raf.current);
    raf.current = null;
    timers.current.forEach((t) => clearTimeout(t));
    timers.current = [];
  }, []);

  const play = useCallback(
    (s: StyleId) => {
      if (raf.current) cancelAnimationFrame(raf.current);
      const duration = STYLES.find((x) => x.id === s)!.duration;
      const start = performance.now();
      setPlaying(true);
      const tick = (now: number) => {
        const t = now - start;
        if (t >= duration) {
          setLevels(ZERO);
          setPlaying(false);
          raf.current = null;
          return;
        }
        setLevels(render(s, t));
        raf.current = requestAnimationFrame(tick);
      };
      raf.current = requestAnimationFrame(tick);
    },
    [],
  );

  const simulatePayment = () => {
    stopAll();
    setLevels(ZERO);
    setPlaying(true);
    setNfc(true);
    timers.current.push(
      window.setTimeout(() => setNotif(true), 900),
      window.setTimeout(() => {
        setNfc(false);
        play(style);
      }, 1500),
      window.setTimeout(() => setNotif(false), 5200),
    );
  };

  useEffect(() => stopAll, [stopAll]);

  const current = STYLES.find((s) => s.id === style)!;

  return (
    <div className="min-h-screen bg-black text-white">
      {/* NAV */}
      <header className="sticky top-0 z-30 border-b border-white/10 bg-black/70 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3">
          <div className="font-dot flex items-center gap-2 text-sm font-bold">
            <span className="inline-block h-2.5 w-2.5 rounded-full bg-[#e8232b]" />
            GLYPH · NFC PAY
          </div>
          <nav className="hidden gap-6 text-sm text-white/60 sm:flex">
            <a href="#demo" className="hover:text-white">Демо</a>
            <a href="#how" className="hover:text-white">Как работает</a>
            <a href="#guide" className="hover:text-white">Инструкция</a>
            <a href="#actions" className="hover:text-white">Сборка</a>
            <a href="#faq" className="hover:text-white">FAQ</a>
          </nav>
        </div>
      </header>

      {/* HERO + SIM */}
      <section id="demo" className="dot-bg relative overflow-hidden">
        <div className="mx-auto grid max-w-6xl items-center gap-10 px-5 py-14 lg:grid-cols-2 lg:py-20">
          <div>
            <p className="font-dot text-xs uppercase tracking-[0.3em] text-[#e8232b]">Nothing Phone (2)</p>
            <h1 className="mt-4 text-4xl font-bold leading-tight sm:text-5xl">
              Оплатил телефоном —<br />
              <span className="text-white/50">Glyph сказал «спасибо».</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg text-white/65">
              Android-приложение, которое ловит оплату через NFC (СберПэй, Mir Pay, Т-Банк, Google Кошелёк и другие)
              и запускает красивую анимацию на Glyph-подсветке. Ниже — живой симулятор: те же формулы анимаций, что и в
              приложении.
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              <button
                onClick={() => {
                  stopAll();
                  setNotif(false);
                  setNfc(false);
                  play(style);
                }}
                className="rounded-full bg-[#e8232b] px-6 py-3 font-semibold transition hover:bg-[#ff3b43] active:scale-95"
              >
                ▶ Тест анимации
              </button>
              <button
                onClick={simulatePayment}
                disabled={playing}
                className="rounded-full border border-white/20 px-6 py-3 font-semibold transition hover:bg-white/10 active:scale-95 disabled:opacity-50"
              >
                💳 Имитация оплаты
              </button>
            </div>

            <div className="mt-8">
              <div className="font-dot mb-3 text-xs uppercase tracking-widest text-white/40">Стиль анимации</div>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {STYLES.map((s) => (
                  <button
                    key={s.id}
                    onClick={() => {
                      setStyle(s.id);
                      stopAll();
                      setNotif(false);
                      setNfc(false);
                      play(s.id);
                    }}
                    className={`rounded-xl border px-3 py-3 text-left transition ${
                      style === s.id
                        ? "border-[#e8232b] bg-[#e8232b]/10"
                        : "border-white/10 bg-white/[0.03] hover:border-white/30"
                    }`}
                  >
                    <div className="text-sm font-semibold">{s.title}</div>
                    <div className="text-xs text-white/50">{s.short}</div>
                  </button>
                ))}
              </div>
              <p className="mt-3 text-sm text-white/45">
                Выбрано: <span className="text-white/80">{current.title}</span> · {(current.duration / 1000).toFixed(1)} c.
                В приложении есть ещё режим «Случайная».
              </p>
            </div>
          </div>

          <div className="relative">
            {/* push-уведомление */}
            <div className="pointer-events-none absolute left-1/2 top-0 z-20 w-[290px] -translate-x-1/2 sm:w-[340px]">
              {notif && (
                <div className="slide-in rounded-2xl border border-white/10 bg-zinc-900/95 p-3 shadow-xl backdrop-blur">
                  <div className="flex items-center gap-2 text-xs text-white/50">
                    <span className="inline-block h-4 w-4 rounded bg-emerald-500" />
                    СберБанк Онлайн · сейчас
                  </div>
                  <div className="mt-1 text-sm font-semibold">Покупка 349 ₽</div>
                  <div className="text-xs text-white/60">МИР-4821 · ВКУСВИЛЛ · Баланс 12 480 ₽</div>
                </div>
              )}
            </div>
            <div className="pt-4">
              <PhoneSim levels={levels} nfcActive={nfc} />
            </div>
            <p className="mt-4 text-center text-xs text-white/35">
              Схематичное изображение Glyph Phone (2): 33 сегмента (A, B, C1–C6, D1, E1)
            </p>
          </div>
        </div>
      </section>

      {/* HOW */}
      <section id="how" className="border-t border-white/10">
        <div className="mx-auto max-w-6xl px-5 py-16">
          <h2 className="text-3xl font-bold">Как это работает</h2>
          <p className="mt-3 max-w-3xl text-white/60">
            Android не даёт сторонним приложениям «подслушивать» NFC-платёж напрямую — платёжные данные защищены. Поэтому
            приложение реагирует на push-уведомление об оплате — оно приходит в тот же момент, когда терминал принял карту.
          </p>
          <div className="mt-8 grid gap-4 md:grid-cols-3">
            {[
              {
                t: "1. Уведомление",
                d: "NotificationListenerService видит уведомление банка/кошелька («Покупка 349 ₽…») и проверяет его по ключевым словам.",
                i: "🔔",
              },
              {
                t: "2. Невидимая Activity",
                d: "Glyph SDK работает только в foreground, поэтому на 3–4 секунды показывается прозрачная Activity (не перехватывает касания).",
                i: "👻",
              },
              {
                t: "3. Анимация",
                d: "Кадры на 25 fps отправляются в Glyph через официальный Nothing Glyph SDK и плавно гаснут.",
                i: "✨",
              },
            ].map((c) => (
              <div key={c.t} className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
                <div className="text-3xl">{c.i}</div>
                <h3 className="mt-3 text-lg font-semibold">{c.t}</h3>
                <p className="mt-2 text-sm leading-relaxed text-white/60">{c.d}</p>
              </div>
            ))}
          </div>

          <div className="mt-8 rounded-2xl border border-amber-400/30 bg-amber-400/5 p-5 text-sm leading-relaxed text-amber-100/80">
            <b className="text-amber-200">Важно знать.</b> Приложение не отличает оплату NFC от других покупок, о которых
            банк присылает уведомление (например, онлайн-покупка картой). Для большинства сценариев это не мешает — анимация
            просто сработает на любую «Покупку/Оплату». Входящие переводы, возвраты и коды подтверждения игнорируются.
          </div>
        </div>
      </section>

      {/* GUIDE */}
      <section id="guide" className="border-t border-white/10 bg-white/[0.02]">
        <div className="mx-auto max-w-4xl px-5 py-16">
          <h2 className="text-3xl font-bold">Инструкция: как пользоваться</h2>
          <ol className="mt-8 space-y-4">
            <Step n={1} title="Получите APK">
              <p>
                Соберите APK через GitHub Actions (см. раздел «Сборка» ниже). Готовый файл{" "}
                <Tag>GlyphNfcPay-release.apk</Tag> лежит в артефактах запуска или в Releases.
              </p>
            </Step>

            <Step n={2} title="Установите на Nothing Phone (2)">
              <p>
                Скачайте APK на телефон, откройте его и разрешите установку из этого источника. Нужен Android 14 или новее
                (Nothing OS 2.5+).
              </p>
            </Step>

            <Step n={3} title="Включите доступ к Glyph SDK">
              <p>
                Для сторонних приложений Glyph SDK требует разрешения. Самый простой способ для себя — debug-режим. Подключите
                телефон к компьютеру (включена «Отладка по USB») и выполните:
              </p>
              <CodeBlock code="adb shell settings put global nt_glyph_interface_debug_enable 1" />
              <p>
                После этого придёт уведомление об активации. Режим автоматически выключается через 48 часов — команду нужно
                повторять. На Android 16 (Nothing OS 4.0) и новее ключ получать не нужно — приложение уже собрано под
                targetSdk 36.
              </p>
              <p>
                Постоянный вариант: получить API-ключ у Nothing и добавить его в GitHub Secrets под именем{" "}
                <Tag>NOTHING_API_KEY</Tag>.
              </p>
            </Step>

            <Step n={4} title="Разрешите «ограниченные настройки»">
              <p>
                На Android 13+ для APK, установленных вручную, доступ к уведомлениям заблокирован. Нажмите в приложении кнопку
                «0. Разрешить ограниченные настройки» → ⋮ в правом верхнем углу → <b>«Разрешить ограниченные настройки»</b>.
              </p>
            </Step>

            <Step n={5} title="Выдайте 3 разрешения в приложении">
              <ul className="list-disc space-y-1 pl-5">
                <li>
                  <b>Доступ к уведомлениям</b> — включите переключатель «Glyph NFC Pay».
                </li>
                <li>
                  <b>Поверх других окон</b> — нужно, чтобы запустить анимацию из фона.
                </li>
                <li>
                  <b>Работа в фоне</b> — снять ограничения батареи, чтобы система не убивала сервис.
                </li>
              </ul>
              <p>Вернитесь в приложение — все три строки должны показать ✅.</p>
            </Step>

            <Step n={6} title="Проверьте анимацию">
              <p>
                Нажмите <b>«▶ Проверить анимацию»</b> — Glyph сразу проиграет выбранный стиль. Затем{" "}
                <b>«💳 Тест как при оплате»</b>: сверните приложение, через 5 секунд анимация запустится так же, как при
                реальной оплате.
              </p>
            </Step>

            <Step n={7} title="Платите!">
              <p>
                Приложите телефон к терминалу через СберПэй, Mir Pay, Google Кошелёк, Т-Банк и т.д. Как только придёт
                уведомление об оплате — Glyph оживёт. В блоке «Последнее срабатывание» можно увидеть, какое приложение
                сработало.
              </p>
              <p>
                Если ваш банк не в списке — добавьте его <Tag>package name</Tag> в поле «Свои приложения» (узнать его можно
                в «Информация о приложении» или в адресе Google Play/RuStore).
              </p>
            </Step>
          </ol>

          <div className="mt-10 rounded-2xl border border-white/10 p-6">
            <h3 className="text-lg font-semibold">Поддерживаемые приложения «из коробки»</h3>
            <div className="mt-4 flex flex-wrap gap-2 text-sm">
              {[
                "СберБанк Онлайн",
                "Mir Pay",
                "Google Кошелёк",
                "Т-Банк",
                "ВТБ",
                "Альфа-Банк",
                "Газпромбанк",
                "Райффайзен",
                "Яндекс Банк",
                "Ozon Банк",
                "QIWI",
                "SMS от 900 и др.",
              ].map((x) => (
                <span key={x} className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-white/75">
                  {x}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ACTIONS */}
      <section id="actions" className="border-t border-white/10">
        <div className="mx-auto max-w-4xl px-5 py-16">
          <h2 className="text-3xl font-bold">Сборка в GitHub Actions</h2>
          <p className="mt-3 text-white/60">
            Workflow лежит в <Tag>.github/workflows/build-android.yml</Tag>. Он собирает debug и release APK на каждый push, а
            по тегу <Tag>v*</Tag> ещё и публикует релиз.
          </p>

          <ol className="mt-8 space-y-4">
            <Step n={1} title="Создайте репозиторий и загрузите проект">
              <CodeBlock
                code={`git init
git add .
git commit -m "Glyph NFC Pay"
git branch -M main
git remote add origin https://github.com/<ваш-логин>/glyph-nfc-pay.git
git push -u origin main`}
              />
            </Step>
            <Step n={2} title="Дождитесь сборки">
              <p>
                Откройте вкладку <b>Actions</b> → «Build Android APK». Через 3–5 минут внизу страницы запуска появится
                артефакт <Tag>GlyphNfcPay-apk</Tag> — скачайте и распакуйте ZIP.
              </p>
            </Step>
            <Step n={3} title="(Опционально) Релиз со ссылкой на APK">
              <CodeBlock
                code={`git tag v1.0.0
git push origin v1.0.0`}
              />
              <p>APK появится в разделе Releases — его удобно скачивать прямо с телефона.</p>
            </Step>
            <Step n={4} title="(Опционально) Свой API-ключ Nothing">
              <p>
                Settings → Secrets and variables → Actions → New repository secret → <Tag>NOTHING_API_KEY</Tag>. Без него
                собирается версия с ключом <Tag>test</Tag> (работает с debug-командой adb).
              </p>
            </Step>
          </ol>

          <p className="mt-6 text-sm text-white/45">
            Локально: открыть папку <Tag>android/</Tag> в Android Studio (Ladybug+) → Run. Glyph SDK лежит в{" "}
            <Tag>android/app/libs/glyph-sdk.aar</Tag>.
          </p>
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" className="border-t border-white/10 bg-white/[0.02]">
        <div className="mx-auto max-w-4xl px-5 py-16">
          <h2 className="text-3xl font-bold">Если что-то не работает</h2>
          <div className="mt-8 space-y-3">
            {[
              {
                q: "В приложении «❌ Не удалось подключиться к Glyph»",
                a: "Выполните adb-команду из шага 3 ещё раз (debug-режим живёт 48 часов) и перезапустите приложение. Убедитесь, что включён Glyph Interface в настройках телефона.",
              },
              {
                q: "Тест кнопкой работает, а при оплате — нет",
                a: "Проверьте, что выданы все три разрешения (✅), включён переключатель «Анимация при оплате» и в блоке «Последнее срабатывание» появляется запись после платежа. Если записи нет — банк присылает уведомление из другого приложения: добавьте его package name в «Свои приложения». Также убедитесь, что уведомления банка не отключены в настройках системы.",
              },
              {
                q: "«Тест как при оплате» не запускается из фона",
                a: "Нужно разрешение «Поверх других окон» и снятые ограничения батареи. На Nothing OS также откройте «Недавние» и закрепите приложение (замок).",
              },
              {
                q: "Сработало два раза / не на ту покупку",
                a: "Повторные срабатывания в течение 6 секунд игнорируются. Приложение не отличает NFC от онлайн-покупок — это ограничение Android.",
              },
              {
                q: "Анимация слишком яркая",
                a: "В приложении есть ползунок яркости (10–100 %).",
              },
            ].map((f) => (
              <details key={f.q} className="group rounded-2xl border border-white/10 bg-black/40 p-5">
                <summary className="cursor-pointer list-none font-semibold marker:hidden">
                  <span className="mr-2 inline-block text-[#e8232b] transition group-open:rotate-90">›</span>
                  {f.q}
                </summary>
                <p className="mt-3 text-sm leading-relaxed text-white/60">{f.a}</p>
              </details>
            ))}
          </div>
        </div>
      </section>

      <footer className="border-t border-white/10 py-8 text-center text-xs text-white/35">
        Исходники Android-проекта — в папке <span className="font-dot">android/</span> · полная инструкция в{" "}
        <span className="font-dot">README.md</span> · Nothing Glyph Developer Kit
      </footer>
    </div>
  );
}
