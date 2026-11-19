export type AdmissionTrack = 'GENERAL' | 'MEISTER' | 'LOCAL' | 'SOCIAL'
export type Phase = 'ADMISSION_1' | 'ADMISSION_2' | 'BEFORE_SCHOOL' | 'CAREER_PICK' | 'Y1' | 'Y2' | 'Y3' | 'SCHOOL' | 'GRAD_READY' | 'ENDED'
export type Stat = 'P' | 'F' | 'B' | 'I' | 'EN' | 'A' | 'G' | 'C' | 'H' | 'S'
export type Stats = Record<Stat, number>
export interface SemesterFocusView { code: string; label: string; semester: number }
export interface VacationView { season: 'SUMMER' | 'WINTER'; campTurn: number }
export interface PlayerView {
  id: string
  name: string
  rulesVersion: string
  demoScenario?: 'COUNSELING'
  endReason?: 'WITHDRAWAL'
  phase: Phase
  year: number
  turn: number
  semesterFocus?: SemesterFocusView
  vacation?: VacationView
  remainingAp: number
  version: number
  stats: Stats
  admissionEligibility?: AdmissionTrack[]
  admissionTrack?: string | null
  beforeSchoolDay?: number | null
  initialCareerCode?: string | null
  currentCareerCode?: string | null
  major?: string | null
  track?: string | null
  resumeScore: number
}

export const CAMPUS_FINISH_CHOICE = 'FINISH_EXPLORATION'
export interface ChoiceView { code: string; label: string; description?: string; apCost: number; enabled: boolean; visited?: boolean; reason?: string | null }
export interface SchoolLifeSource { title: string; url: string; year?: number; fact: string }
export interface SchoolLifeCatalogEntry extends SchoolLifeSource { code: string }
export interface EventView { code: string; title: string; situation: string; required: boolean; blocksAuto?: boolean; selectionCount?: number; choices: ChoiceView[]; source?: SchoolLifeSource | null }
export interface AvailableAction { code: string; target?: string | null; cost: number; enabled: boolean; reason?: string | null }
export interface ItemStack { itemCode: string; name: string; quantity: number; enabled: boolean; reason?: string | null }
export interface CatalogEntry { code: string; title: string; requirements?: unknown; details?: unknown }
export interface CalendarPeriod { year: number; turn: number; month: number; semester: number; theme: string }

export interface MutationResult {
  player: PlayerView
  actionCode: string
  spentAp: number
  statDelta: Partial<Stats>
  resultCode: string
  details?: Record<string, unknown>
}

export interface Recommendation {
  rank: number
  roleCode: string
  label: string
  score: number
  statReasons: { statCode: Stat; value: number; contribution: number }[]
  activityReasons: { day: number; eventCode: string; choiceCode: string; activityLabel: string; tag: string; contribution: number }[]
}
export interface CareerPreferenceView {
  initialCareerCode?: string | null
  currentCareerCode?: string | null
  recommendations: Recommendation[]
  canChange: boolean
  eligibleChangeRoleCodes: string[]
}
export interface CareerPreferenceMutation { player: PlayerView; preference: CareerPreferenceView }

export interface Enrollment { id: string; courseCode: string; status: string; attended?: number; attendedTurns?: number[]; score?: number }
export interface Project { id: string; projectCode: string; status: string; workCount?: number; quality?: number | null; inPortfolio?: boolean }
export interface Qualification { courseCode: string; title?: string }
export interface Snapshot { phase: string; year?: number; turn?: number; beforeSchoolDay?: number; stats: Stats }
export type FieldLearningRoute = 'DOMESTIC' | 'SILICON_VALLEY'
export interface FieldLearningExperience {
  route: FieldLearningRoute
  status: 'INTEREST_RECORDED' | 'SELECTED' | 'NOT_SELECTED' | 'PREPARED' | 'COMPLETED'
  selectionScore?: number | null
  requiredScore?: number | null
  preparationTurn?: number
  participationTurn?: number
  completedYear?: number
  completedTurn?: number
}

export interface NarshaView {
  schoolYear?: number
  topicCode: string
  topicLabel: string
  status: 'PLANNING' | 'YEAR_ONE_COMPLETE' | 'MENTORED' | 'PRESENTED'
  mentorChoice?: string | null
  presentationChoice?: string | null
  quality?: number | null
  yearOneWorkTurns?: number
  yearTwoWorkTurns?: number
  rank?: number
  softwaveStatus?: 'ATTENDED' | 'DECLINED'
}

