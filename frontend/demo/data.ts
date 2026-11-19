import { fieldLearningRoutes } from '../src/game/field-learning.ts'
import { CAMPUS_FINISH_CHOICE, type AdmissionTrack, type CalendarPeriod, type SchoolLifeSource, type Stat, type Stats } from '../src/api/contracts.ts'
import { schoolSources } from './school-life.ts'

export const admissionTracks = {
  GENERAL: { title: '일반전형', eligibilityChance: 100, weights: [40, 35, 25] },
  MEISTER: { title: '마이스터인재', eligibilityChance: 70, weights: [25, 25, 50] },
  LOCAL: { title: '지역우선', eligibilityChance: 20, weights: [30, 45, 25] },
  SOCIAL: { title: '사회통합', eligibilityChance: 50, weights: [45, 30, 25] },
} satisfies Record<AdmissionTrack, { title: string; eligibilityChance: number; weights: number[] }>

export interface DemoChoice {
  code: string
  label: string
  delta?: Partial<Stats>
  tag?: string
  cost?: number
  description?: string
}

interface DemoEvent {
  code: string
  title: string
  situation: string
  required: boolean
  choices: DemoChoice[]
  source?: SchoolLifeSource
}

function event(code: string, title: string, situation: string, choices: DemoChoice[], required = true, source?: SchoolLifeSource): DemoEvent {
  return { code, title, situation, required, choices, source }
}

const choice = (code: string, label: string, delta?: Partial<Stats>, tag?: string): DemoChoice => ({ code, label, delta, tag })

export const admission = [
  event('AD00', '지원 전형 선택', '일반전형은 항상 열립니다. 특별전형은 이번 회차의 게임 내 지원 자격을 각각 추첨합니다.', Object.entries(admissionTracks).map(([code, track]) => choice(code, track.title))),
  event('AD01', '중학교 활동 기록', '지원서의 첫 문단에 어떤 활동을 담을까요?', [
    choice('BUILD', '코딩 활동을 앞세우기', { P: 6, A: 2 }),
    choice('BALANCE', '교과와 팀 활동 강조', { P: 2, A: 6, C: 2 }),
  ]),
  event('AD02', '자기소개서 첫 문장', '선생님께 초안을 보여 드릴 시간이 조금 남았습니다.', [
    choice('REWRITE', '피드백을 받아 다시 쓰기'), choice('SUBMIT', '초안 제출'),
  ]),
  event('AD03', '생활기록부 검토', '프로젝트에서 맡았던 일을 지원서에 담을 수 있습니다.', [
    choice('ADD_EVIDENCE', '프로젝트 역할과 증거 정리'), choice('KEEP', '간단한 기록 제출'),
  ]),
  event('AD04', '코딩 실습 준비', '시험 전 한 언어에 더 시간을 쓸 수 있습니다.', [
    choice('SCRATCH', '스크래치 연습'), choice('PYTHON', '파이썬 연습'),
  ]),
  event('AD05', '마지막 준비', '면접과 기초능력 시험을 앞두고 있습니다.', [
    choice('MOCK', '모의 면접'), choice('LOGIC', '기초능력 문제 풀기'),
  ]),
]

