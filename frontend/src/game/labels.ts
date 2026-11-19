import type { AdmissionProtection, PlayerView, Stat } from '../api/contracts'
import { fieldLearningRoutes } from './field-learning'

export const stats: Record<Stat, string> = { P: '프로그래밍', F: '프론트엔드', B: '백엔드', I: '인공지능', EN: '영어', A: '학업', G: '성적', C: '의사소통', H: '체력', S: '스트레스' }

const labels: Record<string, string> = {
  GENERAL: '일반전형', MEISTER: '마이스터인재', LOCAL: '지역우선', SOCIAL: '사회통합',
  WEB: '웹 개발자', BACKEND: '백엔드 개발자', CROSS_PLATFORM: '크로스플랫폼 개발자', ANDROID: '안드로이드 개발자',
  IOS: 'iOS 개발자', GAME: '게임 개발자', NETWORK: '네트워크 개발자', SECURITY: '보안 개발자',
  UI_UX: 'UI/UX 개발자', AI_DEVELOPER: '인공지능 개발자', AI_NATIVE: 'AI 네이티브 개발자',
  SW: '소프트웨어개발과', AI: '인공지능소프트웨어과', FE: '프론트엔드', BE: '백엔드',
  STUDY: '개인 공부', CLASS: '전공 수업', ROLE_PRACTICE: '직무 연습', REST: '휴식', SOCIALIZE: '친구와 교류',
  COURSE_ENROLL: '방과후 과정 등록', ATTEND: '방과후 출석', EXAM: '자격 시험',
  PROJECT_START: '프로젝트 시작', PROJECT_WORK: '프로젝트 작업', PORTFOLIO_ADD: '포트폴리오 등록',
  RESUME: '이력서 준비', CODING_PREP: '코딩 테스트 준비', INTERVIEW_PREP: '면접 준비', PATH_PREP: '진로 준비',
  C_INFO: '정보처리기능사', C_OFFICE: '컴퓨터활용능력', C_SQL: 'SQLD', C_LINUX: '리눅스 자격', C_NET: '네트워크 자격',
  PR_WEB: '교내 행사 페이지', PR_API: '기숙사 문의 API', PR_AI: '분실물 분류 모델', PR_APP: '교내 알림 앱',
  PR_CROSS: '공용 할 일 앱', PR_ANDROID: '안드로이드 체크리스트', PR_IOS: 'iOS 화면 전환 데모',
  PR_GAME: '입력과 충돌 미니게임', PR_NETWORK: '교내 연결 진단 모형', PR_SECURITY: '권한 점검 실습',
  CO_WEB: '웹서비스 개발팀', CO_SW: '소형 소프트웨어 개발사', CO_AI: 'AI 솔루션 개발팀', CO_OPS: '사내 시스템 개발팀',
  CO_MOBILE: '모바일 앱 개발팀', CO_GAME: '인디 게임 스튜디오',
  DOC_PASSED: '서류 통과', CODE_PASSED: '코딩 통과', INTERVIEW_PASSED: '면접 통과', OFFERED: '채용 제안',
  ACCEPTED: '제안 수락', DECLINED: '제안 거절', REJECTED: '탈락', EXPIRED: '기간 종료',
  DOC_TECH: '직무 기술 기준 미달', DOC_PORTFOLIO: '포트폴리오 기준 미달', DOC_SCORE: '서류 점수 기준 미달',
  CODING_CUT: '코딩 점수 기준 미달', INTERVIEW_CUT: '면접 점수 기준 미달', RESULT_CUT: '최종 채용 기준 미달', STEP_EXPIRED: '채용 단계 미응시',
  ENROLLED: '수강 중', IN_PROGRESS: '진행 중', COMPLETE: '완성', FAILED: '미완료', PASSED: '합격',
  RETRY_JOB: '다시 취업 준비', STUDY_NEXT: '진학 준비', INDIE: '개인 개발 준비',
  ACCEPT_OFFER: '채용 제안 수락', AUTO: '준비 기록에 따라 정하기',
  AD00: '지원 전형', AD01: '중학교 활동 기록', AD02: '자기소개서', AD03: '1차 서류 전형', AD04: '코딩 실습 준비',
  LAB: '실습동 둘러보기', STUDIO: '프로젝트실 둘러보기', MAKER_ROOM: '메이커실 둘러보기',
  CLASSROOM: '교실 둘러보기', CAMPUS: '기숙사동 둘러보기', AUDITORIUM: '강당 둘러보기', FOOTBALL_FIELD: '축구장 둘러보기',
  FINISH_EXPLORATION: '탐색 마치기', CAMPUS_VISITED: '방문 기록됨',
  WEB_STUDIO: '행사 안내 웹 만들기', APP_STUDIO: '앱 화면 연결',
  DATA_STUDIO: '분류 데이터 체험', API_STUDIO: '급식 메뉴 API 만들기',
  GAME_STUDIO: '미니게임 조작 만들기', NETWORK_STUDIO: '실습실 연결 점검',
  SECURITY_STUDIO: '게시판 접근 권한 점검', UX_STUDIO: '도서 검색 화면 개선',
  GAME_LOOP: '게임 루프 만들기', ANDROID_INPUT: '안드로이드 입력 검증',
  IOS_LAYOUT: 'iOS 화면 배치', PYTHON_SCORE: '파이썬으로 경기 점수 집계',
  HTML_NOTICE: 'HTML로 교내 공지 정리', SQL_BOOK: 'SQL로 도서 찾기',
  SERVICE_FIX: 'API 오류 해결', NETWORK_MAP: '교내 연결 지도 만들기',
  ACCESS_AUDIT: '권한 문제 찾기', TEAM_GAME: '둘이서 미니게임 테스트',
  TEAM_MAKER: '3D 출력물 개선', TEAM_AI: 'AI 분류 결과 교차 확인',
  UI_CASE: '화면 사용성 설명', MODEL_CASE: '모델 활용 사례 조사',
  TEAM_CASE: '팀 활동 발표', MOBILE_CASE: '만들고 싶은 앱 상담',
  SERVER_CASE: '서버 개발 학습 순서 상담', SECURITY_CASE: '보안 학습 계획 상담',
  AD05: '최종 전형', BS01: '학교 탐색', BS02: '전공 체험', BS03: '기초 프로그래밍', BS04: '팀 활동', BS05: '진로 상담',
  Y201: '2학년 학과 선택', Y301: '3학년 수업 선택', E11: '기숙사 첫날', S101: '첫 공동 디버깅', PAIR_DEBUG: '민서와 함께 오류 찾기',
  S103: '동아리 첫 모임', S202: '나르샤 멘토 피드백', N104: '소프트웨어로 나르샤 · 주제 선택', N204: 'ICT융합 엑스포 발표',
  NARSHA_WEB: '생활 문제를 푸는 웹 서비스', NARSHA_APP: '기숙사 생활 도우미 앱', NARSHA_AI: '분실물 분류 AI',
  NARSHA_GAME: '짧게 즐기는 미니게임', NARSHA_FIX: '멘토 피드백으로 핵심 기능 다듬기',
  EXPO_DEMO: '무대에서 프로젝트 시연하기', EXPO_BOOTH: '부스에서 방문객에게 설명하기',
  MAKER_CLUB: '제작 동아리', MEDIA_CLUB: '미디어 동아리',
  KEEP_ROUTINE: '기숙사에서 휴식', CONTEST_PREP: '교내 공모전 준비', ENGLISH_DEMO: '영어 시연 연습', ANSWER: '면접 답변',
  CHECK_ALONE: '혼자 원인 정리', AFTER_CLASS: '수업 뒤에 다시 보기', J01: '채용 공고 상담',
  DOMESTIC: fieldLearningRoutes.DOMESTIC.name, SILICON_VALLEY: fieldLearningRoutes.SILICON_VALLEY.name,
  FIELD_LEARNING_RECORDED: '준비 체험 기록됨',
  STRESS_SUPPORT: '스트레스 돌보기', SCHOOL_DOUBTS: '학교생활 고민', WITHDRAWAL_DECISION: '자퇴 결정',
  STRESS_PUSHED_ON: '마감까지 버티기로 함', STRESS_RELIEVED: '부담을 나눔', WITHDRAWAL_CONSIDERED: '자퇴를 고민함', SCHOOL_WITHDRAWAL: '자퇴 선택',
  STRESS_REST: '일정을 줄이고 쉬기', STRESS_FRIEND: '친구에게 속마음 털어놓기', STRESS_TEACHER: '선생님과 부담을 나누기',
  ASK_FOR_SUPPORT: '선생님과 학교생활 조정 방법 찾기', REST_AND_RECONSIDER: '충분히 쉬고 다시 생각하기',
  CONSIDER_WITHDRAWAL: '자퇴를 고민해 보기', STAY_ENROLLED: '도움을 받으며 학교생활 이어가기',
  CONFIRM_WITHDRAWAL: '자퇴를 확정하고 다음 길 준비하기', LEAVE_SCHOOL: '다음 배움과 진로 준비',
  TERM_FOCUS: '학기 집중', FOCUS_SELECTED: '집중 목표 확정', VACATION: '방학',
  CAMP_STARTED: '전공강화캠프 참가 확정', VACATION_RESTED: '방학 휴식',
  CAMP_DAY_DONE: '캠프 첫 실습 완료', CAMP_COMPLETED: '전공강화캠프 수료',
  MAJOR_CAMP: '전공강화캠프 참가', VACATION_REST: '방학 동안 쉬기',
  N201: '2학년 나르샤 주제 선택', NARSHA_CUSTOM: '직접 입력한 나르샤 주제', N205: '소프트웨이브 참가 결정', SOFTWAVE_ATTEND: '소프트웨이브 참가', SOFTWAVE_DECLINE: '교내 기록으로 남기기',
  FOCUS_BALANCE: '생활과 학업의 균형', FOCUS_GRADES: '교과 성적 관리', FOCUS_CODE: '프로그래밍 기본기',
  FOCUS_CAREER: '희망 직무 연습', FOCUS_TEAM: '팀 프로젝트와 협업', FOCUS_ENGLISH: '영어와 글로벌 진로',
  TURN_ADVANCE: '턴 종료', USE_ITEM: '회복 키트 사용', RECOVERY_KIT: '회복 키트', APPLY: '기업 지원',
  CODING: '코딩 테스트', INTERVIEW: '면접', RESULT: '결과 확인',
  CAREER_PICK: '희망 직무 선택', CAREER_CHANGE: '희망 직무 변경',
  ADMISSION_PASSED: '전형 통과', ADMISSION_REJECTED: '전형 탈락',
}

