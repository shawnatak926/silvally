import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { editionName } from './editions'
import photo from '../assets/campus-photo.png'
import { ApiError } from '../api/client'
import { isSchool, type AdmissionProtection, type CalendarPeriod, type InterviewEvaluation, type MutationResult, type PlayerView, type Stat, type Stats } from '../api/contracts'
import { usePlayerData } from '../api/hooks'
import { ChoiceCountdown, randomChoice, useChoiceTimer } from './choice-timer'
import { actionLabel, admissionComponents, admissionProtection, label, phaseLabel, schoolPeriod, stats } from './labels'
import { fieldLearningDetail, fieldLearningRoutes, fieldLearningStatusLabel, isFieldLearningExperience, isFieldLearningRoute } from './field-learning'

export function CampusPhoto({ className }: { className?: string }) {
  return <img className={className} src={photo} width={1059} height={595} alt="대소고 본관과 운동장" />
}

export function PhotoScene({ children, cover = false }: { children: ReactNode; cover?: boolean }) {
  return (
    <div className={cover ? 'scene home-cover' : 'scene'}>
      <section className="scene-panel" id="current-selection">
        <div className="panel-inner">{children}</div>
      </section>
      <figure className="scene-figure">
        <CampusPhoto className="scene-photo" />
        {cover && <figcaption>대구소프트웨어마이스터고등학교 운동장</figcaption>}
      </figure>
    </div>
  )
}

export function Heading({ eyebrow, title, children, level = 1 }: { eyebrow?: string; title: string; children?: ReactNode; level?: 1 | 2 }) {
  const Title = level === 1 ? 'h1' : 'h2'
  return (
    <div className="panel-heading">
      {eyebrow && <p className="eyebrow">{eyebrow}</p>}
      <Title>{title}</Title>
      {children && <p className="description">{children}</p>}
    </div>
  )
}

export function Frame({
  player, children, timeControls, footer, saveStatus = '저장됨',
}: { player?: PlayerView; children: ReactNode; timeControls?: ReactNode; footer?: ReactNode; saveStatus?: string }) {
  const id = player?.id ?? ''
  const calendar = usePlayerData<CalendarPeriod[]>(id, 'catalog/calendar', Boolean(player && isSchool(player)),
    `/catalog/calendar?playerId=${encodeURIComponent(id)}`)
  const currentPeriod = calendar.data?.find((period) => period.year === player?.year && period.turn === player?.turn)
  return (
    <div className={`frame${player ? ' story-frame' : ''}`}>
      <a href="#main" className="skip-link">본문으로</a>
      <header className="header">
        <Link to="/" className="wordmark">
          <span className="wordmark-emblem"><img src="/dgsw-emblem.png" alt="" width={200} height={200} /></span>
          <strong>대소고나</strong>
        </Link>
        {player && <div className="phase">
          <strong>{phaseLabel(player)}</strong><span>{editionName(player.edition ?? 'SPICY')}</span>
          {player && isSchool(player) && <span title={currentPeriod?.theme}>{player.vacation ?
            `${player.vacation.season === 'SUMMER' ? '여름방학' : '겨울방학'}${player.vacation.campTurn ? ` · 캠프 ${player.vacation.campTurn}/2턴` : ''}` : currentPeriod
            ? `${currentPeriod.semester}학기 · ${currentPeriod.month}월` : schoolPeriod(player).label}</span>}
          {player && <span className="save-status">{saveStatus}</span>}
        </div>}
        {timeControls}
      </header>
      <main id="main">{children}</main>
      {footer}
    </div>
  )
}

export function Journey({ player }: { player: PlayerView }) {
  const current = player.phase === 'ENDED' && player.year === 0 ? 0 :
    player.phase === 'ENDED' || player.phase === 'GRAD_READY' ? 5 :
    player.year > 0 ? player.year + 1 : ['BEFORE_SCHOOL', 'CAREER_PICK'].includes(player.phase) ? 1 : 0
  return (
    <nav className="journey" aria-label="학교생활 진행">
      <ol>{['지원', '비포스쿨', '1학년', '2학년', '3학년', '졸업'].map((name, index) => player.edition === 'MILD' && index < 2 ? null : (
        <li key={name} aria-current={index === current ? 'step' : undefined}>
          {name}{index < current ? ' ✓' : index === current ? ' · 현재' : ''}
        </li>
      ))}</ol>
    </nav>
  )
}

interface Option { value: string; label: string; detail?: ReactNode; disabled?: boolean; hint?: string | null; tag?: string; autoPick?: boolean }