export const beforeSchool = [
  event('BS01', '학교 탐색', '정문을 지나 학교 안내도를 펼칩니다. 일곱 곳 중 둘러볼 두 곳을 고르세요.', [
    { ...choice('LAB', '실습동 둘러보기', { P: 2, B: 1 }, 'CODE'), description: '복도에서 유리 벽 너머 프로그래밍 실습실을 살펴봅니다.' },
    { ...choice('STUDIO', '프로젝트실 둘러보기', { F: 2, C: 1 }, 'DESIGN'), description: '컴퓨터가 여러 대 놓인 자리에서 선배들의 프로젝트 화면을 살펴봅니다.' },
    { ...choice('MAKER_ROOM', '메이커실 둘러보기', { P: 2, A: 1 }, 'CODE'), description: '3D 프린터와 출력물을 보며 아이디어를 모형으로 만드는 과정을 살펴봅니다.' },
    { ...choice('CLASSROOM', '교실 둘러보기', { A: 2, C: 1 }, 'TEAM'), description: '교실을 찾아 첫 수업에 필요한 준비물을 정리합니다.' },
    { ...choice('CAMPUS', '기숙사동 둘러보기', { A: 2, H: 3 }, 'TEAM'), description: '기숙사동을 둘러보며 생활 공간과 공동으로 이용하는 시설을 확인합니다.' },
    { ...choice('AUDITORIUM', '강당 둘러보기', { C: 2, S: -2 }, 'TEAM'), description: '강당 무대 앞에서 안내 행사와 발표가 열리는 공간을 살펴봅니다.' },
    { ...choice('FOOTBALL_FIELD', '축구장 둘러보기', { H: 4, S: -2 }), description: '축구장 가장자리를 걸으며 잠시 바깥 공기를 쐽니다.' },
    choice(CAMPUS_FINISH_CHOICE, '탐색 마치기'),
  ]),
  event('BS02', '전공 체험', '관심 있는 실습 두 가지를 골라 짧게 체험합니다.', [
    { ...choice('WEB_STUDIO', '행사 안내 웹 만들기', { F: 4, P: 1 }, 'WEB'), description: '학교 행사 시간표와 안내를 한 화면에 담습니다.' },
    { ...choice('APP_STUDIO', '앱 화면 연결', { P: 3, F: 2 }, 'MOBILE'), description: '시간표 목록과 상세 화면을 연결합니다.' },
    { ...choice('DATA_STUDIO', '분류 데이터 체험', { I: 4, A: 1 }, 'AI'), description: '분실물 사진에 라벨을 붙이고 분류 결과를 비교합니다.' },
    { ...choice('API_STUDIO', '급식 메뉴 API 만들기', { B: 4, P: 1 }, 'SERVER'), description: '날짜를 보내면 그날의 메뉴를 돌려주는 서버를 만듭니다.' },
    { ...choice('GAME_STUDIO', '미니게임 조작 만들기', { P: 4, F: 1 }, 'GAME'), description: '키를 눌러 캐릭터를 움직이고 장애물과의 충돌을 처리합니다.' },
    { ...choice('NETWORK_STUDIO', '실습실 연결 점검', { B: 3, A: 2 }, 'NETWORK'), description: '두 컴퓨터가 연결됐는지 확인하고 연결이 끊긴 지점을 찾습니다.' },
    { ...choice('SECURITY_STUDIO', '게시판 접근 권한 점검', { B: 2, A: 3 }, 'SECURITY'), description: '일반 사용자와 관리자가 볼 수 있는 화면을 비교합니다.' },
    { ...choice('UX_STUDIO', '도서 검색 화면 개선', { F: 3, C: 2 }, 'DESIGN'), description: '친구에게 검색을 부탁하고 막히는 곳의 버튼과 문구를 고칩니다.' },
  ]),
  event('BS03', '기초 프로그래밍', '어떤 실습에 참여할까요?', [
    choice('GAME_LOOP', '게임 루프 만들기', { P: 4, H: -3 }, 'GAME'),
    choice('ANDROID_INPUT', '안드로이드 입력 검증', { P: 3, B: 1, H: -3 }, 'ANDROID'),
    choice('IOS_LAYOUT', 'iOS 화면 배치', { F: 3, P: 1, H: -3 }, 'IOS'),
    { ...choice('PYTHON_SCORE', '파이썬으로 경기 점수 집계', { P: 3, A: 1, H: -3 }, 'CODE'), description: '반복문으로 여러 경기의 점수를 더하고 순위를 구합니다.' },
    { ...choice('HTML_NOTICE', 'HTML로 교내 공지 정리', { F: 3, P: 1, H: -3 }, 'WEB'), description: '제목과 링크를 붙여 공지 페이지를 만듭니다.' },
    { ...choice('SQL_BOOK', 'SQL로 도서 찾기', { B: 3, A: 1, H: -3 }, 'SERVER'), description: '제목과 대출 상태를 기준으로 도서 목록을 검색합니다.' },
  ]),
  event('BS04', '팀 활동', '새 친구들과 한 가지 문제를 해결합니다.', [
    choice('SERVICE_FIX', 'API 오류 해결', { B: 4, C: 2 }, 'SERVER'),
    choice('NETWORK_MAP', '교내 연결 지도 만들기', { B: 3, A: 1, C: 2 }, 'NETWORK'),
    choice('ACCESS_AUDIT', '권한 문제 찾기', { B: 2, A: 2, C: 2 }, 'SECURITY'),
    { ...choice('TEAM_GAME', '둘이서 미니게임 테스트', { P: 3, C: 3 }, 'GAME'), description: '친구와 버그를 찾아 재현 순서를 기록합니다.' },
    { ...choice('TEAM_MAKER', '3D 출력물 개선', { P: 2, A: 1, C: 3 }, 'TEAM'), description: '친구와 출력물의 크기와 맞물리는 부분을 점검합니다.' },
    { ...choice('TEAM_AI', 'AI 분류 결과 교차 확인', { I: 3, C: 3 }, 'AI'), description: '서로 다르게 분류한 사진을 비교해 판단 기준을 맞춥니다.' },
  ]),
  event('BS05', '진로 상담', '지난 체험을 돌아보며 관심사를 더 구체적으로 알아봅니다.', [
    choice('UI_CASE', '화면 사용성 설명', { F: 2, C: 2, S: -3 }, 'DESIGN'),
    choice('MODEL_CASE', '모델 활용 사례 조사', { I: 2, A: 2, S: -3 }, 'AI'),
    choice('TEAM_CASE', '팀 활동 발표', { C: 3, P: 1, S: -3 }, 'TEAM'),
    { ...choice('MOBILE_CASE', '만들고 싶은 앱 상담', { P: 2, F: 1, C: 1, S: -3 }, 'MOBILE'), description: '관심 있는 앱을 하나 정하고 필요한 화면과 기능을 적어 봅니다.' },
    { ...choice('SERVER_CASE', '서버 개발 학습 순서 상담', { B: 2, A: 1, C: 1, S: -3 }, 'SERVER'), description: '기록을 보관하고 화면에 데이터를 보내는 과정을 어떤 순서로 익힐지 정리합니다.' },
    { ...choice('SECURITY_CASE', '보안 학습 계획 상담', { B: 1, A: 2, C: 1, S: -3 }, 'SECURITY'), description: '계정과 접근 권한을 공부하고 안전하게 실습할 방법을 정리합니다.' },
  ]),
]