export interface ProgressView {
  player: PlayerView
  enrollments: Enrollment[]
  projects: Project[]
  qualifications: (Qualification | string)[]
  fieldLearningRoute?: FieldLearningRoute | null
  fieldLearning?: FieldLearningExperience | null
  narsha?: NarshaView | null
  narshaProjects?: NarshaView[]
  friendRelationship?: { npc: string; rapport: number; milestones: number }
  yearSummaries: unknown[]
  snapshots: Snapshot[]
}
export interface HistoryEntry {
  logId: number
  year: number
  turn: number
  beforeSchoolDay?: number | null
  kind: string
  subjectCode: string
  choiceCode?: string
  resultCode: string
  stressLoad?: StressLoad | null
  setbacks?: StatSetback[] | null
  assessments?: AssessmentResult[]
  admission?: AdmissionSummary
}
export interface HistoryPage { items: HistoryEntry[]; nextAfterId?: number | null }

export interface StressLoad {
  stressBefore: number
  stressAfter: number
  total: number
  sources: { code: string; reason: string; delta: number }[]
}

export interface StatSetback {
  reasonCode: 'OVERWORK' | 'SKILL_RUST'
  reason: string
  statDelta: Partial<Stats>
}

export interface StoryEntry {
  id: string
  year: number
  turn: number
  beforeSchoolDay?: number | null
  title: string
  text: string
  dialogue: { speaker: string; text: string }[]
  source: 'AI' | 'MOCK' | 'FALLBACK'
  status: 'PENDING' | 'READY'
  tone?: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL' | null
  stressLoad?: StressLoad | null
  setbacks?: StatSetback[] | null
  assessments?: AssessmentResult[] | null
}
export interface AssessmentResult {
  kind: 'MIDTERM' | 'PERFORMANCE' | 'FINAL'
  title: string
  semester: number
  score: number
  gradeBefore: number
  gradeAfter: number
}
export interface StoryView { entries: StoryEntry[] }

export type JobStep = 'CODING' | 'INTERVIEW' | 'RESULT'
export interface InterviewQuestion { id: string; question: string; source: 'AI' | 'MOCK' | 'FALLBACK'; kind: 'SCHOOL' | 'COMPANY' }
export interface InterviewEvaluation { score: number; criteria: { label: string; score: number }[] }
export interface ApplicationView {
  id: string
  companyCode: string
  role: string
  status: string
  D?: number | null
  K?: number | null
  J?: number | null
  p?: number | null
  failureCode?: string | null
  interviewEvaluation?: InterviewEvaluation | null
}
export interface ApplicationMutation { application: ApplicationView; change: MutationResult }
export interface GraduationSummary {
  schoolStatus?: 'WITHDRAWN'
  withdrawalYear?: number
  withdrawalTurn?: number
  initialCareerCode?: string
  currentCareerCode?: string
  careerChangeTurn?: unknown
  major?: string
  track?: string
  qualifications?: (Qualification | string)[]
  projects?: Project[]
  finalStats?: Stats
  fieldLearningRoute?: FieldLearningRoute | null
  fieldLearning?: FieldLearningExperience | null
  narsha?: NarshaView | null
  narshaProjects?: NarshaView[]
  employed?: boolean
  acceptedCompany?: string
  acceptedRole?: string
  nextPath?: string
  applicationResults?: ApplicationView[]
}
export interface AdmissionSummary {
  passed?: boolean
  minimumScore?: number
  protection?: AdmissionProtection
  admissionTrack?: string
  stage: number
  score: number
  rank: number
  reason?: string
  components?: number[]
  interviewEvaluation?: InterviewEvaluation
}
export interface AdmissionProtection {
  specialTrack: AdmissionTrack
  score: number
  rank: number
  minimumScore?: number
}
export interface EndingView {
  endingCode: string
  title: string
  body: string
  selectedPath?: string | null
  acceptedCompany?: string | null
  acceptedRole?: string | null
  summary: GraduationSummary | AdmissionSummary
}
export interface EndingMutation { player: PlayerView; ending: EndingView }

export type CommandResult = PlayerView | MutationResult | CareerPreferenceMutation | ApplicationMutation | EndingMutation
export interface ApiProblem { code: string; message: string; details?: unknown }
export type Envelope<T> = { success: true; data: T; error: null } | { success: false; data: null; error: ApiProblem }

export function isSchool(player: PlayerView) {
  return player.phase === 'SCHOOL' || player.phase === 'Y1' || player.phase === 'Y2' || player.phase === 'Y3'
}

export function playerPath(id: string) {
  return `/players/${encodeURIComponent(id)}`
}