export function ChoiceForm({
  options, onSubmit, submit = '선택 확정', busy, onSelectionChange, children, canSubmit = true, automatic = false,
}: { options: Option[]; onSubmit: (value: string) => void; submit?: string; busy: boolean;
  onSelectionChange?: (value: string) => void; children?: ReactNode; canSubmit?: boolean; automatic?: boolean }) {
  const id = useId()
  const [selection, setSelection] = useState('')
  const selected = options.find((item) => item.value === selection && !item.disabled)
  const allowed = options.filter(item => !item.disabled && item.autoPick !== false && !item.value.includes('WITHDRAWAL'))
  const timer = useChoiceTimer({ enabled: automatic && canSubmit && (Boolean(selected) || allowed.length > 0), busy,
    choiceKey: options.map(item => `${item.value}:${item.disabled}`).join('|'),
    onExpire: () => { const choice = selected ?? randomChoice(allowed); if (choice) onSubmit(choice.value) } })
  return (
    <form onSubmit={(event) => { event.preventDefault(); if (selected && !busy && canSubmit) timer.confirm(() => onSubmit(selected.value)) }}>
      {automatic && <ChoiceCountdown timer={timer} description={!canSubmit ? '필요한 입력을 마치면 자동 선택을 시작합니다.' : selected ? '선택해 둔 항목을 자동으로 확정합니다.' : undefined} />}
      <fieldset className="options" aria-label="선택지" disabled={busy}>
        {options.map((option) => (
          <label className={`option${selected?.value === option.value ? ' selected' : ''}${option.disabled ? ' locked' : ''}`} key={option.value}>
            <input type="radio" name={id} value={option.value} checked={selected?.value === option.value}
              disabled={option.disabled} onChange={() => { setSelection(option.value); onSelectionChange?.(option.value) }} aria-label={option.label} />
            <span className="option-copy"><strong>{option.label}</strong>
              {(option.detail || option.hint) && <small>{option.detail}{option.hint && <span className="lock-note">{option.hint}</span>}</small>}
            </span>
            {option.tag && <span className="option-tag">{option.tag}</span>}
          </label>
        ))}
      </fieldset>
      {children}
      <button className="btn primary wide" type="submit" disabled={busy || !selected || !canSubmit}>{busy ? '처리 중…' : submit}</button>
    </form>
  )
}

export function Dialog({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current
    if (dialog && !dialog.open) dialog.showModal()
    return () => { if (dialog?.open) dialog.close() }
  }, [])
  return (
    <dialog ref={ref} className="dialog" aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); close() }}>
      <header className="dialog-heading"><h2 id={titleId}>{title}</h2><button type="button" className="btn" onClick={close}>닫기</button></header>
      {children}
    </dialog>
  )
}

export function Loading({ text = '학교생활을 불러오는 중…' }: { text?: string }) {
  return <p className="notice" role="status">{text}</p>
}

export function Problem({ error, retry }: { error: unknown; retry?: () => void }) {
  const message = error instanceof ApiError ? error.message : '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  return (
    <div className="problem" role="alert">
      <p>{message}</p>
      {retry && <button className="btn" type="button" onClick={retry}>다시 불러오기</button>}
    </div>
  )
}

export function Receipt({ receipt }: { receipt: MutationResult }) {
  const admission = typeof receipt.details?.rank === 'number' && typeof receipt.details.score === 'number'
  const route = receipt.details?.fieldLearningRoute
  const field = receipt.details?.fieldLearning
  const experience = isFieldLearningExperience(field) ? field :
    isFieldLearningRoute(route) ? { route, status: 'INTEREST_RECORDED' as const } : null
  const changes = Object.entries(receipt.statDelta ?? {}).map(([stat, value]) => `${stats[stat as Stat]} ${value > 0 ? '+' : ''}${value}`)
  const interview = receipt.details?.interviewEvaluation as InterviewEvaluation | undefined
  const protection = receipt.details?.protection as AdmissionProtection | undefined
  return (
    <p className="receipt" role="status">
      {experience ? `${fieldLearningRoutes[experience.route].name} · ${fieldLearningStatusLabel(experience.status)}` :
        admission ? `${receipt.details?.passed ? '전형 통과' : '전형 탈락'} · ${receipt.details?.score}점 · ${receipt.details?.rank}위` :
        `${actionLabel(receipt.actionCode)} · ${receipt.resultCode === 'DONE' || receipt.resultCode === 'CHOICE_SAVED' ? '기록됨' : label(receipt.resultCode)}`}
      {experience && <small>{fieldLearningDetail(experience)}</small>}
      {admission && <small>{admissionComponents(Number(receipt.details?.stage), receipt.details?.components)}</small>}
      {typeof receipt.details?.minimumScore === 'number' && <small>통과 기준 {receipt.details.minimumScore}점</small>}
      {protection && <small>{admissionProtection(protection, receipt.details?.passed === true)}</small>}
      {interview && <small>학교 면접 {interview.score}점 · {interview.criteria.map((item) => `${item.label} ${item.score}`).join(' · ')}</small>}
      {changes.length > 0 && <small>{changes.join(' · ')}</small>}
    </p>
  )
}

export function StatList({ values }: { values: Stats }) {
  return <dl className="stats">{Object.entries(stats).filter(([code]) => typeof values[code as Stat] === 'number').map(([code, name]) => (
    <div key={code}><dt>{name}</dt><dd><meter value={values[code as Stat]} min={0} max={100} aria-label={name} /><span>{values[code as Stat]}</span></dd></div>
  ))}</dl>
}