export const majorEvent = event('Y201', '2학년 학과 선택', "자료에서 안내한 인공지능소프트웨어과와 소프트웨어개발과를 살펴봅니다. 희망 직무와 관계없이 학과를 선택하며, 이후 다른 분야도 프로젝트와 방과후로 배울 수 있습니다.", [
  choice('AI', '인공지능소프트웨어과'), choice('SW', '소프트웨어개발과'),
], true, schoolSources.CURRICULUM)
export const trackEvent = event('Y301', '3학년 수업 선택', "자료의 3학년 교육과정에는 화면 구현·서버 운영·응용 개발·CS 심화가 소개됩니다. 게임에서는 프론트엔드 또는 백엔드를 집중 수업으로 고릅니다. 실제 학교가 두 수업으로만 나뉜다는 뜻은 아닙니다.", [
  choice('FE', '프론트엔드'), choice('BE', '백엔드'),
], true, schoolSources.CURRICULUM)
export const dormEvent = event('E11', '기숙사 첫날', '룸메이트와 생활 규칙을 정하거나 먼저 짐을 풀 수 있습니다.', [
  { ...choice('ROOMMATE', '룸메이트와 생활 규칙 정하기', { C: 4 }), cost: 1 },
  choice('UNPACK', '짐을 풀고 쉬기', { H: 4, S: -3 }),
], false, schoolSources.DORM)
export const pairDebugEvent = event('S101', '옆자리 친구가 손을 들었다', '코드가 멈춘 순간, 민서가 화면을 돌아본다. 지금 함께 원인을 찾을지 정할 수 있다.', [
  { ...choice('PAIR_DEBUG', '민서와 함께 오류 찾기', { P: 3, C: 2 }), cost: 1 },
  { ...choice('CHECK_ALONE', '먼저 혼자 원인 정리하기', { A: 2 }), cost: 1 },
  choice('AFTER_CLASS', '수업이 끝나고 다시 보기'),
], false)
export const clubEvent = event('S103', '동아리 첫 모임', '방과후에 둘러본 두 모임 중 한 곳에서 첫 활동을 해 볼 수 있습니다.', [
  { ...choice('MAKER_CLUB', '제작 동아리에서 모형 만들기', { P: 4, A: 2 }), description: '동아리에서 작은 모형을 만들어 시연했다.' },
  { ...choice('MEDIA_CLUB', '미디어 동아리에서 학교 소식 정리하기', { F: 4, C: 2 }), description: '동아리 친구와 학교 소식을 정리하고 전달할 화면을 만들었다.' },
  { ...choice('KEEP_ROUTINE', '이번에는 기숙사에서 쉬기', { H: 5, S: -4 }), description: '기숙사로 돌아가 짐을 정리하고 쉬었다.' },
], true, schoolSources.CLUB)
export const focusEvent = event('TERM_FOCUS', '이번 학기에 집중할 것',
  '학기 동안 시간을 쓰고 싶은 분야를 고릅니다. 수업을 따라가며 선택한 분야가 매 턴 조금씩 성장합니다.', [
    { ...choice('FOCUS_BALANCE', '생활과 학업의 균형', { H: 3, S: -2 }), description: '체력을 회복하고 스트레스를 관리합니다.' },
    { ...choice('FOCUS_GRADES', '교과 성적 관리', { A: 2 }), description: '학습 역량이 자라고 중간·기말고사와 수행평가를 준비합니다.' },
    { ...choice('FOCUS_CODE', '프로그래밍 기본기', { P: 2 }), description: '코드를 읽고 구현하는 능력을 기릅니다.' },
    { ...choice('FOCUS_CAREER', '희망 직무 연습'), description: '희망 직무와 연결된 기술을 연습합니다.' },
    { ...choice('FOCUS_TEAM', '팀 프로젝트와 협업', { P: 1, C: 2 }), description: '프로그래밍과 협업 역량을 함께 기릅니다.' },
    { ...choice('FOCUS_ENGLISH', '영어와 글로벌 진로', { EN: 3 }), description: '기술 문서를 읽고 작업 내용을 영어로 설명합니다.' },
  ])

