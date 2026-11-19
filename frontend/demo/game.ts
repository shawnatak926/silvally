import { createHash, randomInt, randomUUID } from 'node:crypto'
import { applicationTurn, codingTurn, interviewTurn, resultTurn, semesterTurns, yearTurns } from '../src/game/schedule.ts'
import { isFieldLearningRoute } from '../src/game/field-learning.ts'
import {
  CAMPUS_FINISH_CHOICE, isSchool, type AdmissionTrack, type ApplicationView, type AvailableAction, type CareerPreferenceView,
  type EndingView, type Enrollment, type EventView, type FieldLearningExperience, type FieldLearningRoute,
  type HistoryEntry, type InterviewEvaluation, type InterviewQuestion, type ItemStack, type NarshaView,
  type PlayerView, type Project, type Recommendation, type SemesterFocusView, type Snapshot, type Stat, type Stats, type StatSetback, type StressLoad, type VacationView,
} from '../src/api/contracts.ts'
import {
  admission, admissionTracks, beforeSchool, careerEvent, clubEvent, companies, dormEvent,
  type DemoChoice, focusEvent, majorEvent, narshaExpoEvent, narshaTopicEvent, narshaTopics, pairDebugEvent, projects, roles, softwaveEvent, teamGoalEvent, trackEvent, vacationEvent, yearTwoNarshaEvent, stressSupportEvent, schoolDoubtsEvent, withdrawalEvent, stressRestChoices, stressHelpChoices, stressExtraChoices,
  dailySchoolEvents, dailyExamEvent, dailyNarshaEvent,
} from './data.ts'
import { advanceFieldLearning, fieldLearningEnding, fieldLearningSummary, selectFieldLearning } from './field-learning.ts'
import { companyQuestion, gradeInterview, schoolQuestion } from './interview.ts'
import { eventRandom } from './random.ts'
import { schoolActivityEvents } from './school-life.ts'
import { evaluateAssessments, prepareAssessment } from './assessments.ts'

interface DemoProject extends Project { year: number; deadline: number; readings: number[] }
interface DemoEnrollment extends Enrollment { year: number; attendedTurns: number[] }
interface DemoApplication extends ApplicationView { portfolioScore: number }
interface RoutineActivity { action: string; result: string; subject?: string }
interface DemoHistory extends HistoryEntry {
  beforeSchoolActivities?: { title: string; description: string }[]
  stressEvent?: { choiceLabel: string; description?: string; resultCode: string; stress: number }
  dailyEvent?: { title: string; choiceLabel: string; situation: string }
  routine?: { friendContact: boolean; activities: RoutineActivity[]; statDelta: Partial<Stats> }
  fieldLearning?: FieldLearningExperience
  narsha?: NarshaView
  semesterFocus?: SemesterFocusView
  vacation?: VacationView
  vacationOpening?: VacationView
}

export interface DemoGame {
  view: PlayerView
  admissionIndex: number
  admissionChoices: string[]
  assessmentPrep?: number
  kit: number
  used: string[]
  seen: string[]
  prep: { coding: boolean; interview: boolean; study: number; indie: number }
  projects: DemoProject[]
  enrollments: DemoEnrollment[]
  applications: DemoApplication[]
  careerChanged: boolean
  practiced: string[]
  recommendations: Recommendation[]
  evidence: Recommendation['activityReasons']
  history: DemoHistory[]
  snapshots: Snapshot[]
  autoLife?: boolean
  fieldLearning?: FieldLearningExperience
  narsha?: NarshaView
  narshaProjects?: NarshaView[]
  statGrowthCarry?: Partial<Stats>
  eventDraws?: Record<string, { eventCode: string; situation: string; choiceCodes: string[] }>
  dailyChoicePeriod?: string
  stressHandled?: { period: string; level: number }
  withdrawalPending?: boolean
  ending?: EndingView
}

export class DemoError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message) }
}

const block = (message: string, code = 'LOCKED'): never => { throw new DemoError(409, code, message) }
const cap = (value: number) => Math.max(0, Math.min(100, value))
const skills: Stat[] = ['P', 'F', 'B', 'I', 'A', 'C']
const activityCount = 2
const initialStats: Record<Stat, readonly [min: number, max: number, fixture: number]> = {
  P: [19, 25, 20], F: [5, 11, 8], B: [5, 11, 8], I: [5, 11, 8], EN: [18, 26, 22],
  A: [22, 28, 24], G: [60, 80, 71], C: [19, 25, 20], H: [64, 80, 72], S: [12, 24, 18],
}

export function createGame(body: Record<string, unknown>, fixedStats: boolean): DemoGame {
  if (body.demoScenario === 'COUNSELING') return createCounselingGame()
  const name = typeof body.name === 'string' ? body.name.trim() : ''
  if (!name || name.length > 24) {
    throw new DemoError(400, 'VALIDATION_ERROR', '이름은 1~24자로 입력해 주세요.')
  }
  const stats = Object.fromEntries(Object.entries(initialStats).map(([code, [min, max, fixture]]) =>
    [code, fixedStats ? fixture : randomInt(min, max + 1)])) as Stats
  const admissionEligibility = (Object.keys(admissionTracks) as AdmissionTrack[])
    .filter((code) => randomInt(100) < admissionTracks[code].eligibilityChance)
  return {
    view: { id: randomUUID(), name, rulesVersion: 'v1.2', phase: 'ADMISSION_1', year: 0, turn: 0,
      remainingAp: 0, version: 0, stats,
      admissionEligibility, resumeScore: 0 },
    admissionIndex: 0, admissionChoices: [], kit: 0, used: [], seen: [],
    prep: { coding: false, interview: false, study: 0, indie: 0 },
    projects: [], enrollments: [], applications: [], careerChanged: false,
    practiced: [], recommendations: [], evidence: [], history: [], snapshots: [],
  }
}

function createCounselingGame() {
  const game = createGame({ name: '체험 학생' }, true)
  const choices = ['GENERAL', 'BUILD', 'REWRITE', 'ADD_EVIDENCE', 'PYTHON', 'MOCK']
  for (const [index, event] of admission.entries()) chooseEvent(game, event.code, choices[index])
  chooseActivities(game, 'BS01', ['STUDIO', 'FOOTBALL_FIELD'])
  chooseActivities(game, 'BS02', ['WEB_STUDIO', 'DATA_STUDIO'])
  for (const [index, event] of beforeSchool.slice(2).entries()) {
    chooseEvent(game, event.code, ['IOS_LAYOUT', 'SERVICE_FIX', 'UI_CASE'][index])
  }
  pickCareer(game, 'WEB')
  while (game.view.year < 3 || game.view.turn < applicationTurn) {
    const decision = requiredSchoolEvent(game)
    if (!decision) advance(game)
    else {
      const code = ({ TERM_FOCUS: 'FOCUS_BALANCE', VACATION: 'VACATION_REST', Y201: 'SW', Y301: 'FE' } as Record<string, string>)[decision.code]
      chooseEvent(game, decision.code, code ?? decision.choices[0].code)
    }
  }
  for (let attempt = 0; attempt < 3; attempt++) {
    const stress = stressEvent(game)
    if (!stress) break
    chooseEvent(game, stress.code, stress.choices[0].code)
  }
  game.view.stats.EN = 53
  game.view.demoScenario = 'COUNSELING'
  return game
}

const stressEventCodes = new Set(['STRESS_SUPPORT', 'SCHOOL_DOUBTS', 'WITHDRAWAL_DECISION'])

function stressPeriod(g: DemoGame) {
  return `${g.view.year}-${g.view.turn}-${g.view.vacation?.campTurn ?? 0}`
}

function stressEvent(g: DemoGame) {
  if (!isSchool(g.view)) return undefined
  if (g.withdrawalPending) return withdrawalEvent
  const level = g.view.stats.S >= 90 ? 90 : g.view.stats.S >= 60 ? 60 : 0
  if (!level || (g.stressHandled?.period === stressPeriod(g) && g.stressHandled.level >= level)) return undefined
  const random = eventRandom(g.view.id, `stress:${stressPeriod(g)}:${level}`)
  const choices = level === 90
    ? [random.shuffled(stressHelpChoices)[0], random.shuffled(stressRestChoices)[0], schoolDoubtsEvent.choices[2]]
    : [random.shuffled(stressRestChoices)[0], random.shuffled(stressHelpChoices)[0], random.shuffled(stressExtraChoices)[0]]
  const situations = [
    '시험 범위와 남은 과제를 번갈아 보다가 손이 멈췄습니다. 지금 가장 필요한 것을 고릅니다.',
    '친구가 오늘 유난히 말이 없다고 말을 건넸습니다. 쌓인 부담을 어떻게 풀까요?',
    '실습을 마치고도 머릿속에서 할 일이 떠나지 않습니다. 잠시 속도를 조절할 시간이 생겼습니다.',
    '팀 일정과 개인 공부가 겹쳤습니다. 오늘은 어느 쪽으로 부담을 덜어 볼까요?',
  ]
  let situation = situations[random.nextInt(situations.length)]
  if (level === 90) situation = '스트레스가 90 이상입니다. ' + situation + ' 자퇴를 고민해 볼 수도 있습니다.'
  const drawn = { ...(level === 90 ? schoolDoubtsEvent : stressSupportEvent), situation, choices }
  rememberEvent(g, `stress:${stressPeriod(g)}:${level}`, drawn)
  return drawn
}

