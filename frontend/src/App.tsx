import { useEffect, useState, type ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom'
import { ApiError, resultReceipt } from './api/client'
import {
  CAMPUS_FINISH_CHOICE, isSchool, type ApplicationView, type CareerPreferenceView, type EndingView, type EventView,
  type MutationResult, type PlayerView,
} from './api/contracts'
import { useCommands, usePlayerData, useStory } from './api/hooks'
import { useGameUi, useSavedGame } from './state'
import { onPlayer, Recruitment, useRecruitment, type RunPlayer } from './game/activities'
import { editionName, lockedEdition, type GameEdition } from './game/editions'
import { applicationTurn, semesterTurns } from './game/schedule'
import { fieldLearningRoutes, isFieldLearningRoute } from './game/field-learning'
import { admissionComponents, admissionProtection, jobLabel, label, stats } from './game/labels'
import { FieldLearningRecord, History, Notebook, qualificationName } from './game/records'
import { CurrentStory, DashboardLinks, SchoolRail, StoryJournal, StudentRail } from './game/story'
import { InterviewForm } from './game/interview'
import { EventSource, NarshaRecord } from './game/school-life'
import { CampusPhoto, ChoiceForm, Dialog, Frame, Heading, Journey, Loading, PhotoScene, Problem, Receipt, StatList } from './game/ui'

type Run = (method: 'POST' | 'PUT', path: string, body?: Record<string, unknown>, version?: number) => void
const autoTurnDelay = 3600
const nextDecisionDelay = 750
const isNarshaTopic = (code: string) => code === 'N104' || code === 'N201'

export default function App() {
  const commands = useCommands()
  const { pending, active, setDialog } = useGameUi()
  const [error, setError] = useState<unknown>()
  const [receipt, setReceipt] = useState<MutationResult | null>(null)
  const { pathname } = useLocation()
  useEffect(() => {
    setDialog(null)
  }, [pathname, setDialog])

  const run: Run = (method, path, body, version) => {
    setError(undefined)
    void commands.run(method, path, body, version).then((result) => { if (result) setReceipt(resultReceipt(result)) }, setError)
  }
  const retry = () => {
    setError(undefined)
    void commands.retry().then((result) => { if (result) setReceipt(resultReceipt(result)) }, setError)
  }
  const busy = Boolean(pending || active)
  const commandFailed = error != null || Boolean(pending && !active)
  const feedback = (
    <>
      {pending && !active && <div className="problem" role="status">
        <p>응답을 확인하지 못했습니다. 저장된 요청을 다시 보내 진행 상태를 확인할 수 있습니다.</p>
        <button type="button" className="btn" onClick={retry}>같은 요청 다시 보내기</button>
      </div>}
      {error != null && <Problem error={error} />}
    </>
  )

  return (
    <Routes>
      <Route path="/" element={<Home run={run} busy={busy} feedback={feedback} />} />
      <Route path="/play/:id" element={<GameRoute run={run} busy={busy} receipt={receipt} feedback={feedback} commandFailed={commandFailed} />} />
      <Route path="/ending/:id" element={<GameRoute ending run={run} busy={busy} receipt={receipt} feedback={feedback} commandFailed={commandFailed} />} />
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  )
}

function Home({ run, busy, feedback }: { run: Run; busy: boolean; feedback: ReactNode }) {
  const [name, setName] = useState('')
  const [edition, setEdition] = useState<GameEdition>(lockedEdition ?? 'MILD')
  const savedId = useSavedGame((state) => state.playerIds?.[edition] ?? (edition === 'SPICY' && !Object.keys(state.playerIds ?? {}).length ? state.playerId : null))
  const saved = usePlayerData<PlayerView>(savedId ?? '', '')
  const savedMissing = saved.error instanceof ApiError && saved.error.status === 404
  return (
    <Frame>
      {feedback}
      <PhotoScene cover>
        <Heading eyebrow="대소고 학교생활 시뮬레이션" title="대소고나">{edition === 'MILD' ? '1학년 1학기부터 시작하는 학교생활과 취업 준비.' : '입학 지원서를 내는 날부터 졸업하는 날까지.'}</Heading>
        {lockedEdition ? <p className="edition-note">{editionName(edition)} 버전</p> : <fieldset className="edition-picker"><legend>플레이 버전</legend>
          {(['MILD', 'SPICY'] as const).map((value) => <label key={value}>
            <input type="radio" name="edition" aria-label={editionName(value)} checked={edition === value} onChange={() => setEdition(value)} />
            <strong>{editionName(value)}</strong><small>{value === 'MILD' ? '1학년부터 · 휴식과 상담 · 취업 준비 지원' : '입학부터 · 기존 스트레스와 진로 선택'}</small>
          </label>)}
        </fieldset>}
        <form className="create-form" onSubmit={(event) => { event.preventDefault(); if (name.trim() && !busy) run('POST', '/players', { name: name.trim(), edition }) }}>
          <label htmlFor="name">이름</label>
          <input id="name" maxLength={24} required value={name} placeholder="이름을 입력하세요" onChange={(event) => setName(event.target.value)} />
          <button className="btn primary wide" type="submit" disabled={busy || !name.trim()}>{edition === 'MILD' ? '학교생활 시작' : '지원 시작'}</button>
        </form>
        {savedId && !savedMissing && <Link className="btn wide continue" to={`/play/${savedId}`}>이어서 하기</Link>}
        {savedMissing && <small className="muted">저장된 회차를 찾지 못했습니다. 새 회차를 시작할 수 있습니다.</small>}
      </PhotoScene>
      <div className="home-foot"><span>대소고 학교생활을 바탕으로 만든 비공식 게임</span>
        {import.meta.env.MODE === 'demo' && <span>로컬 데모</span>}</div>
    </Frame>
  )
}

function GameRoute({ run, busy, receipt, feedback, commandFailed, ending = false }: {
  run: Run; busy: boolean; receipt: MutationResult | null; feedback: ReactNode; commandFailed: boolean; ending?: boolean
}) {
  const id = useParams().id ?? ''
  const player = usePlayerData<PlayerView>(id, '')
  if (player.isPending || player.error) return (
    <Frame>{feedback}<PhotoScene>
      {player.error ? <><Heading eyebrow="학교생활" title="회차를 열 수 없습니다" />
        <Problem error={player.error} retry={() => { void player.refetch() }} /><Link className="btn wide continue" to="/">시작 화면</Link></> : <Loading />}
    </PhotoScene></Frame>
  )
  if (lockedEdition && (player.data.edition ?? 'SPICY') !== lockedEdition) return <Frame><PhotoScene>
    <Heading title="다른 버전의 회차입니다">이 버전에서 새 학교생활을 시작하거나 저장된 회차를 이어 주세요.</Heading><Link className="btn" to="/">시작 화면</Link>
  </PhotoScene></Frame>
  if (ending !== (player.data.phase === 'ENDED')) return <Navigate replace to={`/${player.data.phase === 'ENDED' ? 'ending' : 'play'}/${id}`} />
  return <Game key={id} player={player.data} busy={busy} receipt={receipt?.player.id === id ? receipt : null}
    feedback={feedback} commandFailed={commandFailed} run={onPlayer(player.data, run)} />
}

function Game({ player, busy, receipt, feedback, commandFailed, run }: {
  player: PlayerView; busy: boolean; receipt: MutationResult | null; feedback: ReactNode; commandFailed: boolean; run: RunPlayer
}) {
  const school = isSchool(player)
  const ordinary = !['CAREER_PICK', 'GRAD_READY', 'ENDED'].includes(player.phase)
  const hiring = school && !player.vacation && player.year === 3 && player.turn >= applicationTurn
  const { dialog, setDialog } = useGameUi()
  const events = usePlayerData<EventView[]>(player.id, '/events', ordinary)
  const story = useStory(player.id)
  const recruitment = useRecruitment(player, hiring)
  const [selectedEvent, setSelectedEvent] = useState<string>()
  const playbackKey = `daesogo.playback.${player.id}`
  const [playback, setPlayback] = useState<'auto' | 'next' | null>(
    () => sessionStorage.getItem(playbackKey) === 'paused' ? null : 'auto',
  )
  const [speed, setSpeed] = useState(1)
  const entries = story.data?.entries ?? []
  const visible = ordinary ? events.data ?? [] : []
  const requiredEvents = visible.filter((event) => event.required)
  const eventOptions = requiredEvents.length ? requiredEvents : visible
  const current = eventOptions.find((event) => event.code === selectedEvent) ?? eventOptions[0]
  const decision = school && current?.required && current.code !== 'TERM_FOCUS' ? current : undefined
  const hasRequired = requiredEvents.length > 0 || (school && ((player.year === 2 && !player.major) || (player.year === 3 && !player.track)))
  const canAct = school && Boolean(events.data) && !events.error && !hasRequired
  const choiceOpen = visible.some((event) => event.choices.some((choice) => choice.enabled)) || recruitment.hasAction
  const blocksAuto = visible.some((event) => (event.blocksAuto ?? (event.required || event.code === 'J01'))
    && event.choices.some((choice) => choice.enabled)) || recruitment.hasAction
  const checking = events.isFetching || recruitment.pending || recruitment.fetching
  const canAuto = canAct && !blocksAuto && !recruitment.error && !checking
  const awaitingChoice = hasRequired || blocksAuto
  const running = Boolean(playback && !awaitingChoice && !dialog && !commandFailed)

  useEffect(() => {
    if (!playback || !school) return
    if (commandFailed || events.error || recruitment.error || (playback === 'next' && awaitingChoice)) {
      setPlayback(null)
      return
    }
    if (busy || dialog || !canAuto) return
    const delay = playback === 'next' ? nextDecisionDelay : autoTurnDelay / speed
    const timer = window.setTimeout(() => run('POST', '/turns/advance', { mode: 'AUTO' }), delay)
    return () => window.clearTimeout(timer)
  }, [playback, speed, school, commandFailed, events.error, recruitment.error, busy, checking, awaitingChoice, dialog, canAuto, run])

  useEffect(() => {
    if (school) sessionStorage.setItem(playbackKey, playback === 'auto' ? 'auto' : 'paused')
  }, [playbackKey, playback, school])

  const showStory = () => document.getElementById('current-story')?.scrollIntoView({ block: 'start' })
  const nextTurn = () => {
    setDialog(null)
    showStory()
    run('POST', '/turns/advance')
  }
  const advance = () => {
    setPlayback(null)
    if (choiceOpen) setDialog('turn')
    else nextTurn()
  }

  let panel: ReactNode
  if (player.phase === 'CAREER_PICK') panel = <CareerPicker player={player} busy={busy} run={run} />
  else if (player.phase === 'GRAD_READY') panel = <GraduationPicker player={player} busy={busy} run={run} />
  else if (player.phase === 'ENDED') panel = <Ending player={player} />
  else if (events.isPending) panel = <Loading />
  else if (events.error) panel = <Problem error={events.error} retry={() => { void events.refetch() }} />
  else if (current && current.code !== 'TERM_FOCUS') panel = (
    <>
      {eventOptions.length > 1 && <select className="event-selector" aria-label="현재 이벤트" value={current.code} disabled={busy}
        onChange={(event) => setSelectedEvent(event.target.value)}>
        {eventOptions.map((event) => <option key={event.code} value={event.code}>{event.title}</option>)}
      </select>}
      {decision ? (
        <section aria-label="중대 결정">
          <Heading eyebrow="중대 결정" title={decision.title} level={2}>{decision.situation}</Heading>
          <EventSource source={decision.source} />
          {isNarshaTopic(decision.code) ? <NarshaTopicPicker key={decision.code} event={decision} busy={busy} run={run} /> :
            <ol className="decision-options" data-event-code={decision.code}>{decision.choices.map((choice, index) => <li key={choice.code}>
              <button className="btn wide decision-option" type="button" disabled={busy || !choice.enabled}
                aria-label={choice.label} aria-describedby={choice.description ? `decision-${choice.code}` : undefined}
                onClick={() => run('POST', `/events/${decision.code}/choices`, { choiceCode: choice.code })}>
                <span aria-hidden="true">{index + 1}</span><span className="decision-copy"><strong>{choice.label}</strong>
                {choice.description && <small id={`decision-${choice.code}`}>{choice.description}</small>}</span>
              </button>
            </li>)}</ol>}
        </section>
      ) : <EventPanel key={current.code} event={current} player={player} busy={busy} run={run} />}
    </>
  )

  const timeControls = school && (
    <div className="time-controls" role="group" aria-label="시간 진행">
      <span className={`clock-label${running ? ' running' : ''}`} aria-live="polite">
        {running ? '시간이 흐르는 중' : '일시 정지 상태'}
        {awaitingChoice && <small> · 선택 대기</small>}
      </span>
      <button className="btn primary" type="button" disabled={!running && (!canAuto || busy || commandFailed)}
        onClick={() => { if (!running) showStory(); setPlayback(running ? null : 'auto') }}>{running ? '일시 정지' : '자동 진행'}</button>
      <select className="clock-speed" aria-label="진행 속도" value={speed} onChange={(event) => setSpeed(Number(event.target.value))}>
        {[1, 2, 4, 8, 16].map((rate) => <option key={rate} value={rate}>{rate}×</option>)}
      </select>
      <button className="btn" type="button" disabled={playback === 'next' || !canAuto || busy || commandFailed}
        onClick={() => { showStory(); setPlayback('next') }}>다음 결정까지</button>
      <button className="btn end-turn" type="button" disabled={!canAct || busy} onClick={advance}>턴 종료</button>
    </div>
  )

  return (
    <Frame player={player} timeControls={timeControls} footer={<Journey player={player} />}
      saveStatus={busy ? '저장 중' : commandFailed ? '저장 상태 확인' : '저장됨'}>
      <div className="story-layout">
        <section className="story-main" aria-labelledby="journal-title">
          <DashboardLinks />
          <div className="journal-heading"><h1 id="journal-title">인생 기록</h1><span>{entries.length}건</span></div>
          <CurrentStory player={player} entry={entries.at(-1)} loading={story.isPending} failed={story.isError} retry={() => { void story.refetch() }}>
            {receipt && receipt.actionCode !== 'TURN_ADVANCE' && <Receipt receipt={receipt} />}
          </CurrentStory>
          <section className="current-choices" id="current-choices" aria-labelledby="choices-title">
            <h2 className="section-label" id="choices-title">선택지</h2>
            {!dialog && feedback}
            {panel && <section className="story-card choice-card" id="current-selection" aria-label="지금 열린 선택">{panel}</section>}
            {hiring && canAct && <Recruitment player={player} busy={busy} run={run} data={recruitment} />}
            {school && !player.vacation && (!hasRequired || current?.code === 'TERM_FOCUS') &&
              <SemesterFocus key={`${player.year}-${player.turn <= semesterTurns ? 1 : 2}`}
                player={player} busy={busy} run={run} event={current?.code === 'TERM_FOCUS' ? current : undefined} />}
            {Boolean(player.vacation?.campTurn) && <section className="story-card choice-card">
              <Heading title={`전공강화캠프 · ${player.vacation?.campTurn}/2턴`} level={2}>
                시간을 진행하면 이번 실습을 마칩니다.
              </Heading>
            </section>}
            {school && <p className="clock-hint">중대 결정과 채용 일정에서는 시간을 멈춥니다.</p>}
            {!panel && !hiring && !school && <p className="story-note">현재 열린 선택지가 없습니다.</p>}
          </section>
          <StoryJournal entries={entries.slice(0, -1)} />
        </section>
        <StudentRail player={player} />
        <SchoolRail player={player} />
      </div>
      {dialog === 'notebook' && <Dialog title="학생 수첩" close={() => setDialog(null)}>{feedback}<Notebook player={player} busy={busy} run={run} /></Dialog>}
      {dialog === 'history' && <Dialog title="전체 기록" close={() => setDialog(null)}>{feedback}<History player={player} /></Dialog>}
      {dialog === 'photo' && <Dialog title="대소고 캠퍼스 사진" close={() => setDialog(null)}><CampusPhoto className="campus-photo" /></Dialog>}
      {dialog === 'turn' && <Dialog title="턴을 끝낼까요?" close={() => setDialog(null)}>
        {feedback}
        {choiceOpen && <p>현재 열린 선택이나 채용 일정은 이번 턴과 함께 지나갑니다.</p>}
        <button type="button" className="btn primary wide" disabled={busy || !canAct} onClick={nextTurn}>다음 턴으로</button>
      </Dialog>}
    </Frame>
  )
}

function SemesterFocus({ player, busy, run, event }: {
  player: PlayerView; busy: boolean; run: RunPlayer; event?: EventView
}) {
  const query = usePlayerData<EventView>(player.id, '/events/TERM_FOCUS', !event && !player.semesterFocus)
  const focus = player.semesterFocus
  const semester = player.turn <= semesterTurns ? 1 : 2
  const turnInSemester = (player.turn - 1) % semesterTurns + 1
  const choices = (event ?? query.data)?.choices ?? []
  return (
    <section className="story-card choice-card" aria-label="이번 학기에 집중할 것">
      <Heading eyebrow={`${semester}학기 · ${turnInSemester}/${semesterTurns}턴`} title="이번 학기에 집중할 것" level={2} />
      {focus ? <p><strong>{focus.label}</strong><br /><small>이 학기가 끝날 때까지 매 턴 적용됩니다.</small></p> :
        choices.length ? <ChoiceForm busy={busy} submit="이번 학기 집중 정하기"
          options={choices.map((choice) => ({ value: choice.code, label: choice.label, detail: choice.description, disabled: !choice.enabled }))}
          onSubmit={(choiceCode) => run('POST', '/events/TERM_FOCUS/choices', { choiceCode })} /> :
          query.isError ? <Problem error={query.error} retry={() => { void query.refetch() }} /> : <Loading />}
    </section>
  )
}

function NarshaTopicPicker({ event, busy, run }: { event: EventView; busy: boolean; run: RunPlayer }) {
  const [choiceCode, setChoiceCode] = useState('')
  const [topic, setTopic] = useState('')
  const custom = choiceCode === 'NARSHA_CUSTOM'
  const valid = !custom || (topic.trim().length >= 2 && topic.trim().length <= 80 && !/[\x00-\x1f\x7f]/.test(topic.trim()))
  return <ChoiceForm busy={busy} submit="이 주제로 시작" canSubmit={valid} onSelectionChange={setChoiceCode}
    options={event.choices.map((choice) => ({
      value: choice.code, label: choice.label, detail: choice.description, disabled: !choice.enabled,
    }))} onSubmit={(code) => run('POST', `/events/${event.code}/choices`, {
      choiceCode: code, ...(code === 'NARSHA_CUSTOM' ? { customTopic: topic.trim() } : {}),
    })}>
    {custom && <div className="custom-topic-field">
      <label htmlFor={`custom-topic-${event.code}`}>프로젝트 주제</label>
      <input id={`custom-topic-${event.code}`} value={topic} maxLength={80} disabled={busy}
        placeholder="팀이 만들 프로젝트 주제를 입력해 주세요" onChange={(e) => setTopic(e.target.value)}
        aria-describedby={`custom-topic-hint-${event.code}`} />
      <small id={`custom-topic-hint-${event.code}`}>2~80자 · {topic.trim().length}/80</small>
    </div>}
  </ChoiceForm>
}

function BeforeSchoolPicker({ event, busy, onSubmit }: {
  event: EventView
  busy: boolean
  onSubmit: (choiceCodes: string[]) => void
}) {
  const [selected, setSelected] = useState<string[]>([])
  const choices = event.choices.filter((choice) => choice.code !== CAMPUS_FINISH_CHOICE)
  const limit = event.selectionCount ?? 2
  const visited = choices.filter((choice) => choice.visited).length
  const remaining = Math.max(0, limit - visited)
  const submitLabel = event.code === 'BS01' ? '탐색 마치기' : '체험 마치기'

  return <>
    <p className="field-note" role="status">선택 {Math.min(visited + selected.length, limit)} / {limit}</p>
    <fieldset className="options before-school-options" aria-label={event.title}>
      {choices.map((choice) => {
        const checked = Boolean(choice.visited) || selected.includes(choice.code)
        const disabled = busy || !choice.enabled || (!checked && selected.length >= remaining)
        return <label key={choice.code} className={`option${checked ? ' selected' : ''}${disabled ? ' locked' : ''}`}>
          <input type="checkbox" aria-label={choice.label} checked={checked} disabled={disabled}
            onChange={(e) => {
              const check = e.target.checked
              setSelected((current) => check
                ? [...current, choice.code]
                : current.filter((code) => code !== choice.code))
            }} />
          <span className="option-copy"><strong>{choice.label}</strong><small>{choice.description}</small></span>
          {choice.visited && <span className="option-tag">방문 완료</span>}
        </label>
      })}
    </fieldset>
    <button className="btn primary wide" type="button" disabled={busy || selected.length !== remaining}
      onClick={() => onSubmit(selected)}>{submitLabel}</button>
  </>
}

function EventPanel({ event, player, busy, run }: { event: EventView; player: PlayerView; busy: boolean; run: RunPlayer }) {
  const path = `/events/${encodeURIComponent(event.code)}/choices`
  const commit = (choiceCode: string) => run('POST', path, { choiceCode })
  const fieldLearning = event.code === 'J01' && event.choices.some((choice) => isFieldLearningRoute(choice.code))
  const caption = fieldLearning ? '게임 속 진로 체험 · 선택 이벤트' : event.code === 'J01' ? '진로 상담 · 선택 이벤트' :
    player.phase === 'BEFORE_SCHOOL' ? `게임 속 비포스쿨 · ${player.beforeSchoolDay}일 차` :
      event.required ? undefined : '선택 이벤트'
  return (
    <>
      <Heading eyebrow={caption} title={event.title} level={2}>{event.situation}</Heading>
      {event.code === 'AD00' && event.choices.length > 1 && <p className="field-note">표시된 확률은 이 회차의 지원 자격 추첨에 적용됩니다. 특별전형 탈락 시 보호막이 발동해 이미 제출한 결과로 일반전형 판정을 한 번 받습니다.</p>}
      <EventSource source={event.source} />
      {fieldLearning && <p className="field-note">가상 운영 규칙입니다. 한 회차에서 한 프로그램에 신청할 수 있으며, 참가 여부와 관계없이 기업 지원도 가능합니다.</p>}
      {event.code === 'AD05' ? (
        <InterviewForm playerId={player.id} path="/events/AD05/interview" busy={busy}
          submit={(questionId, answer) => run('POST', '/events/AD05/choices', { choiceCode: 'ANSWER', questionId, answer })} />
      ) : isNarshaTopic(event.code) ? (
        <NarshaTopicPicker key={event.code} event={event} busy={busy} run={run} />
      ) : event.selectionCount ? (
        <BeforeSchoolPicker key={event.code} event={event} busy={busy}
          onSubmit={(choiceCodes) => run('POST', path, { choiceCodes })} />
      ) : event.required ? (
        <ChoiceForm busy={busy} onSubmit={commit} options={event.choices.map((choice) => ({
          value: choice.code, label: choice.label, detail: choice.description, disabled: !choice.enabled,
          hint: choice.visited ? undefined : choice.reason, tag: choice.visited ? '방문 완료' : choice.apCost > 0 ? `${choice.apCost} AP` : undefined,
        }))} />
      ) : (
        <>
          <div className="event-buttons">
            {event.choices.map((choice, index) => <div key={choice.code}>
              <button type="button" className={`btn wide${index === 0 ? ' primary' : ''}`} disabled={busy || !choice.enabled}
                onClick={() => commit(choice.code)}>{choice.label}{choice.apCost > 0 ? ` · ${choice.apCost} AP` : ''}</button>
              {fieldLearning && isFieldLearningRoute(choice.code) && <small className="field-option">{fieldLearningRoutes[choice.code].preview}</small>}
              {choice.description && !fieldLearning && <small className="field-option">{choice.description}</small>}
              {choice.reason && <small className="lock-note">{choice.reason}</small>}
            </div>)}
          </div>
        </>
      )}
    </>
  )
}

function CareerPicker({ player, busy, run }: { player: PlayerView; busy: boolean; run: RunPlayer }) {
  const career = usePlayerData<CareerPreferenceView>(player.id, '/career-preference')
  if (career.isPending) return <Loading />
  if (career.error) return <Problem error={career.error} retry={() => { void career.refetch() }} />
  return (
    <>
      <Heading eyebrow="나의 진로" title="희망 직무 선택" level={2}>{player.edition === 'MILD' ? '현재 기초 역량을 바탕으로 11개 직무를 적합도순으로 정리했습니다.' : '비포스쿨 경험과 능력치를 바탕으로 11개 직무를 적합도순으로 정리했습니다.'} 순위는 참고하고, 하고 싶은 직무를 자유롭게 골라 주세요.</Heading>
      <ChoiceForm busy={busy} submit="이 직무로 시작" onSubmit={(roleCode) => run('PUT', '/career-preference', { roleCode })}
        options={[...career.data.recommendations].sort((a, b) => a.rank - b.rank).map((entry) => ({
          value: entry.roleCode, label: entry.label, tag: `적합도 ${entry.rank}위 · ${entry.score.toFixed(2)}점`,
          detail: [...entry.activityReasons.map((item) => item.eventCode === 'BS01' && item.choiceCode === 'STUDIO'
            ? label('STUDIO') : item.activityLabel), ...entry.statReasons.map((item) => `${stats[item.statCode]} +${item.contribution}`)].join(' · '),
        }))} />
    </>
  )
}

function GraduationPicker({ player, busy, run }: { player: PlayerView; busy: boolean; run: RunPlayer }) {
  const applications = usePlayerData<ApplicationView[]>(player.id, '/applications')
  if (applications.isPending) return <Loading />
  if (applications.error) return <Problem error={applications.error} retry={() => { void applications.refetch() }} />
  const offers = applications.data.filter((item) => item.status === 'OFFERED')
  return (
    <>
      <Heading eyebrow="학교생활의 마지막 선택" title="졸업 후 진로" level={2}>학교에서 쌓은 경험을 바탕으로 다음 걸음을 정합니다.</Heading>
      <ChoiceForm busy={busy} submit="졸업 확정" onSubmit={(value) => {
        if (value.startsWith('offer:')) run('POST', '/graduation', { path: 'ACCEPT_OFFER', applicationId: value.slice(6) })
        else run('POST', '/graduation', { path: value })
      }} options={[
        ...offers.map((item) => ({ value: `offer:${item.id}`, label: `${label(item.companyCode)} 제안 수락`, detail: jobLabel(item.role) })),
        { value: 'RETRY_JOB', label: '다시 취업 준비' },
        { value: 'STUDY_NEXT', label: '진학 준비', detail: '진학 준비 2회와 학업 35점' },
        { value: 'INDIE', label: '개인 개발 준비', detail: '개인 개발 준비 1회와 완성 프로젝트' },
      ]} />
    </>
  )
}

function Ending({ player }: { player: PlayerView }) {
  const result = usePlayerData<EndingView>(player.id, '/ending')
  if (result.isPending) return <Loading />
  if (result.error) return <Problem error={result.error} retry={() => { void result.refetch() }} />
  const { title, body, summary } = result.data
  return (
    <>
      <Heading eyebrow={player.endReason === 'WITHDRAWAL' ? '학교생활 기록' : player.year ? '졸업 기록' : '입학 전형 결과'} title={title} level={2}>{body}</Heading>
      {'stage' in summary ? <div className="record-line"><p>{summary.stage}차 · {summary.score}점 · {summary.rank}위</p>
        {summary.protection && <p>{admissionProtection(summary.protection, summary.passed === true)}</p>}
        {summary.components && <p className="muted">{admissionComponents(summary.stage, summary.components)}</p>}</div> :
        <div className="ending-summary">
          {summary.schoolStatus === 'WITHDRAWN' && <p>{summary.withdrawalYear}학년 {summary.withdrawalTurn}턴에 자퇴를 선택했습니다.</p>}
          <p>최초 희망 · {label(summary.initialCareerCode)}<br />현재 희망 · {label(summary.currentCareerCode)}</p>
          <p>{label(summary.major)} · {label(summary.track)}</p>
          {summary.employed && summary.acceptedRole ? <p>취업 · {label(summary.acceptedCompany)} · {jobLabel(summary.acceptedRole)}</p> :
            <p>{summary.schoolStatus === 'WITHDRAWN' ? '다음 계획' : '졸업 후'} · {label(summary.nextPath)}</p>}
          <FieldLearningRecord route={summary.fieldLearningRoute} experience={summary.fieldLearning} />
          {summary.narshaProjects?.length ? summary.narshaProjects.map((record, i) =>
            <NarshaRecord key={record.schoolYear ?? i} record={record} />) : <NarshaRecord record={summary.narsha} />}
          {summary.qualifications?.length ? <p>자격증 · {summary.qualifications.map(qualificationName).join(', ')}</p> : null}
          {summary.projects?.length ? <p>프로젝트 · {summary.projects.map((item) => label(item.projectCode)).join(', ')}</p> : null}
          {summary.applicationResults?.map((item) => <p key={item.id}>{label(item.companyCode)} · {label(item.status)}{item.failureCode ? ` · ${label(item.failureCode)}` : ''}</p>)}
          {summary.finalStats && <details><summary>졸업 능력치</summary><StatList values={summary.finalStats} /></details>}
        </div>}
      <Link className="btn primary wide continue" to="/">새 회차 시작</Link>
    </>
  )
}