export function vacationEvent(season: 'SUMMER' | 'WINTER') {
  return event('VACATION', season === 'SUMMER' ? '여름방학' : '겨울방학', '방학을 어떻게 보낼지 고르세요.', [
    { ...choice('MAJOR_CAMP', '전공강화캠프 참가'), description: '정규 학기 사이에 캠프를 2턴 진행합니다. 전공 기술과 프로그래밍을 연습합니다.' },
    { ...choice('VACATION_REST', '방학 동안 쉬기'), description: '체력을 회복하고 다음 일정으로 넘어갑니다.' },
  ])
}

export const stressSupportEvent = event('STRESS_SUPPORT', '잠시 숨을 고를 시간',
  '학교생활의 부담이 커졌습니다. 이번에는 어떻게 마음을 돌볼까요?', [
    { ...choice('STRESS_REST', '일정을 줄이고 쉬기', { S: -12, H: 6 }), description: '스트레스 -12 · 체력 +6' },
    { ...choice('STRESS_FRIEND', '친구에게 속마음 털어놓기', { S: -10 }), description: '스트레스 -10' },
    { ...choice('STRESS_TEACHER', '선생님과 부담을 나누기', { S: -16 }), description: '스트레스 -16' },
  ])
export const stressRestChoices = [
  stressSupportEvent.choices[0],
  { ...choice('STRESS_WALK', '운동장을 천천히 걷기', { S: -11, H: 4 }), description: '스트레스 -11 · 체력 +4' },
  { ...choice('STRESS_SLEEP', '오늘은 일찍 잠들기', { S: -14, H: 8 }), description: '스트레스 -14 · 체력 +8' },
]
export const stressHelpChoices = [
  ...stressSupportEvent.choices.slice(1),
  { ...choice('STRESS_COUNSELOR', '상담 선생님과 일정 정리하기', { S: -18 }), description: '스트레스 -18' },
  { ...choice('STRESS_MENTOR', '멘토에게 막힌 부분 도움받기', { S: -13 }), description: '스트레스 -13' },
]
export const stressExtraChoices = [
  { ...choice('STRESS_HOBBY', '잠깐 취미에 집중하기', { S: -9 }), description: '스트레스 -9' },
  { ...choice('STRESS_PUSH_ON', '이번 마감까지만 버텨 보기', { S: 5, H: -3 }), description: '스트레스 +5 · 체력 -3' },
  { ...choice('STRESS_PLAN', '할 일을 줄이고 우선순위 정하기', { S: -8 }), description: '스트레스 -8' },
]
export const dailySchoolEvents = [
  event('DAILY_ROOMMATE', '기숙사 복도에서 들은 한마디', '룸메이트가 잠깐 산책을 가자고 합니다. 책상 위에는 아직 과제가 남아 있습니다.', [
    choice('ROOM_WALK', '함께 잠깐 걷고 오기', { S: -5, C: 1 }),
    choice('ROOM_STUDY', '같이 앉아 과제 정리하기', { P: 1, S: 2 }),
    choice('ROOM_REST', '오늘은 먼저 쉬기', { H: 4, S: -4 }),
  ], false),
  event('DAILY_FEEDBACK', '예상하지 못한 코드 리뷰', '선생님이 작성한 코드에 질문을 남겼습니다. 지금 확인할지, 질문부터 정리할지 고릅니다.', [
    choice('REVIEW_ASK', '모르는 부분을 질문하기', { A: 1, S: -3 }),
    choice('REVIEW_RETRY', '혼자 다시 구현해 보기', { P: 2, S: 4 }),
    choice('REVIEW_NOTE', '내일 확인할 질문을 적기', { S: -2 }),
  ], false),
  event('DAILY_FRIEND', '친구가 자리를 내어 주었다', '쉬는 시간에 친구가 옆자리를 가리킵니다. 요즘 어떻게 지내는지 이야기할 수 있습니다.', [
    choice('FRIEND_TALK', '요즘 고민을 나누기', { C: 1, S: -5 }),
    choice('FRIEND_HELP', '친구의 오류를 함께 보기', { P: 1, C: 1, S: 1 }),
    choice('FRIEND_ALONE', '혼자 조용히 쉬기', { S: -4, H: 2 }),
  ], false),
  event('DAILY_AFTER_CLASS', '실습실 불이 아직 켜져 있다', '방과후 실습실에 몇 명이 남아 있습니다. 오늘의 마무리 방법을 고릅니다.', [
    choice('AFTER_BREAK', '짐을 챙기고 기숙사에서 쉬기', { H: 5, S: -5 }),
    choice('AFTER_PRACTICE', '작은 기능 하나를 더 구현하기', { P: 2, S: 4 }),
    choice('AFTER_PLAN', '남은 작업의 우선순위만 정하기', { A: 1, S: -2 }),
  ], false),
]
export const dailyExamEvent = event('DAILY_EXAM', '시험 준비 방법을 바꿔 볼까', '복습할 내용과 제출할 과제가 겹쳤습니다. 오늘의 공부 방법을 고릅니다.', [
  choice('EXAM_GROUP', '친구와 모르는 문제만 비교하기', { A: 1, C: 1, S: -3 }),
  choice('EXAM_CRAM', '오늘 밤 공부 시간을 늘리기', { A: 2, S: 5, H: -3 }),
  choice('EXAM_SPLIT', '범위를 나누고 잠깐 쉬기', { S: -4 }),
], false)
export const dailyNarshaEvent = event('DAILY_NARSHA', '나르샤 회의가 길어졌다', '', [
  choice('NARSHA_PAIR', '막힌 기능을 둘이 나눠 해결하기', { P: 1, C: 1, S: -3 }),
  choice('NARSHA_SCOPE', '필수 기능부터 마무리하기', { S: -5 }),
  choice('NARSHA_EXPAND', '욕심났던 기능도 추가해 보기', { P: 2, S: 5 }),
], false)
export const schoolDoubtsEvent = event('SCHOOL_DOUBTS', '학교를 계속 다닐 수 있을까',
  '스트레스가 90 이상입니다. 학교생활을 이어갈 방법을 의논하거나, 자퇴를 고민해 볼 수 있습니다.', [
    { ...choice('ASK_FOR_SUPPORT', '선생님과 학교생활 조정 방법 찾기', { S: -25 }), description: '스트레스 -25' },
    { ...choice('REST_AND_RECONSIDER', '충분히 쉬고 다시 생각하기', { S: -20, H: 8 }), description: '스트레스 -20 · 체력 +8' },
    { ...choice('CONSIDER_WITHDRAWAL', '자퇴를 고민해 보기'), description: '다음 화면에서 재학을 이어갈지, 자퇴를 확정할지 선택합니다.' },
  ])
