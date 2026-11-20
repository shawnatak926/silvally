import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'

export const choiceTimeoutMs = 10_000
type Clock = { enabled: boolean; paused: boolean; key: string; inputAt: { current: number }; toggle: () => void }
const ChoiceClock = createContext<Clock | null>(null)

export function ChoiceTimerScope({ children, paused, resetKey }: { children: ReactNode; paused: boolean; resetKey: string }) {
  const [enabled, setEnabled] = useState(true)
  const [hidden, setHidden] = useState(document.hidden)
  const inputAt = useRef(Date.now())
  useEffect(() => {
    const activity = () => { inputAt.current = Date.now() }
    const visibility = () => { activity(); setHidden(document.hidden) }
    const inputs = ['pointerdown', 'keydown', 'input', 'wheel', 'touchstart'] as const
    inputs.forEach(event => window.addEventListener(event, activity, { passive: true, capture: true }))
    document.addEventListener('visibilitychange', visibility)
    return () => {
      inputs.forEach(event => window.removeEventListener(event, activity, true))
      document.removeEventListener('visibilitychange', visibility)
    }
  }, [])
  return <ChoiceClock.Provider value={{ enabled, paused: paused || hidden, key: resetKey, inputAt,
    toggle: () => { inputAt.current = Date.now(); setEnabled(value => !value) } }}>{children}</ChoiceClock.Provider>
}

export function randomChoice<T>(choices: T[]): T | undefined {
  return choices.length ? choices[Math.floor(Math.random() * choices.length)] : undefined
}

/** Only an existing command callback can run; this clock never changes player data itself. */
export function useChoiceTimer({ enabled, busy, choiceKey, onExpire }: {
  enabled: boolean; busy: boolean; choiceKey: string; onExpire: () => void
}) {
  const clock = useContext(ChoiceClock)
  const callback = useRef(onExpire)
  const submitted = useRef(false)
  const [remaining, setRemaining] = useState(choiceTimeoutMs)
  const suspended = !enabled || busy || !clock?.enabled || clock.paused
  const resetKey = `${clock?.key}:${choiceKey}`
  useEffect(() => { callback.current = onExpire })
  useEffect(() => { submitted.current = false }, [resetKey, busy])
  useEffect(() => {
    setRemaining(choiceTimeoutMs)
    if (suspended || !clock) return
    const started = Date.now()
    const interval = window.setInterval(() => {
      const left = Math.max(0, choiceTimeoutMs - (Date.now() - Math.max(started, clock.inputAt.current)))
      setRemaining(left)
      if (left === 0 && !submitted.current && !document.hidden) {
        submitted.current = true
        window.clearInterval(interval)
        callback.current()
      }
    }, 100)
    return () => window.clearInterval(interval)
  }, [resetKey, suspended, clock?.inputAt])
  const confirm = (action: () => void) => {
    if (busy || submitted.current) return
    submitted.current = true
    action()
  }
  return { remaining, suspended, confirm, clock }
}

export function ChoiceCountdown({ timer, description = '선택하지 않으면 가능한 항목 중 자동으로 고릅니다.' }: {
  timer: ReturnType<typeof useChoiceTimer>; description?: string
}) {
  if (!timer.clock) return null
  const seconds = Math.ceil(timer.remaining / 1000)
  return <div className="choice-countdown" role="group" aria-label="선택 자동 진행">
    <div className="choice-countdown-heading">
      <span>{!timer.clock.enabled ? '자동 선택 꺼짐' : timer.suspended ? '자동 선택 대기' : `입력이 없으면 ${seconds}초 후 자동 선택`}</span>
      <button type="button" className="choice-countdown-toggle" onClick={timer.clock.toggle}>
        {timer.clock.enabled ? '자동 선택 끄기' : '자동 선택 켜기'}
      </button>
    </div>
    <progress aria-label="자동 선택까지 남은 시간" max={choiceTimeoutMs} value={timer.remaining}
      aria-valuetext={`${seconds}초 남음`} />
    <small>{description} 클릭·입력·스크롤하면 10초가 다시 시작됩니다.</small>
  </div>
}

export function TimedChoices({ choices, busy, onChoose, children }: {
  choices: { code: string; enabled: boolean }[]; busy: boolean; onChoose: (code: string) => void;
  children: (choose: (code: string) => void) => ReactNode
}) {
  // A missing input must never invent a withdrawal request.
  const allowed = choices.filter(c => c.enabled && !c.code.includes('WITHDRAWAL'))
  const timer = useChoiceTimer({ enabled: allowed.length > 0, busy,
    choiceKey: choices.map(c => `${c.code}:${c.enabled}`).join('|'),
    onExpire: () => { const choice = randomChoice(allowed); if (choice) onChoose(choice.code) } })
  return <>
    {allowed.length > 0 && <ChoiceCountdown timer={timer} />}
    {children(code => timer.confirm(() => onChoose(code)))}
  </>
}