function recordStressChoice(g: DemoGame, code: string, choice: DemoChoice) {
  if (!stressEventCodes.has(code)) return undefined
  let resultCode = (choice.delta?.S ?? 0) > 0 ? 'STRESS_PUSHED_ON' : 'STRESS_RELIEVED'
  if (choice.code === 'CONSIDER_WITHDRAWAL') {
    g.withdrawalPending = true
    resultCode = 'WITHDRAWAL_CONSIDERED'
  } else {
    delete g.withdrawalPending
    g.stressHandled = { period: stressPeriod(g), level: code === 'STRESS_SUPPORT' ? 60 : 90 }
    if (choice.code === 'CONFIRM_WITHDRAWAL') {
      const { year, turn, initialCareerCode, currentCareerCode, major, track, stats } = g.view
      Object.assign(g.view, { phase: 'ENDED', endReason: 'WITHDRAWAL', remainingAp: 0 })
      delete g.view.vacation
      g.ending = {
        endingCode: 'SCHOOL_WITHDRAWAL', selectedPath: 'LEAVE_SCHOOL', title: '잠시 다른 길을 선택했습니다',
        body: '부담을 느꼈던 시간과 도움을 구했던 순간을 돌아봤습니다. 학교를 떠나기로 했지만, 지금까지 배운 것과 만든 기록을 가지고 다음 배움을 준비합니다.',
        summary: {
          schoolStatus: 'WITHDRAWN', withdrawalYear: year, withdrawalTurn: turn, employed: false, nextPath: 'LEAVE_SCHOOL',
          initialCareerCode: initialCareerCode ?? undefined, currentCareerCode: currentCareerCode ?? undefined,
          major: major ?? undefined, track: track ?? undefined, finalStats: { ...stats }, qualifications: qualifications(g),
          projects: structuredClone(g.projects), applicationResults: structuredClone(g.applications),
          narsha: g.narsha, narshaProjects: [...(g.narshaProjects ?? []), ...(g.narsha ? [g.narsha] : [])],
          fieldLearningRoute: fieldLearningRoute(g), fieldLearning: fieldLearningSummary(g.fieldLearning, fieldLearningRoute(g)),
        },
      }
      resultCode = 'SCHOOL_WITHDRAWAL'
    }
  }
  return { choiceLabel: choice.label, description: choice.description, resultCode, stress: g.view.stats.S }
}

function rememberEvent(g: DemoGame, key: string, event: typeof stressSupportEvent) {
  const draws = g.eventDraws ??= {}
  draws[key] = { eventCode: event.code, situation: event.situation, choiceCodes: event.choices.map((choice) => choice.code) }
}

function drawSchoolEvent(g: DemoGame) {
  if (!isSchool(g.view) || g.view.vacation) return undefined
  const { year, turn } = g.view
  const key = `daily:${year}-${turn}`
  if (g.dailyChoicePeriod === key) return undefined
  const random = eventRandom(g.view.id, key)
  if (random.nextInt(100) >= 55) return undefined
  const pool = [...dailySchoolEvents, ...schoolActivityEvents(year, turn)]
  const period = (turn - 1) % semesterTurns + 1
  if ([4, 5, 8, 9, 11].includes(period)) pool.push(dailyExamEvent)
  const record = g.narsha
  if (record?.schoolYear === year && ['PLANNING', 'MENTORED'].includes(record.status)) {
    pool.push({ ...dailyNarshaEvent, situation: `‘${record.topicLabel}’ 팀이 남은 기능과 시연 범위를 두고 의견을 나눕니다.` })
  }
  const saved = g.eventDraws?.[key]
  const selected = pool.find((event) => event.code === saved?.eventCode) ?? pool[random.nextInt(pool.length)]
  const ordered = saved?.choiceCodes.map((code) => selected.choices.find((choice) => choice.code === code))
    .filter((choice): choice is (typeof selected.choices)[number] => choice !== undefined)
  const drawn = { ...selected, choices: ordered?.length === selected.choices.length ? ordered : random.shuffled(selected.choices) }
  rememberEvent(g, key, drawn)
  return drawn
}

function applyStressPressure(g: DemoGame): StressLoad {
  const random = eventRandom(g.view.id, `pressure:${stressPeriod(g)}`)
  const stressBefore = g.view.stats.S
  const sources: StressLoad['sources'] = []
  const add = (code: string, reason: string, amount: number) => {
    if (amount > 0) {
      const accumulated = sources.reduce((sum, source) => sum + Math.max(0, source.delta), 0)
      amount = Math.min(amount, Math.max(0, 5 - accumulated))
    }
    if (amount === 0) return
    const before = g.view.stats.S
    applyStats(g, { S: amount })
    sources.push({ code, reason, delta: g.view.stats.S - before })
  }
  add('SCHOOL_ROUTINE', '수업과 과제를 이어가며 부담이 조금 쌓였다.', random.nextInt(2))
  const period = (g.view.turn - 1) % semesterTurns + 1
  switch (period) {
    case 4: case 5: add('MIDTERM_PREP', '중간고사를 앞두고 복습할 범위를 정리했다.', 1 + random.nextInt(2)); break
    case 6: add('MIDTERM', '중간고사 시험을 치르며 긴장했다.', 3 + random.nextInt(3)); break
    case 8: case 9: add('ASSESSMENT_PREP', '수행평가 과제와 제출 일정을 맞췄다.', 1); break
    case 10: add('ASSESSMENT', '수행평가 결과물을 제출했다.', 3 + random.nextInt(2)); break
    case 11: add('FINAL_PREP', '기말고사 준비와 남은 과제가 겹쳤다.', 2 + random.nextInt(2)); break
    case 12: add('FINAL', '기말고사를 치르고 한 학기를 정리했다.', 4 + random.nextInt(3)); break
  }
  const record = g.narsha
  let deadline = false
  if (record?.schoolYear === g.view.year) {
    const end = g.view.year === 1 ? 23 : 22
    const developing = ['PLANNING', 'MENTORED'].includes(record.status)
    const presenting = g.view.year === 2 && g.view.turn === 22 && record.status === 'PRESENTED'
    if (developing || presenting) {
      deadline = g.view.turn >= end - 3
      add(deadline ? 'NARSHA_DEADLINE' : 'NARSHA_WORK',
        deadline ? '나르샤 마감과 발표가 가까워져 시연과 오류를 다시 점검했다.' : '나르샤 팀 작업과 수업 과제를 함께 진행했다.',
        deadline ? 2 + random.nextInt(2) : random.nextInt(2))
    }
  }
  if (![6, 10, 12].includes(period) && !deadline && g.view.stats.H >= 40) {
    add('DAILY_RECOVERY', '시험·제출·막판 마감이 없는 시기에는 일과를 마치고 쉬며 부담을 덜었다.', -2)
  }
  return { stressBefore, stressAfter: g.view.stats.S, total: g.view.stats.S - stressBefore, sources }
}

function requiredSchoolEvent(g: DemoGame) {
  const stress = stressEvent(g)
  if (stress) return stress
  if (g.view.year === 2 && !g.view.major) return majorEvent
  if (g.view.year === 3 && !g.view.track) return trackEvent
  const { year, turn, semesterFocus, vacation } = g.view
  if (vacation) return vacation.campTurn === 0 ? vacationEvent(vacation.season) : undefined
  if (!semesterFocus && (turn === 1 || turn === semesterTurns + 1)) return focusEvent
  if (year === 1 && turn >= 8 && turn <= 23 && !g.seen.includes(narshaTopicEvent.code)) return narshaTopicEvent
  if (year === 1 && turn === 14 && g.autoLife && !g.seen.includes(clubEvent.code)) return clubEvent
  if (year === 2 && turn >= 2 && turn <= 22 && !g.seen.includes(yearTwoNarshaEvent.code) &&
    (!g.narsha || (g.narsha.schoolYear ?? (g.narsha.status === 'PLANNING' || g.narsha.status === 'YEAR_ONE_COMPLETE' ? 1 : 2)) !== 2)) return yearTwoNarshaEvent
  if (year === 2 && turn >= 2 && turn <= 22 && !g.seen.includes(teamGoalEvent.code)) return teamGoalEvent
  if (year === 2 && turn === 22 && g.narsha?.status === 'MENTORED' && !g.seen.includes(narshaExpoEvent.code)) return narshaExpoEvent
  if (year === 2 && turn === 23 && (g.narsha?.rank ?? 8) <= 3 && !g.seen.includes(softwaveEvent.code)) return softwaveEvent
}