export const withdrawalEvent = event('WITHDRAWAL_DECISION', '자퇴를 결정하기 전에',
  '지금까지의 학교생활과 다음 계획을 돌아봅니다. 자퇴를 확정하면 이 회차의 재학 생활이 끝납니다.', [
    { ...choice('STAY_ENROLLED', '도움을 받으며 학교생활 이어가기', { S: -25 }), description: '스트레스 -25' },
    { ...choice('CONFIRM_WITHDRAWAL', '자퇴를 확정하고 다음 길 준비하기'), description: '학교생활을 마치고 현재까지의 기록으로 엔딩을 봅니다.' },
  ])
export const narshaTopics = {
  NARSHA_WEB: { label: '캠퍼스 시설 운영·안전 플랫폼', stat: 'F', delta: { F: 2, P: 1 },
    description: '학생 신고, 시설팀 작업 배정, 역할별 권한, 시설 상태 통계를 연결하고 실사용 테스트를 진행합니다.' },
  NARSHA_APP: { label: '기숙사 생활 통합 시스템', stat: 'B', delta: { B: 2, P: 1 },
    description: '외출 신청, 공용 시설 예약, 사감 승인, 푸시 알림을 앱과 관리자 서버로 연결하고 동시 예약을 검증합니다.' },
  NARSHA_AI: { label: 'AI 영상 개인정보 보호 시스템', stat: 'I', delta: { I: 2, P: 1 },
    description: '영상 속 얼굴을 탐지하고 가립니다. 지연 시간과 탐지 정확도를 비교하고 누락은 사람이 검토할 수 있게 설계합니다.' },
  NARSHA_GAME: { label: '다중 접속 재난 대피 훈련 시뮬레이터', stat: 'P', delta: { P: 2, F: 1 },
    description: '학교 공간을 모델링하고 여러 사람이 동시에 대피하는 상황에서 경로 안내, 진행 동기화, 훈련 결과 분석을 구현합니다.' },
} satisfies Record<string, { label: string; stat: Stat; delta: Partial<Stats>; description: string }>