export const label = (code?: string | null) => code ? labels[code] ?? code : '미정'
export const actionLabel = (code: string) => code === 'SOCIAL' ? '친구와 교류' : label(code)
export const jobLabel = (role: string) => ({
  FE: '프론트엔드', BE: '백엔드', AI: '인공지능 개발자', SW: '소프트웨어 개발', MOBILE: '모바일 개발', GAME: '게임 개발',
})[role] ?? role

export function schoolPeriod(player: PlayerView) {
  const months = [3, 4, 4, 5, 6, 7, 8, 9, 9, 10, 11, 12]
  const semester = player.turn <= months.length ? 1 : 2
  return { semester, label: `${semester}학기 · ${months[Math.floor((player.turn - 1) / 2)] ?? 11}월` }
}

export function admissionComponents(stage: number, scores: unknown) {
  if (!Array.isArray(scores)) return ''
  const names = stage === 1 ? ['자기소개서', '중학교 성적', '학교생활기록부'] : ['스크래치', '파이썬', '기초능력', '학교 면접']
  return names.map((name, index) => `${name} ${scores[index]}점`).join(' · ')
}

export function admissionProtection(protection: AdmissionProtection, passed: boolean) {
  return `보호막 발동 · ${label(protection.specialTrack)} ${protection.score}점 탈락 → 같은 결과로 일반전형 ${passed ? '통과' : '탈락'}`
}

export function phaseLabel(player?: PlayerView) {
  if (!player) return '입학 지원'
  switch (player.phase) {
    case 'ADMISSION_1': return '입학 · 1차 전형'
    case 'ADMISSION_2': return '입학 · 2차 전형'
    case 'BEFORE_SCHOOL': return `비포스쿨 ${player.beforeSchoolDay ?? 1} / 5일`
    case 'CAREER_PICK': return '비포스쿨 · 5일 완료'
    case 'GRAD_READY': return '졸업 준비'
    case 'ENDED': return player.year ? '학교생활 결과' : '입학 전형 결과'
    default: return `${player.year}학년 · ${player.turn}턴`
  }
}