function eventDefinitions(g: DemoGame) {
  const { phase, year, turn, beforeSchoolDay } = g.view
  if (phase === 'ADMISSION_1' || phase === 'ADMISSION_2') return [admission[g.admissionIndex]]
  if (phase === 'BEFORE_SCHOOL') return [beforeSchool[(beforeSchoolDay ?? 1) - 1]]
  if (!isSchool(g.view)) return []
  const milestone = requiredSchoolEvent(g)
  if (milestone) return [milestone]
  if (g.view.vacation) return []
  if (year === 1 && turn === 1 && !g.seen.includes('E11')) return [dormEvent]
  if (year === 1 && turn === 7 && !g.seen.includes('S101')) return [pairDebugEvent]
  if (year === 3 && turn === applicationTurn && !g.seen.includes('J01')) return [careerEvent]
  const drawn = drawSchoolEvent(g)
  return drawn ? [drawn] : []
}

export function trackEnabled(g: DemoGame, code: AdmissionTrack) {
  return (g.view.admissionEligibility ?? ['GENERAL']).includes(code)
}

function campusVisits(g: DemoGame) {
  return new Set(g.history.filter((entry) => entry.subjectCode === 'BS01' && entry.resultCode === 'CAMPUS_VISITED')
    .map((entry) => entry.choiceCode))
}

export function events(g: DemoGame, focusOnly = false): EventView[] {
  const visited = campusVisits(g)
  const definitions = focusOnly
    ? isSchool(g.view) && !g.view.vacation && !g.view.semesterFocus ? [focusEvent] : []
    : eventDefinitions(g)
  return definitions.map((event) => ({
    code: event.code, title: event.title,
    required: event.required,
    blocksAuto: event.required || event.code === 'J01', source: event.source,
    selectionCount: ['BS01', 'BS02'].includes(event.code) ? activityCount : undefined,
    situation: g.narsha && (event === teamGoalEvent || event === narshaExpoEvent)
      ? `${g.narsha.topicLabel} 팀. ${event.situation}` : event.situation,
    choices: event.choices.map((choice) => {
      const track = event.code === 'AD00' ? choice.code as AdmissionTrack : undefined
      const cost = choice.cost ?? 0
      const visitedStop = event.code === 'BS01' && visited.has(choice.code)
      const reason = visitedStop ? '이미 둘러본 장소입니다.' :
        event.code === 'BS01' && choice.code === CAMPUS_FINISH_CHOICE && visited.size < activityCount ? '두 곳을 둘러보면 탐색을 마칠 수 있습니다.' :
        event.code === 'BS01' && choice.code !== CAMPUS_FINISH_CHOICE && visited.size >= activityCount ? '둘러볼 장소를 이미 두 곳 골랐습니다.' :
        track && !trackEnabled(g, track) ? '이번 회차에는 이 전형으로 지원할 수 없습니다.' :
        cost > g.view.remainingAp ? '남은 AP가 부족합니다.' : undefined
      const description = track && track !== 'GENERAL' ? `게임 내 지원 자격 추첨 ${admissionTracks[track].eligibilityChance}%` : choice.description
      return { code: choice.code, label: choice.label, description, apCost: cost,
        enabled: !reason, reason, visited: visitedStop || undefined }
    }),
  }))
}

function applyStats(g: DemoGame, delta: Partial<Stats>, scaled = false, bonus = 0) {
  const before = { ...g.view.stats }
  const efficiency = before.H < 40 || before.S >= 65 ? .75 : 1
  for (const [key, raw] of Object.entries(delta)) {
    const stat = key as Stat
    const diminishing = before[stat] < 60 ? 1 : before[stat] < 80 ? .75 : .5
    let amount = raw
    if (scaled && (skills.includes(stat) || stat === 'EN') && raw > 0) {
      const carry = g.statGrowthCarry ??= {}
      const earned = (raw + bonus) * .25 * diminishing * efficiency + (carry[stat] ?? 0)
      amount = Math.floor(earned + .0000001)
      carry[stat] = before[stat] + amount >= 100 ? 0 : earned - amount
    }
    g.view.stats[stat] = cap(before[stat] + amount)
  }
}

function receipt(g: DemoGame, before: PlayerView, actionCode: string, resultCode: string, details?: Record<string, unknown>) {
  const statDelta = Object.fromEntries(Object.entries(g.view.stats)
    .map(([key, value]) => [key, value - before.stats[key as Stat]])
    .filter(([, delta]) => delta !== 0))
  return { player: g.view, actionCode, spentAp: actionCode === 'TURN_ADVANCE' ? 0 : Math.max(0, before.remainingAp - g.view.remainingAp), statDelta, resultCode, details }
}

function log(g: DemoGame, before: PlayerView, kind: string, subjectCode: string, resultCode: string, choiceCode?: string) {
  const entry: DemoHistory = { logId: g.history.length + 1, year: before.year, turn: before.turn,
    beforeSchoolDay: before.beforeSchoolDay, kind, subjectCode, resultCode, choiceCode }
  g.history.push(entry)
  return entry
}

function admissionDecision(stage: number, track: AdmissionTrack, components: number[]) {
  const firstStage = stage === 1
  const weights = firstStage ? admissionTracks[track].weights : [25, 30, 20, 25]
  const total = components.reduce((sum, value, i) => sum + value * weights[i], 0)
  const seats = firstStage ? 144 : 72
  const count = seats * 2 - 1
  const floor = track === 'GENERAL' ? 26 : firstStage ? 30 : 32
  const spread = firstStage ? 65 : 60
  const rivals = Array.from({ length: count }, (_, i) => floor + Math.floor(spread * i / (count - 1)))
  const rank = 1 + rivals.filter((score) => score * 100 >= total).length
  const minimumScore = ((floor + Math.floor(spread / 2)) * 100 + 1) / 100
  return { score: total / 100, rank, minimumScore, passed: rank <= seats }
}

function recordAdmission(g: DemoGame, code: string, choiceCode: string, interviewEvaluation?: InterviewEvaluation) {
  g.admissionChoices.push(choiceCode)
  g.admissionIndex++
  if (code === 'AD00') g.view.admissionTrack = choiceCode
  if (code !== 'AD03' && code !== 'AD05') return undefined

  const stage = code === 'AD03' ? 1 : 2
  const { A, P, C, G } = g.view.stats
  const components = stage === 1 ? [
    g.admissionChoices[2] === 'REWRITE' ? 78 : 48,
    G,
    choiceCode === 'ADD_EVIDENCE' ? 76 : 56,
  ] : [
    cap(36 + P + (g.admissionChoices[4] === 'SCRATCH' ? 14 : 0)),
    cap(34 + P + (g.admissionChoices[4] === 'PYTHON' ? 14 : 0)),
    cap(32 + A + (choiceCode === 'LOGIC' ? 14 : 0)),
    interviewEvaluation?.score ?? cap(35 + C + (choiceCode === 'MOCK' ? 14 : 0)),
  ]
  const track = g.view.admissionTrack as AdmissionTrack
  const original = admissionDecision(stage, track, components)
  const protection = !original.passed && track !== 'GENERAL'
    ? { specialTrack: track, score: original.score, rank: original.rank, minimumScore: original.minimumScore } : undefined
  const finalTrack = protection ? 'GENERAL' : track
  const result = protection ? admissionDecision(stage, finalTrack, components) : original
  g.view.admissionTrack = finalTrack
  const summary = { stage, admissionTrack: finalTrack, ...result, components, interviewEvaluation, protection }
  if (!result.passed) {
    g.view.phase = 'ENDED'
    g.ending = { endingCode: `ADMISSION_REJECT_${stage}`, title: stage === 1 ? '1차 결과' : '최종 전형 결과',
      body: `합산 점수 ${result.score.toFixed(2)}점으로 ${result.rank}위였습니다. 이번 전형은 상위 ${stage === 1 ? 144 : 72}명, 최소 ${result.minimumScore.toFixed(2)}점이 통과했습니다.`,
      summary }
  } else if (stage === 1) {
    g.view.phase = 'ADMISSION_2'
  } else {
    g.view.phase = 'BEFORE_SCHOOL'
    g.view.beforeSchoolDay = 1
    g.kit = g.admissionChoices.includes('SUBMIT') || g.admissionChoices.includes('KEEP') ? 2 : 1
  }
  return summary
}

function recordActivityEvidence(g: DemoGame, code: string, choice: DemoChoice, day: number) {
  if (choice.tag && !g.evidence.some((item) => item.tag === choice.tag)) {
    g.evidence.push({ day, eventCode: code, choiceCode: choice.code, activityLabel: choice.label, tag: choice.tag, contribution: 3 })
  }
}