export const narshaTopicEvent = event('N104', '1학년 나르샤 · 주제 선택', '1학년 팀 프로젝트를 시작합니다. 23턴까지 개발할 주제를 정하고 요구사항, 구현, 검증 기록을 남깁니다.',
  [...Object.entries(narshaTopics).map(([code, topic]) => ({ ...choice(code, topic.label, topic.delta), description: topic.description })),
    { ...choice('NARSHA_CUSTOM', '기타 · 직접 주제 입력'), description: '팀이 개발할 프로젝트 주제를 직접 적습니다.' }], true, schoolSources.NARSHA)

export const yearTwoNarshaEvent: DemoEvent = { ...narshaTopicEvent, code: 'N201', title: '2학년 나르샤 · 주제 선택',
  situation: '2학년 팀이 새 프로젝트를 시작합니다. 1학년 프로젝트를 돌아보고 이번 학년에 22턴까지 개발할 주제를 정합니다.' }

export const teamGoalEvent = event('S202', '나르샤 멘토 피드백', '2학년 나르샤 주제를 정했습니다. 멘토와 함께 22턴 발표까지 개발하고 검증할 내용을 정합니다.', [
  { ...choice('CONTEST_PREP', '교내 공모전 시연 준비하기', { P: 4, C: 3 }), description: '교내 공모전을 목표로 맡은 기능의 시연 순서를 점검했다.' },
  { ...choice('ENGLISH_DEMO', '영어로 프로젝트 소개 연습하기', { EN: 7, C: 2 }), description: '프로젝트에서 맡은 일을 영어로 설명하고 질문에 답하는 연습을 했다.' },
  { ...choice('NARSHA_FIX', '멘토 피드백으로 핵심 기능 다듬기', { P: 3, A: 2 }), description: '멘토가 짚어 준 오류를 재현하고 해결 과정을 팀 노트에 정리했다.' },
], true, schoolSources.NARSHA)
export const narshaExpoEvent = event('N204', 'ICT융합 엑스포 발표', '나르샤 결과물을 시연하고 팀 순위를 확인하는 날입니다. 상위 세 팀은 소프트웨이브 참가를 선택할 수 있습니다.', [
  { ...choice('EXPO_DEMO', '무대에서 프로젝트 시연하기', { C: 4, S: 2 }), description: '무대에서 팀이 만들었던 기능을 실행하며 해결하려던 문제와 결과를 설명했다.' },
  { ...choice('EXPO_BOOTH', '부스에서 방문객에게 설명하기', { C: 3, A: 2 }), description: '부스에서 직접 결과물을 보여 주고 방문객의 질문을 팀과 나눴다.' },
], true, schoolSources.EXPO)
export const softwaveEvent = event('N205', '소프트웨이브 참가 결정',
  '게임 내 나르샤 순위가 3위 이내라 팀 결과물을 전시할 기회가 생겼습니다. 참가할까요?', [
    { ...choice('SOFTWAVE_ATTEND', '소프트웨이브에서 결과물 전시하기', { C: 3, A: 2 }), description: '팀과 함께 시연하고 관람객의 질문에 답합니다.' },
    { ...choice('SOFTWAVE_DECLINE', '팀 결과물을 교내 기록으로 남기기'), description: '교내 발표 영상과 개발 기록을 정리합니다.' },
  ])
export const careerEvent = event('J01', '공고를 함께 읽어 볼까요?', "국내 기업 현장실습과 미국 실리콘밸리 현장학습은 서로 다른 경로입니다. 온라인 사전 프로젝트·영어 준비와 현지 참여를 구분해서 살펴봅니다. 선발과 취업은 별도 결과입니다.", [
  choice('DOMESTIC', fieldLearningRoutes.DOMESTIC.choice),
  choice('SILICON_VALLEY', fieldLearningRoutes.SILICON_VALLEY.choice),
  choice('COMPARE', '공고 기준 보기', { C: 2 }),
  choice('LATER', '나중에 볼게요'),
], false, schoolSources.GLOBAL_SELECTION)