function chooseActivities(g: DemoGame, code: string, input: unknown) {
  const event = eventDefinitions(g).find((entry) => entry.code === code)
  const campus = code === 'BS01'
  if (!event || (!campus && code !== 'BS02')) return block('현재 단계에서 선택할 수 없는 이벤트입니다.', 'WRONG_PHASE')
  const visited = campus ? campusVisits(g) : new Set<string>()
  if (!Array.isArray(input) || input.length !== Math.max(0, activityCount - visited.size)) {
    return block('남은 선택 가능 수만큼 활동을 골라 주세요.', 'ACTIVITY_COUNT_REQUIRED')
  }
  const selections = input.map((selected: unknown) => event.choices.find((choice) =>
    choice.code === selected && choice.code !== CAMPUS_FINISH_CHOICE && !visited.has(choice.code)))
  if (selections.some((choice) => !choice) || new Set(input).size !== input.length) {
    return block('서로 다른 활동을 선택해 주세요.', 'INVALID_ACTIVITY_SELECTION')
  }
  const before = structuredClone(g.view)
  const day = before.beforeSchoolDay ?? 1
  const records = []
  for (const choice of selections) {
    if (!choice) continue
    applyStats(g, choice.delta ?? {})
    recordActivityEvidence(g, code, choice, day)
    log(g, before, 'EVENT_CHOICE', code, campus ? 'CAMPUS_VISITED' : 'CHOICE_SAVED', choice.code)
    records.push({ title: (campus ? '비포스쿨 첫날 · ' : '전공 체험 · ') + choice.label, description: choice.description ?? choice.label })
  }
  g.snapshots.push({ phase: 'BEFORE_SCHOOL', beforeSchoolDay: day, stats: { ...g.view.stats } })
  g.view.beforeSchoolDay = day + 1
  g.seen.push(code)
  const entry = log(g, before, 'EVENT_CHOICE', code, 'CHOICE_SAVED', campus ? CAMPUS_FINISH_CHOICE : undefined)
  entry.beforeSchoolActivities = records
  return receipt(g, before, code, 'CHOICE_SAVED', { playedDay: day, nextDay: day + 1, recommendationReady: false })
}

function chooseEvent(g: DemoGame, code: string, choiceCode: string, customTopic?: unknown) {
  const topic = typeof customTopic === 'string' ? customTopic.trim() : ''
  if (choiceCode === 'NARSHA_CUSTOM' && (topic.length < 2 || topic.length > 80 || /[\x00-\x1f\x7f-\x9f]/.test(topic))) {
    throw new DemoError(400, 'VALIDATION_ERROR', '프로젝트 주제는 2~80자의 한 줄로 입력해 주세요.')
  }
  const before = structuredClone(g.view)
  const isFocus = code === focusEvent.code
  const event = isFocus ? focusEvent : eventDefinitions(g).find((entry) => entry.code === code)
  const choice = event?.choices.find((entry) => entry.code === choiceCode)
  const visible = events(g, isFocus).find((entry) => entry.code === code)?.choices.find((entry) => entry.code === choiceCode)
  if (!event || !choice || !visible) return block('현재 단계에서 선택할 수 없는 이벤트입니다.', 'WRONG_PHASE')
  if (code === 'BS02') return block('체험 두 가지를 골라 주세요.', 'ACTIVITY_COUNT_REQUIRED')
  if (!visible.enabled) {
    const errorCode = visible.visited ? 'CAMPUS_ALREADY_VISITED' :
      code === 'BS01' && choiceCode === CAMPUS_FINISH_CHOICE ? 'CAMPUS_VISIT_REQUIRED' :
      code === 'BS01' ? 'CAMPUS_VISIT_LIMIT' :
      code === 'AD00' ? 'ADMISSION_INELIGIBLE' : 'INSUFFICIENT_AP'
    return block(visible.reason ?? '선택할 수 없습니다.', errorCode)
  }
  if (!isFocus) applyStats(g, choice.delta ?? {}, isSchool(g.view))
  g.view.remainingAp -= visible.apCost
  let details: Record<string, unknown> | undefined
  let admissionResult: ReturnType<typeof recordAdmission>
  let resultCode = 'CHOICE_SAVED'
  if (code.startsWith('AD')) {
    admissionResult = recordAdmission(g, code, choiceCode)
    if (admissionResult) resultCode = admissionResult.passed ? 'ADMISSION_PASSED' : 'ADMISSION_REJECTED'
    details = admissionResult
  } else if (code.startsWith('BS')) {
    const day = g.view.beforeSchoolDay ?? 1
    recordActivityEvidence(g, code, choice, day)
    if (code === 'BS01' && choiceCode !== CAMPUS_FINISH_CHOICE) {
      log(g, before, 'EVENT_CHOICE', code, 'CAMPUS_VISITED', choiceCode)
      return receipt(g, before, code, 'CAMPUS_VISITED', { playedDay: day, campusLocation: choice.label })
    }
    g.snapshots.push({ phase: 'BEFORE_SCHOOL', beforeSchoolDay: day, stats: { ...g.view.stats } })
    details = { playedDay: day, activityTag: choice.tag, nextDay: day < 5 ? day + 1 : null, recommendationReady: day === 5 }
    if (day < 5) g.view.beforeSchoolDay = day + 1
    else {
      g.view.phase = 'CAREER_PICK'
      g.recommendations = roles.map(([roleCode, label, , weights, tags], index) => {
        const reasons = skills.map((statCode, i) => ({
          statCode, value: g.view.stats[statCode], contribution: weights[i] * g.view.stats[statCode] / 100,
        }))
        const activityReasons = g.evidence.filter((item) => tags.includes(item.tag))
        return { index, rank: 0, roleCode, label, score: Number((reasons.reduce((sum, item) => sum + item.contribution, 0) + 3 * activityReasons.length).toFixed(2)),
          statReasons: reasons.sort((a, b) => b.contribution - a.contribution).slice(0, 2), activityReasons }
      }).sort((a, b) => b.score - a.score || b.activityReasons.length - a.activityReasons.length || a.index - b.index)
        .slice(0, 3).map(({ index: _index, ...item }, i) => ({ ...item, rank: i + 1 }))
    }
  } else if (code === 'J01' && isFieldLearningRoute(choiceCode)) {
    g.fieldLearning = selectFieldLearning(choiceCode, g.view.stats.EN)
    details = { fieldLearningRoute: choiceCode, fieldLearning: g.fieldLearning }
    resultCode = 'FIELD_LEARNING_RECORDED'
  } else if (isFocus) {
    g.view.semesterFocus = { code: choiceCode, label: choice.label, semester: g.view.turn <= semesterTurns ? 1 : 2 }
    details = { ...g.view.semesterFocus }
    resultCode = 'FOCUS_SELECTED'
  } else if (code === 'VACATION' && before.vacation) {
    details = { ...before.vacation, choice: choiceCode }
    if (choiceCode === 'MAJOR_CAMP') {
      g.view.vacation = { ...before.vacation, campTurn: 1 }
      resultCode = 'CAMP_STARTED'
    } else {
      applyStats(g, { H: 10, S: -8 })
      finishVacation(g)
      resultCode = 'VACATION_RESTED'
    }
  } else if (code === 'Y201') {
    g.view.major = choiceCode
  } else if (code === 'Y301') {
    g.view.track = choiceCode
  } else if (code === narshaTopicEvent.code || code === yearTwoNarshaEvent.code) {
    if (g.narsha) {
      const schoolYear = g.narsha.schoolYear ?? (g.narsha.status === 'PLANNING' || g.narsha.status === 'YEAR_ONE_COMPLETE' ? 1 : 2)
      if (schoolYear !== g.view.year) (g.narshaProjects ??= []).push({ ...g.narsha, schoolYear })
    }
    g.narsha = { topicCode: choiceCode, topicLabel: choiceCode === 'NARSHA_CUSTOM' ? topic : choice.label,
      schoolYear: g.view.year, status: 'PLANNING' }
    details = { narsha: g.narsha }
  } else if (code === teamGoalEvent.code) {
    g.narsha = { topicCode: 'NARSHA_WEB', topicLabel: narshaTopics.NARSHA_WEB.label, ...g.narsha,
      status: 'MENTORED', mentorChoice: choice.label }
    details = { narsha: g.narsha }
  } else if (code === narshaExpoEvent.code && g.narsha) {
    const stats = g.view.stats
    const topicStat = narshaTopics[g.narsha.topicCode as keyof typeof narshaTopics]?.stat ?? 'F'
    const work = g.narsha.yearTwoWorkTurns ?? 0
    const competency = 20 * stats.P + 25 * stats[topicStat] + 20 * stats.C + 10 * stats.A + 20 * stats.G + 5 * stats.H
    const progress = Math.floor(Math.min(20, work) * 100 / 20)
    const quality = Math.floor((85 * competency + 1_500 * progress) / 10_000)
    const rank = 1 + [96, 92, 87, 82, 76, 69, 62].filter((score) => score >= quality).length
    g.narsha = { ...g.narsha, status: 'PRESENTED', presentationChoice: choice.label, quality, rank }
    details = { narsha: g.narsha }
  } else if (code === softwaveEvent.code && g.narsha) {
    g.narsha.softwaveStatus = choiceCode === 'SOFTWAVE_ATTEND' ? 'ATTENDED' : 'DECLINED'
    details = { narsha: g.narsha }
  }
  if (code.startsWith('DAILY_')) g.dailyChoicePeriod = `daily:${g.view.year}-${g.view.turn}`
  const stress = recordStressChoice(g, code, choice)
  if (stress) {
    details = { stressEvent: stress }
    resultCode = stress.resultCode
  }
  if (!isFocus && code !== 'VACATION' && !stressEventCodes.has(code)) g.seen.push(code)
  const entry = log(g, before, 'EVENT_CHOICE', code, resultCode, choiceCode)
  if (admissionResult) entry.admission = admissionResult
  if (stress) entry.stressEvent = stress
  if (code.startsWith('DAILY_')) entry.dailyEvent = { title: event.title, choiceLabel: choice.label, situation: event.situation }
  if (details?.narsha && g.narsha) entry.narsha = { ...g.narsha }
  if (isFocus) entry.semesterFocus = g.view.semesterFocus
  if (code === 'VACATION') entry.vacation = before.vacation
  return receipt(g, before, code, resultCode, details)
}

export function admissionInterview(g: DemoGame) {
  if (eventDefinitions(g)[0]?.code !== 'AD05') return block('현재 입학 면접을 진행할 수 없습니다.', 'WRONG_PHASE')
  return schoolQuestion(g.view.id, g.admissionChoices[4])
}

function interviewAnswer(question: InterviewQuestion, body: Record<string, unknown>) {
  if (body.questionId !== question.id) return block('현재 면접 질문을 다시 불러와 주세요.', 'INTERVIEW_QUESTION_MISMATCH')
  const answer = typeof body.answer === 'string' ? body.answer.trim() : ''
  if (!answer || answer.length > 1_200) throw new DemoError(400, 'VALIDATION_ERROR', '면접 답변은 1~1,200자로 입력해 주세요.')
  return answer
}

function answerAdmission(g: DemoGame, body: Record<string, unknown>) {
  const question = admissionInterview(g)
  const evaluation = gradeInterview(interviewAnswer(question, body))
  const before = structuredClone(g.view)
  const details = recordAdmission(g, 'AD05', 'ANSWER', evaluation)!
  const result = details.passed ? 'ADMISSION_PASSED' : 'ADMISSION_REJECTED'
  g.seen.push('AD05')
  log(g, before, 'EVENT_CHOICE', 'AD05', result, 'ANSWER').admission = details
  return receipt(g, before, 'AD05', result, details)
}

export function preferences(g: DemoGame): CareerPreferenceView {
  if (!g.recommendations.length) return block('비포스쿨을 완료하면 추천을 볼 수 있습니다.', 'RECOMMENDATION_NOT_READY')
  const { year, turn, initialCareerCode, currentCareerCode } = g.view
  const windowOpen = isSchool(g.view) && !requiredSchoolEvent(g) && (year === 2 || (year === 3 && turn < applicationTurn))
  const eligible = [...new Set([...g.recommendations.map((item) => item.roleCode), ...g.practiced,
    ...g.projects.filter((project) => project.status === 'COMPLETE').flatMap((project) => projects[project.projectCode].careers)])]
    .filter((role) => role !== currentCareerCode)
  const canChange = Boolean(initialCareerCode && windowOpen && !g.careerChanged && eligible.length)
  return { initialCareerCode, currentCareerCode, recommendations: g.recommendations, canChange, eligibleChangeRoleCodes: canChange ? eligible : [] }
}

function pickCareer(g: DemoGame, role: string) {
  const before = structuredClone(g.view)
  const choices = preferences(g)
  if (g.view.phase === 'CAREER_PICK') {
    if (!choices.recommendations.some((item) => item.roleCode === role)) return block('추천 직무 세 가지에서 골라 주세요.', 'CAREER_NOT_RECOMMENDED')
    Object.assign(g.view, { initialCareerCode: role, currentCareerCode: role, phase: 'Y1', year: 1, turn: 1, remainingAp: 4, beforeSchoolDay: null })
  } else {
    if (!choices.eligibleChangeRoleCodes.includes(role)) return block('지금 변경할 수 없는 희망 직무입니다.', 'CAREER_CHANGE_LOCKED')
    g.view.currentCareerCode = role
    g.careerChanged = true
  }
  log(g, before, before.phase === 'CAREER_PICK' ? 'CAREER_PICK' : 'CAREER_CHANGE', role, 'SAVED')
  return { player: g.view, preference: preferences(g) }
}

function schoolOnly(g: DemoGame) {
  if (!isSchool(g.view) || g.view.vacation || requiredSchoolEvent(g)) block('먼저 학년 또는 수업 선택을 완료해 주세요.', 'WRONG_PHASE')
}

function fatigue(g: DemoGame) { return g.view.stats.H < 20 || g.view.stats.S >= 85 }

function actionSlot(code: string) { return code === 'ROLE_PRACTICE' ? 'STUDY' : code }

function specialization(player: PlayerView): Stat[] {
  if (player.year === 2) return player.major === 'AI' ? ['P', 'I'] : ['F', 'B']
  if (player.year === 3) return [player.track === 'FE' ? 'F' : 'B']
  return []
}

export function actions(g: DemoGame): AvailableAction[] {
  if (!isSchool(g.view) || g.view.vacation || requiredSchoolEvent(g)) return []
  const { year, turn, resumeScore, stats } = g.view
  const result: AvailableAction[] = []
  function add(code: string, cost: number, target?: string, reason?: string, heavy = false) {
    const lock = reason || (g.used.includes(actionSlot(code)) ? '이번 턴에 이미 실행했습니다.' :
      cost > g.view.remainingAp ? '남은 AP가 부족합니다.' :
      heavy && fatigue(g) ? '체력이나 스트레스를 먼저 회복해 주세요.' : undefined)
    result.push({ code, target, cost, enabled: !lock, reason: lock })
  }
  for (const stat of ['P', 'F', 'B', 'I', 'EN', 'A']) add('STUDY', 2, stat, undefined, true)
  const classStats = year === 1 ? ['P', 'F', 'B', 'A'] : specialization(g.view)
  for (const stat of classStats) add('CLASS', 2, stat, undefined, true)
  for (const [role] of roles) add('ROLE_PRACTICE', 2, role, undefined, true)
  add('REST', 1)
  add('SOCIAL', 1)
  const enrollment = g.enrollments.find((item) => item.status === 'ENROLLED')
  if (year < 3 && turn === 1 && !enrollment && !g.enrollments.some((item) => item.status === 'PASSED' || item.year === year)) {
    add('COURSE_ENROLL', 1, 'C_INFO', stats.P < 20 ? '프로그래밍 20점 이상이 필요합니다.' : undefined)
  }
  if (enrollment) {
    if (turn < applicationTurn && enrollment.attendedTurns.length < 2) add('ATTEND', 1, enrollment.id,
      enrollment.attendedTurns.includes(turn) ? '다른 턴에 출석해 주세요.' : undefined, true)
    add('EXAM', 1, enrollment.id, enrollment.attendedTurns.length === 2 && turn >= applicationTurn && turn > Math.max(...enrollment.attendedTurns)
      ? undefined : '서로 다른 두 턴에 출석한 뒤 12~24턴에 응시할 수 있습니다.', true)
  }
  const activeProject = g.projects.find((item) => item.status === 'IN_PROGRESS')
  if (activeProject) add('PROJECT_WORK', 2, activeProject.id, undefined, true)
  else for (const [code, def] of Object.entries(projects)) {
    if (year >= def.from && !g.projects.some((item) => item.projectCode === code)) {
      add('PROJECT_START', 1, code, turn + def.duration - 1 > yearTurns ? '올해 남은 기간이 부족합니다.' : undefined)
    }
  }
  if (g.projects.filter((item) => item.inPortfolio).length < 2) {
    for (const item of g.projects.filter((item) => item.status === 'COMPLETE' && !item.inPortfolio)) add('PORTFOLIO_ADD', 1, item.id)
  }
  if (year === 3) {
    if (resumeScore < 70) add('RESUME', 1)
    if (!g.prep.coding) add('CODING_PREP', 1)
    if (!g.prep.interview) add('INTERVIEW_PREP', 1)
    if (turn >= applicationTurn) {
      add('PATH_PREP', 1, 'STUDY_NEXT')
      add('PATH_PREP', 1, 'INDIE')
    }
  }
  return result
}

function projectQuality(g: DemoGame, project: typeof projects[string], average: number) {
  const { P, C } = g.view.stats
  const teamBonus = project.team ? .12 * C : 0
  const penalty = Math.max(0, project.target - average) * .25
  return cap(Math.floor(24 + .45 * average + .18 * P + teamBonus - penalty))
}