export const calendar: CalendarPeriod[] = [
  { year: 1, turn: 1, month: 3, semester: 1, theme: '첫 교실과 기숙사' },
  { year: 1, turn: 2, month: 3, semester: 1, theme: '수업 준비와 개인 활동' },
  { year: 1, turn: 3, month: 4, semester: 1, theme: '기초 수업과 생활 적응' },
  { year: 1, turn: 4, month: 4, semester: 1, theme: '기초 수업과 생활 적응' },
  { year: 1, turn: 5, month: 4, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 6, month: 4, semester: 1, theme: '중간고사' },
  { year: 1, turn: 7, month: 5, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 8, month: 5, semester: 1, theme: '나르샤 프로젝트 주제 선택' },
  { year: 1, turn: 9, month: 6, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 10, month: 6, semester: 1, theme: '수행평가' },
  { year: 1, turn: 11, month: 7, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 12, month: 7, semester: 1, theme: '기말고사와 여름방학' },
  { year: 1, turn: 13, month: 8, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 14, month: 8, semester: 2, theme: '동아리와 방과후 활동' },
  { year: 1, turn: 15, month: 9, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 16, month: 9, semester: 2, theme: '나르샤 아이디어 모으기' },
  { year: 1, turn: 17, month: 9, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 18, month: 9, semester: 2, theme: '중간고사' },
  { year: 1, turn: 19, month: 10, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 20, month: 10, semester: 2, theme: '나르샤 시제품 개발' },
  { year: 1, turn: 21, month: 11, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 1, turn: 22, month: 11, semester: 2, theme: '수행평가' },
  { year: 1, turn: 23, month: 12, semester: 2, theme: '1학년 나르샤 마무리' },
  { year: 1, turn: 24, month: 12, semester: 2, theme: '기말고사와 겨울방학' },
  { year: 2, turn: 1, month: 3, semester: 1, theme: '학과 선택' },
  { year: 2, turn: 2, month: 3, semester: 1, theme: '나르샤 개발과 멘토링 시작' },
  { year: 2, turn: 3, month: 4, semester: 1, theme: '기초 수업과 생활 적응' },
  { year: 2, turn: 4, month: 4, semester: 1, theme: '전공 수업과 팀 구성' },
  { year: 2, turn: 5, month: 4, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 6, month: 4, semester: 1, theme: '중간고사' },
  { year: 2, turn: 7, month: 5, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 8, month: 5, semester: 1, theme: '나르샤 코드 리뷰' },
  { year: 2, turn: 9, month: 6, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 10, month: 6, semester: 1, theme: '수행평가' },
  { year: 2, turn: 11, month: 7, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 12, month: 7, semester: 1, theme: '기말고사와 여름방학' },
  { year: 2, turn: 13, month: 8, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 14, month: 8, semester: 2, theme: '기업 탐방과 전공면접 캠프' },
  { year: 2, turn: 15, month: 9, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 16, month: 9, semester: 2, theme: '전공 프로젝트 개선' },
  { year: 2, turn: 17, month: 9, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 18, month: 9, semester: 2, theme: '중간고사' },
  { year: 2, turn: 19, month: 10, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 20, month: 10, semester: 2, theme: '나르샤 시연 준비' },
  { year: 2, turn: 21, month: 11, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 2, turn: 22, month: 11, semester: 2, theme: '수행평가와 나르샤 발표·순위' },
  { year: 2, turn: 23, month: 12, semester: 2, theme: '소프트웨이브 참가 결정' },
  { year: 2, turn: 24, month: 12, semester: 2, theme: '기말고사와 겨울방학' },
  { year: 3, turn: 1, month: 3, semester: 1, theme: '실무 수업 선택' },
  { year: 3, turn: 2, month: 3, semester: 1, theme: '수업 준비와 개인 활동' },
  { year: 3, turn: 3, month: 4, semester: 1, theme: '기초 수업과 생활 적응' },
  { year: 3, turn: 4, month: 4, semester: 1, theme: '포트폴리오 작업' },
  { year: 3, turn: 5, month: 4, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 6, month: 4, semester: 1, theme: '중간고사와 이력서' },
  { year: 3, turn: 7, month: 5, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 8, month: 5, semester: 1, theme: '면접 연습' },
  { year: 3, turn: 9, month: 6, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 10, month: 6, semester: 1, theme: '수행평가' },
  { year: 3, turn: 11, month: 7, semester: 1, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 12, month: 7, semester: 1, theme: '기말고사·기업 지원과 여름방학' },
  { year: 3, turn: 13, month: 8, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 14, month: 8, semester: 2, theme: '실무 경험 준비' },
  { year: 3, turn: 15, month: 9, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 16, month: 9, semester: 2, theme: '현장학습과 코딩 테스트' },
  { year: 3, turn: 17, month: 9, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 18, month: 9, semester: 2, theme: '중간고사' },
  { year: 3, turn: 19, month: 10, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 20, month: 10, semester: 2, theme: '회사 면접' },
  { year: 3, turn: 21, month: 11, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 22, month: 11, semester: 2, theme: '수행평가와 결과 준비' },
  { year: 3, turn: 23, month: 12, semester: 2, theme: '정규 수업과 개인 활동' },
  { year: 3, turn: 24, month: 12, semester: 2, theme: '채용 결과·기말고사와 겨울방학' },
]