function useAction(g: DemoGame, code: string, target?: string) {
  schoolOnly(g)
  const action = actions(g).find((item) => item.code === code && item.target === target)
  if (!action?.enabled) return block(action?.reason ?? '지금 실행할 수 없는 행동입니다.', action && action.cost > g.view.remainingAp ? 'INSUFFICIENT_AP' : 'LOCKED')
  const before = structuredClone(g.view)
  const { year, turn, currentCareerCode, stats } = g.view
  let outcome = 'DONE'
  switch (code) {
    case 'STUDY':
    case 'CLASS':
    case 'ROLE_PRACTICE': {
      const stat = code === 'ROLE_PRACTICE' ? roles.find(([role]) => role === target)![2] : target as Stat
      const bonus = (specialization(g.view).includes(stat) ? 2 : 0) + (code === 'ROLE_PRACTICE' && currentCareerCode === target ? 1 : 0)
      applyStats(g, { [stat]: code === 'CLASS' ? 9 : 7, H: -6, S: 7 }, true, bonus)
      if (code !== 'ROLE_PRACTICE') prepareAssessment(g, 2)
      if (code === 'ROLE_PRACTICE' && target && !g.practiced.includes(target)) g.practiced.push(target)
      break
    }
    case 'REST': applyStats(g, { H: 14, S: -12 }); break
    case 'SOCIAL': applyStats(g, { C: 6, H: -2, S: -3 }, true); break
    case 'COURSE_ENROLL':
      g.enrollments.push({ id: randomUUID(), courseCode: 'C_INFO', status: 'ENROLLED', year, attendedTurns: [] })
      break
    case 'ATTEND':
      g.enrollments.find((item) => item.id === target)!.attendedTurns.push(turn)
      applyStats(g, { P: 2, A: 2, H: -2, S: 3 }, true)
      break
    case 'EXAM': {
      const entry = g.enrollments.find((item) => item.id === target)!
      entry.score = cap(.65 * stats.P + .35 * stats.A + 18)
      entry.status = outcome = entry.score >= 45 ? 'PASSED' : 'FAILED'
      applyStats(g, { H: -3, S: 4 })
      break
    }
    case 'PROJECT_START':
      g.projects.push({ id: randomUUID(), projectCode: target!, status: 'IN_PROGRESS', workCount: 0, year, deadline: turn + projects[target!].duration - 1, readings: [] })
      break
    case 'PROJECT_WORK': {
      const entry = g.projects.find((item) => item.id === target)!
      const def = projects[entry.projectCode]
      entry.readings.push(stats[def.stat])
      entry.workCount = entry.readings.length
      if (entry.readings.length === 2) {
        const avg = (entry.readings[0] + entry.readings[1]) / 2
        entry.quality = projectQuality(g, def, avg)
        entry.status = outcome = entry.quality >= 46 ? 'COMPLETE' : 'FAILED'
      }
      applyStats(g, { H: -5, S: 6 })
      break
    }
    case 'PORTFOLIO_ADD': g.projects.find((item) => item.id === target)!.inPortfolio = true; break
    case 'RESUME': g.view.resumeScore = g.view.resumeScore === 0 ? 45 : 70; break
    case 'CODING_PREP':
    case 'INTERVIEW_PREP':
      g.prep[code === 'CODING_PREP' ? 'coding' : 'interview'] = true
      applyStats(g, { H: -2, S: 2 })
      break
    case 'PATH_PREP': g.prep[target === 'STUDY_NEXT' ? 'study' : 'indie']++; break
  }
  g.used.push(actionSlot(code))
  g.view.remainingAp -= action.cost
  log(g, before, 'ACTION', code, outcome, target)
  return receipt(g, before, code, outcome)
}

export function inventory(g: DemoGame): ItemStack[] {
  if (!g.kit) return []
  const reason = !isSchool(g.view) || g.view.vacation || requiredSchoolEvent(g) ? '재학 단계의 필수 선택 후에 사용할 수 있습니다.' :
    g.used.includes('USE_ITEM') ? '이번 턴에 이미 사용했습니다.' :
    g.view.stats.H === 100 && g.view.stats.S === 0 ? '지금은 회복할 수치가 없습니다.' : undefined
  return [{ itemCode: 'RECOVERY_KIT', name: '회복 키트', quantity: g.kit, enabled: !reason, reason }]
}

function useItem(g: DemoGame, itemCode: string) {
  const item = inventory(g).find((entry) => entry.itemCode === itemCode)
  if (!item?.enabled) return block(item?.reason ?? '사용할 수 있는 회복 키트가 없습니다.')
  const before = structuredClone(g.view)
  g.kit--
  g.used.push('USE_ITEM')
  applyStats(g, { H: 10, S: -8 })
  log(g, before, 'ITEM_USE', itemCode, 'DONE')
  return receipt(g, before, 'USE_ITEM', 'DONE')
}

function applyRoutine(g: DemoGame) {
  const { year, turn, currentCareerCode } = g.view
  const friendContact = turn === 2 || (year === 1 && turn === 1)
  const growth: Partial<Stats> = { P: year === 1 ? 2 : 1, A: 1, EN: year === 2 ? 2 : 1,
    C: friendContact ? 2 : 1, H: turn % 2 === 0 ? -2 : 6 }
  if (year === 1) Object.assign(growth, { F: 1, B: 1, I: 1 })
  for (const stat of specialization(g.view)) growth[stat] = (growth[stat] ?? 0) + (year === 3 ? 2 : 1)
  const careerStat = roles.find(([code]) => code === currentCareerCode)?.[2]
  if (careerStat) growth[careerStat] = (growth[careerStat] ?? 0) + 1
  applyStats(g, growth, true)
  prepareAssessment(g, 1)

  const activities: RoutineActivity[] = []
  const run = (code: string, target?: string) => {
    const available = actions(g).find((item) => item.code === code && item.enabled && (!target || item.target === target))
    if (!available) return
    const outcome = useAction(g, code, available.target ?? undefined)
    const projectCode = code === 'PROJECT_START' ? available.target : g.projects.find((item) => item.id === available.target)?.projectCode
    const subject = projectCode ? projects[projectCode].title : ['COURSE_ENROLL', 'ATTEND', 'EXAM'].includes(code) ? '정보처리기능사' : undefined
    activities.push({ action: code, result: outcome.resultCode, subject })
  }

  if (year < 3) {
    const target = Object.entries(projects).find(([code, item]) => item.from <= year
      && item.careers.includes(currentCareerCode ?? '') && g.view.stats[item.stat] >= item.target
      && projectQuality(g, item, g.view.stats[item.stat]) >= 46 && !g.projects.some((project) => project.projectCode === code))?.[0]
    if (target) run('PROJECT_START', target)
  }
  if (year === 3 && turn < applicationTurn) {
    run('RESUME')
    run(turn === 1 ? 'INTERVIEW_PREP' : 'CODING_PREP')
  }
  run('PROJECT_WORK')
  run('PORTFOLIO_ADD')
  if (year < 3) {
    run('COURSE_ENROLL')
    run('ATTEND')
    run('EXAM')
  }
  return { friendContact, activities }
}

function recruitmentDecisionOpen(g: DemoGame) {
  const { year, turn, remainingAp } = g.view
  if (year !== 3 || !remainingAp) return false
  if (turn === applicationTurn) return g.applications.length < 2
  const pendingStatus: Record<number, string> = { [codingTurn]: 'DOC_PASSED', [interviewTurn]: 'CODE_PASSED', [resultTurn]: 'INTERVIEW_PASSED' }
  return g.applications.some((item) => item.status === pendingStatus[turn])
}

function finishVacation(g: DemoGame) {
  const { year, turn } = g.view
  delete g.view.vacation
  delete g.view.semesterFocus
  if (turn === semesterTurns) Object.assign(g.view, { turn: semesterTurns + 1, remainingAp: 4 })
  else if (year === 3) g.view.phase = 'GRAD_READY'
  else Object.assign(g.view, { phase: `Y${year + 1}`, year: year + 1, turn: 1, remainingAp: 4 })
}

function advanceCamp(g: DemoGame, vacation: VacationView) {
  const before = structuredClone(g.view)
  const specialty = specialization(g.view).at(-1) ?? 'A'
  applyStats(g, { P: 3, [specialty]: 2, S: 1 }, true)
  const result = vacation.campTurn === 1 ? 'CAMP_DAY_DONE' : 'CAMP_COMPLETED'
  if (vacation.campTurn === 1) g.view.vacation = { ...vacation, campTurn: 2 }
  else finishVacation(g)
  const entry = log(g, before, 'TURN_ADVANCE', 'VACATION', result, 'MAJOR_CAMP')
  entry.vacation = vacation
  return receipt(g, before, 'TURN_ADVANCE', result, { ...vacation })
}

function applyFocus(g: DemoGame) {
  const focus = g.view.semesterFocus
  if (!focus) return
  const choice = focusEvent.choices.find((option) => option.code === focus.code)
  const stat = roles.find(([code]) => code === g.view.currentCareerCode)?.[2] ?? 'P'
  applyStats(g, focus.code === 'FOCUS_CAREER' ? { [stat]: 3 } : choice?.delta ?? {}, true)
  if (focus.code === 'FOCUS_GRADES') prepareAssessment(g, 2)
}

function applySetbacks(g: DemoGame, turnStart: PlayerView): StatSetback[] {
  const setbacks: StatSetback[] = []
  function apply(reasonCode: StatSetback['reasonCode'], reason: string, delta: Partial<Stats>) {
    const before = { ...g.view.stats }
    applyStats(g, delta)
    const statDelta: Partial<Stats> = {}
    for (const stat of Object.keys(delta) as Stat[]) {
      const change = g.view.stats[stat] - before[stat]
      if (change) statDelta[stat] = change
    }
    if (Object.keys(statDelta).length) setbacks.push({ reasonCode, reason, statDelta })
  }
  if (turnStart.stats.H < 40 || turnStart.stats.S >= 65) {
    apply('OVERWORK', '피로와 스트레스가 쌓여 수업과 실습에 집중하기 어려웠다.', { P: -2, A: -1 })
  }
  const focus = g.view.semesterFocus?.code ?? 'FOCUS_BALANCE'
  const { year, turn, stats } = g.view
  if (turn % 5 !== 0 || focus === 'FOCUS_BALANCE') return setbacks
  const careerStat = roles.find(([code]) => code === g.view.currentCareerCode)?.[2] ?? 'P'
  const maintained = new Set<Stat>([careerStat, ...specialization(g.view)])
  if (focus === 'FOCUS_ENGLISH') maintained.add('EN')
  const neglected = (['F', 'B', 'I', 'EN'] as Stat[]).filter((stat) => !maintained.has(stat) && stats[stat] > 10)
  if (neglected.length) {
    const index = ((year - 1) * 4 + Math.floor(turn / 5) - 1) % neglected.length
    const stat = neglected[index]
    const loss = Math.min(stats[stat] - 10, stats[stat] >= 60 ? 2 : 1)
    apply('SKILL_RUST', '다른 공부에 시간을 쓰는 동안 덜 연습한 기술의 감각이 조금 무뎌졌다.', { [stat]: -loss })
  }
  return setbacks
}

function advanceNarsha(g: DemoGame) {
  const { year, turn } = g.view
  const record = g.narsha
  if (!record || (record.schoolYear != null && record.schoolYear !== year)) return
  if (year === 1 && record.status === 'PLANNING') {
    record.yearOneWorkTurns = (record.yearOneWorkTurns ?? 0) + 1
    if (turn === 23) {
      record.status = 'YEAR_ONE_COMPLETE'
      return { ...record }
    }
  } else if (year === 2 && record.status === 'MENTORED') {
    record.yearTwoWorkTurns = (record.yearTwoWorkTurns ?? 0) + 1
  }
}

function advance(g: DemoGame, automatic = false) {
  if (automatic && isSchool(g.view) && requiredSchoolEvent(g)) {
    return block('중대 결정을 마친 뒤 시간이 다시 흐릅니다.', 'DECISION_PENDING')
  }
  if (stressEvent(g)) return block('먼저 지금의 학교생활 고민을 선택해 주세요.', 'DECISION_PENDING')
  if (g.view.vacation?.campTurn) return advanceCamp(g, g.view.vacation)
  schoolOnly(g)
  const before = structuredClone(g.view)
  const { year, turn, stats } = g.view
  const event = automatic ? eventDefinitions(g)[0] : undefined
  if (automatic && (event?.required || event?.code === 'J01' || recruitmentDecisionOpen(g))) {
    return block('열린 진로 선택을 마치거나 이번 시기를 직접 넘겨 주세요.', 'DECISION_PENDING')
  }
  if (automatic) g.autoLife = true
  const routine = automatic || g.view.semesterFocus ? applyRoutine(g) : undefined
  applyFocus(g)
  const stressLoad = applyStressPressure(g)
  const setbacks = applySetbacks(g, before)
  const narsha = advanceNarsha(g)
  const assessments = evaluateAssessments(g)
  const field = advanceFieldLearning(g.fieldLearning, year, turn)
  if (field) {
    g.fieldLearning = field.experience
    applyStats(g, field.delta)
    log(g, before, 'FIELD_LEARNING', 'FIELD_LEARNING', field.resultCode, field.experience.route)
  }
  g.snapshots.push({ phase: 'SCHOOL', year, turn, stats: { ...stats } })
  for (const project of g.projects) if (project.status === 'IN_PROGRESS' && project.deadline <= turn) project.status = 'EXPIRED'
  for (const enrollment of g.enrollments) if (enrollment.status === 'ENROLLED' && turn >= yearTurns) enrollment.status = 'EXPIRED'
  if (year === 3) {
    const expiry: Record<string, number> = { DOC_PASSED: codingTurn, CODE_PASSED: interviewTurn, INTERVIEW_PASSED: resultTurn }
    for (const application of g.applications) if (expiry[application.status] === turn) {
      application.status = 'EXPIRED'
      application.failureCode = 'STEP_EXPIRED'
      application.p = 0
    }
  }
  g.used = []
  if (turn % semesterTurns === 0) {
    g.view.vacation = { season: turn === semesterTurns ? 'SUMMER' : 'WINTER', campTurn: 0 }
    g.view.remainingAp = 0
  } else Object.assign(g.view, { turn: turn + 1, remainingAp: 4 })
  const entry = log(g, before, 'TURN_ADVANCE', 'TURN_ADVANCE', 'DONE')
  entry.assessments = assessments
  entry.stressLoad = stressLoad
  if (setbacks.length) entry.setbacks = setbacks
  entry.semesterFocus = before.semesterFocus
  entry.narsha = narsha
  entry.vacationOpening = g.view.vacation
  const result = receipt(g, before, 'TURN_ADVANCE', 'DONE', { assessments, setbacks, stressLoad })
  if (routine) entry.routine = { ...routine, statDelta: result.statDelta }
  if (field) entry.fieldLearning = field.experience
  return result
}

export function fieldLearningRoute(g: DemoGame): FieldLearningRoute | null {
  if (g.fieldLearning) return g.fieldLearning.route
  const choice = g.history.find((entry) => entry.kind === 'EVENT_CHOICE' && entry.subjectCode === 'J01'
    && entry.resultCode === 'FIELD_LEARNING_RECORDED')?.choiceCode
  return isFieldLearningRoute(choice) ? choice : null
}

export function friendRelationship(g: DemoGame) {
  const milestones = g.history.filter((item) => item.routine?.friendContact || item.subjectCode === 'SOCIAL'
    || item.choiceCode === 'ROOMMATE' || item.choiceCode === 'PAIR_DEBUG').length
  return { npc: '민서', rapport: Math.min(5, milestones), milestones }
}

export function qualifications(g: DemoGame) {
  return g.enrollments.filter((item) => item.status === 'PASSED').map(({ courseCode }) => ({ courseCode, title: '정보처리기능사' }))
}

function technical(stats: Stats, role: string) {
  switch (role) {
    case 'FE': return stats.F
    case 'BE': return stats.B
    case 'AI': return stats.I
    case 'MOBILE': return Math.max(stats.F, stats.B)
    case 'GAME': return Math.round(.65 * stats.P + .35 * stats.F)
    default: return Math.max(stats.F, stats.B, stats.I)
  }
}

function portfolio(g: DemoGame, role: string) {
  return Math.max(0, ...g.projects.filter((project) => project.inPortfolio).map((project) => {
    const { projectCode, quality = 0 } = project
    const related = (role === 'MOBILE' && ['PR_APP', 'PR_ANDROID', 'PR_IOS', 'PR_CROSS'].includes(projectCode))
      || (role === 'GAME' && projectCode === 'PR_GAME')
    return role === 'SW' || projects[projectCode].role === role || related ? quality ?? 0 : Math.floor(.75 * (quality ?? 0))
  }))
}

function jobTurn(g: DemoGame, turn: number) {
  schoolOnly(g)
  if (g.view.year !== 3 || g.view.turn !== turn) block('현재 채용 단계에서 실행할 수 없습니다.', 'WRONG_STEP')
  if (g.view.remainingAp < 1) block('남은 AP가 부족합니다.', 'INSUFFICIENT_AP')
}

function apply(g: DemoGame, companyCode: string) {
  jobTurn(g, applicationTurn)
  const company = companies[companyCode]
  if (!company) throw new DemoError(404, 'NOT_FOUND', '기업을 찾을 수 없습니다.')
  if (g.applications.length >= 2 || g.applications.some((item) => item.companyCode === companyCode)) return block('최대 두 곳에 한 번씩 지원할 수 있습니다.')
  const before = structuredClone(g.view)
  const stats = g.view.stats
  const T = technical(stats, company.role)
  const Q = portfolio(g, company.role)
  const certificate = ['CO_SW', 'CO_OPS'].includes(companyCode) && qualifications(g).length ? 4 : 0
  const D = cap(.3 * T + .15 * stats.P + .35 * Q + .15 * g.view.resumeScore + .05 * stats.A + certificate)
  const failureCode = T < company.cut[0] ? 'DOC_TECH' : Q < company.cut[1] ? 'DOC_PORTFOLIO' : D < company.cut[2] ? 'DOC_SCORE' : null
  const application: DemoApplication = { portfolioScore: Q, id: randomUUID(), companyCode, role: company.role, status: failureCode ? 'REJECTED' : 'DOC_PASSED', D, failureCode, p: failureCode ? 0 : null }
  g.applications.push(application)
  g.view.remainingAp--
  log(g, before, 'APPLICATION', companyCode, application.status)
  return { application, change: receipt(g, before, 'APPLY', application.status) }
}

function demoDraw(companyCode: string, version: string) {
  const seed = '00000000000000000000000000000059'
  for (let counter = 0; ; counter++) {
    const number = createHash('sha256').update(`${seed}|${companyCode}|${version}|${counter}`).digest().readUInt32BE()
    if (number < 4_294_960_000) return number % 10_000
  }
}