export const roles: [code: string, label: string, practice: Stat, weights: number[], tags: string[]][] = [
  ['WEB', '웹 개발자', 'F', [20, 45, 15, 0, 5, 15], ['WEB', 'DESIGN']],
  ['BACKEND', '백엔드 개발자', 'B', [25, 0, 50, 5, 10, 10], ['SERVER', 'SECURITY']],
  ['CROSS_PLATFORM', '크로스플랫폼 개발자', 'P', [35, 30, 10, 0, 5, 20], ['MOBILE', 'WEB']],
  ['ANDROID', '안드로이드 개발자', 'P', [45, 10, 25, 0, 10, 10], ['MOBILE', 'ANDROID']],
  ['IOS', 'iOS 개발자', 'F', [35, 25, 10, 0, 10, 20], ['MOBILE', 'IOS']],
  ['GAME', '게임 개발자', 'P', [50, 20, 10, 5, 5, 10], ['GAME', 'CODE']],
  ['NETWORK', '네트워크 개발자', 'B', [20, 0, 55, 0, 15, 10], ['NETWORK', 'SERVER']],
  ['SECURITY', '보안 개발자', 'B', [25, 0, 35, 5, 25, 10], ['SECURITY', 'NETWORK']],
  ['UI_UX', 'UI/UX 개발자', 'F', [10, 45, 0, 0, 10, 35], ['DESIGN', 'WEB']],
  ['AI_DEVELOPER', '인공지능 개발자', 'I', [20, 0, 10, 45, 20, 5], ['AI', 'DATA']],
  ['AI_NATIVE', 'AI 네이티브 개발자', 'I', [25, 0, 15, 40, 10, 10], ['AI', 'CODE']],
]

export const projects: Record<string, { title: string; stat: Stat; target: number; from: number; duration: number; team: boolean; role: string; careers: string[] }> = {
  PR_WEB: { title: '교내 행사 페이지', stat: 'F', target: 24, from: 1, duration: 2, team: false, role: 'FE', careers: ['WEB', 'UI_UX'] },
  PR_API: { title: '기숙사 문의 API', stat: 'B', target: 34, from: 2, duration: 2, team: true, role: 'BE', careers: ['BACKEND'] },
  PR_AI: { title: '분실물 분류 모델', stat: 'I', target: 38, from: 2, duration: 3, team: true, role: 'AI', careers: ['AI_DEVELOPER', 'AI_NATIVE'] },
  PR_CROSS: { title: '공용 할 일 앱', stat: 'P', target: 30, from: 1, duration: 2, team: false, role: 'FE', careers: ['CROSS_PLATFORM'] },
  PR_ANDROID: { title: '안드로이드 체크리스트', stat: 'P', target: 32, from: 1, duration: 2, team: false, role: 'FE', careers: ['ANDROID'] },
  PR_IOS: { title: 'iOS 화면 전환 데모', stat: 'F', target: 30, from: 1, duration: 2, team: false, role: 'FE', careers: ['IOS'] },
  PR_GAME: { title: '입력과 충돌 미니게임', stat: 'P', target: 32, from: 1, duration: 2, team: false, role: 'SW', careers: ['GAME'] },
  PR_NETWORK: { title: '교내 연결 진단 모형', stat: 'B', target: 30, from: 1, duration: 2, team: false, role: 'BE', careers: ['NETWORK'] },
  PR_SECURITY: { title: '권한 점검 실습', stat: 'B', target: 32, from: 1, duration: 2, team: false, role: 'BE', careers: ['SECURITY'] },
}

export const companies: Record<string, { title: string; role: 'FE' | 'SW' | 'AI' | 'BE' | 'MOBILE' | 'GAME'; cut: [number, number, number, number, number, number] }> = {
  CO_WEB: { title: '웹서비스 개발팀', role: 'FE', cut: [38, 42, 44, 38, 38, 49] },
  CO_SW: { title: '소형 소프트웨어 개발사', role: 'SW', cut: [30, 38, 40, 34, 36, 43] },
  CO_AI: { title: 'AI 솔루션 개발팀', role: 'AI', cut: [42, 45, 48, 46, 43, 53] },
  CO_OPS: { title: '사내 시스템 개발팀', role: 'BE', cut: [35, 42, 43, 44, 39, 50] },
  CO_MOBILE: { title: '모바일 앱 개발팀', role: 'MOBILE', cut: [35, 40, 38, 35, 36, 40] },
  CO_GAME: { title: '인디 게임 스튜디오', role: 'GAME', cut: [35, 40, 38, 35, 36, 40] },
}