export function employmentInterview(g: DemoGame, id: string) {
  const application = g.applications.find((item) => item.id === id)
  if (!application) throw new DemoError(404, 'NOT_FOUND', '지원 기록을 찾을 수 없습니다.')
  if (application.status !== 'CODE_PASSED') return block('현재 이 회사 면접을 진행할 수 없습니다.', 'WRONG_STEP')
  const company = companies[application.companyCode]
  return companyQuestion(id, company.title, company.role)
}

function jobStep(g: DemoGame, id: string, step: string, body: Record<string, unknown>) {
  const application = g.applications.find((item) => item.id === id)
  if (!application) throw new DemoError(404, 'NOT_FOUND', '지원 기록을 찾을 수 없습니다.')
  const before = structuredClone(g.view)
  const { stats, resumeScore } = g.view
  const { cut, role } = companies[application.companyCode]
  const tech = technical(stats, role)
  const penalty = stats.H < 40 || stats.S >= 65 ? 8 : 0
  if (step === 'CODING' && application.status === 'DOC_PASSED') {
    jobTurn(g, codingTurn)
    if (fatigue(g)) return block('휴식 또는 회복 키트로 컨디션을 회복해 주세요.')
    application.K = cap(.65 * stats.P + .35 * tech + (g.prep.coding ? 5 : 0) - penalty)
    application.status = application.K >= cut[3] ? 'CODE_PASSED' : 'REJECTED'
    if (application.status === 'REJECTED') application.failureCode = 'CODING_CUT'
    applyStats(g, { H: -3, S: 4 })
  } else if (step === 'INTERVIEW' && application.status === 'CODE_PASSED') {
    jobTurn(g, interviewTurn)
    if (fatigue(g)) return block('휴식 또는 회복 키트로 컨디션을 회복해 주세요.')
    const quality = application.portfolioScore
    const technicalScore = .45 * tech + .25 * stats.C + .30 * quality
    const fitScore = .65 * stats.C + .20 * resumeScore + .15 * stats.A
    const preparation = cap(.65 * technicalScore + .35 * fitScore + (g.prep.interview ? 5 : 0) - penalty)
    if ('questionId' in body || 'answer' in body) {
      const answer = interviewAnswer(employmentInterview(g, id), body)
      application.interviewEvaluation = gradeInterview(answer, role)
    }
    application.J = application.interviewEvaluation ? .65 * application.interviewEvaluation.score + .35 * preparation : preparation
    application.status = application.J >= cut[4] ? 'INTERVIEW_PASSED' : 'REJECTED'
    if (application.status === 'REJECTED') application.failureCode = 'INTERVIEW_CUT'
    applyStats(g, { H: -3, S: 4 })
  } else if (step === 'RESULT' && application.status === 'INTERVIEW_PASSED') {
    jobTurn(g, resultTurn)
    const score = .3 * (application.D ?? 0) + .4 * (application.K ?? 0) + .3 * (application.J ?? 0)
    application.p = Math.max(15, Math.min(90, 55 + 1.2 * (score - cut[5])))
    const sample = demoDraw(application.companyCode, g.view.rulesVersion)
    application.status = sample < Math.floor(application.p * 100) ? 'OFFERED' : 'REJECTED'
    if (application.status === 'REJECTED') application.failureCode = 'RESULT_CUT'
  } else return block('아직 이 채용 단계를 진행할 수 없습니다.', 'WRONG_STEP')
  if (application.status === 'REJECTED' && application.p == null) application.p = 0
  g.view.remainingAp--
  log(g, before, 'JOB_STEP', application.companyCode, application.status, step)
  return { application, change: receipt(g, before, step, application.status) }
}

function graduate(g: DemoGame, requested: string, applicationId?: string) {
  if (g.view.phase !== 'GRAD_READY') return block('재학 일정을 끝낸 뒤 졸업할 수 있습니다.', 'WRONG_PHASE')
  const before = structuredClone(g.view)
  const offers = g.applications.filter((item) => item.status === 'OFFERED')
  const canStudy = g.prep.study >= 2 && g.view.stats.A >= 35
  const canIndie = g.prep.indie >= 1 && g.projects.some((project) => project.status === 'COMPLETE')
  if (requested === 'AUTO' && offers.length) return block('받은 제안이 있어 진로를 직접 선택해야 합니다.', 'FINAL_PATH_REQUIRED')
  const path = requested === 'AUTO' ? canStudy ? 'STUDY_NEXT' : canIndie ? 'INDIE' : 'RETRY_JOB' : requested
  const accepted = path === 'ACCEPT_OFFER' ? offers.find((item) => item.id === applicationId) : undefined
  if (path === 'ACCEPT_OFFER' && !accepted) return block('수락할 수 있는 제안을 골라 주세요.')
  if (path === 'STUDY_NEXT' && !canStudy) return block('두 턴의 진학 준비와 학업 35점이 필요합니다.')
  if (path === 'INDIE' && !canIndie) return block('개인 개발 준비 1회와 완성 프로젝트가 필요합니다.')
  if (!['ACCEPT_OFFER', 'STUDY_NEXT', 'INDIE', 'RETRY_JOB'].includes(path)) throw new DemoError(400, 'INVALID_TARGET', '졸업 후 진로를 골라 주세요.')
  for (const offer of offers) offer.status = offer.id === accepted?.id ? 'ACCEPTED' : 'DECLINED'
  const ability = accepted ? technical(g.view.stats, accepted.role) : 0
  const mismatch = accepted && (ability < 48 || (accepted.role !== 'SW' && Math.max(g.view.stats.F, g.view.stats.B, g.view.stats.I) - ability >= 15))
  const initialEndingCode = accepted ? mismatch ? 'JOB_MISMATCH' : `JOB_${accepted.role}` :
    path === 'STUDY_NEXT' ? 'FURTHER_STUDY' : path === 'INDIE' ? 'INDIE_PREP' : 'JOB_RETRY'
  const { endingCode, bodyPrefix, roleEndingCode } = fieldLearningEnding(g.fieldLearning, initialEndingCode, Boolean(accepted))
  const title = accepted ? '첫 출근을 준비합니다' : path === 'STUDY_NEXT' ? '새로운 공부를 이어 갑니다' :
    path === 'INDIE' ? '내 프로젝트를 이어 갑니다' : '다음 지원을 준비합니다'
  const body = accepted ? '학교에서의 경험을 바탕으로 새로운 팀에서 일하게 됐습니다.' : '학교에서 쌓은 기록을 돌아보고 다음 걸음을 준비합니다.'
  g.view.phase = 'ENDED'
  g.ending = { endingCode, title, body: bodyPrefix + body, selectedPath: path,
    acceptedCompany: accepted?.companyCode, acceptedRole: accepted?.role,
    summary: { initialCareerCode: g.view.initialCareerCode ?? undefined, currentCareerCode: g.view.currentCareerCode ?? undefined,
      major: g.view.major ?? undefined, track: g.view.track ?? undefined, qualifications: qualifications(g),
      projects: g.projects.filter((item) => item.status === 'COMPLETE'), finalStats: g.view.stats,
      employed: Boolean(accepted), acceptedCompany: accepted?.companyCode, acceptedRole: accepted?.role,
      nextPath: path, applicationResults: g.applications, fieldLearningRoute: fieldLearningRoute(g),
      fieldLearning: fieldLearningSummary(g.fieldLearning, fieldLearningRoute(g)), ...(g.narsha ? { narsha: g.narsha, narshaProjects: [...(g.narshaProjects ?? []), g.narsha] } : {}),
      ...(roleEndingCode ? { roleEndingCode } : {}) } }
  log(g, before, 'GRADUATION', path, endingCode)
  return { player: g.view, ending: g.ending }
}

export function execute(g: DemoGame, path: string, body: Record<string, unknown>) {
  const event = path.match(/^\/events\/([^/]+)\/choices$/)
  const item = path.match(/^\/inventory\/([^/]+)\/use$/)
  const step = path.match(/^\/applications\/([^/]+)\/steps$/)
  if (event && Object.hasOwn(body, 'choiceCodes')) return chooseActivities(g, event[1], body.choiceCodes)
  if (event) return event[1] === 'AD05' && body.choiceCode === 'ANSWER'
    ? answerAdmission(g, body) : chooseEvent(g, event[1], String(body.choiceCode ?? ''), body.customTopic)
  if (item) return useItem(g, item[1])
  if (step) return jobStep(g, step[1], String(body.step ?? ''), body)
  switch (path) {
    case '/career-preference': return pickCareer(g, String(body.roleCode ?? ''))
    case '/turns/advance': return advance(g, body.mode === 'AUTO')
    case '/actions': return useAction(g, String(body.actionCode ?? ''), typeof body.target === 'string' ? body.target : undefined)
    case '/applications': return apply(g, String(body.companyCode ?? ''))
    case '/graduation': return graduate(g, String(body.path ?? ''), typeof body.applicationId === 'string' ? body.applicationId : undefined)
    default: throw new DemoError(404, 'NOT_FOUND', '요청을 찾을 수 없습니다.')
  }
}
