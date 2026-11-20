import type { InterviewEvaluation, InterviewQuestion } from '../src/api/contracts.ts'

const roleLabels: Record<string, string> = { FE: '프론트엔드', BE: '백엔드', AI: 'AI', SW: '소프트웨어' }
const roleTerms: Record<string, string[]> = {
  FE: ['화면', '컴포넌트', '렌더링', '접근성', '상태 관리', 'react', 'css', 'ui'],
  BE: ['api', '서버', '데이터베이스', '쿼리', '캐시', '트랜잭션', '인증', 'db'],
  AI: ['모델', '데이터', '추론', '학습', '프롬프트', '정확도', 'ml', 'llm'],
  SW: ['앱', '기능', '데이터', '코드', 'api', '모듈', '구현', '배포'],
}

type Criterion = [label: string, ...signals: string[][]]

const schoolCriteria: Criterion[] = [
  ['지원 동기', ['지원', '관심', 'apply'], ['대소고', '학교', 'school'], ['이유', '때문', '계기', '덕분', '영향', 'because'], ['배우', '목표', '진로', 'learn']],
  ['지원서와 활동 경험', ['자기소개서', '지원서', '활동', '프로젝트', 'project'], ['맡', '역할', '담당', 'role'], ['구현', '만들', '연습', '정리', '기록', 'build'], ['파이썬', '스크래치', '피드백', '기능', '결과물', '기여', 'python', 'scratch']],
  ['문제 해결 과정', ['문제', '오류', 'problem', 'bug'], ['원인', '재현', '분석', '비교', '살펴', 'cause'], ['해결', '수정', '개선', 'fix'], ['확인', '테스트', '검증', '결과물', '기록', 'test']],
  ['입학 후 관심과 계획', ['입학 후', '앞으로', '대소고에서', 'future'], ['개발', '프로그래밍', '진로', '문제 해결', '문제를 해결', 'program'], ['배우', '연습', '학습', '성장', 'learn'], ['친구', '협업', '팀', '동아리', 'team']],
]

const companyCriteria: Criterion[] = [
  ['프로젝트 경험', ['프로젝트', '서비스', '앱', 'project'], ['역할', '담당', '맡', 'role'], ['문제', '목표', '요구', 'problem'], ['결과', '개선', '완료', '출시', 'result']],
  ['협업 경험', ['팀', '동료', '협업', 'team'], ['공유', '합의', '조율', 'agree'], ['리뷰', '피드백', '논의', 'review'], ['반영', '소통', '분담', 'change']],
  ['직무 적합성', ['직무', '회사', '지원', 'role'], ['활용', '기여', '적용', 'contribute'], ['경험', '배운', 'learn'], ['안정', '성능', '사용자', '품질', 'quality']],
]

export function schoolQuestion(playerId: string, preparedLanguage: string): InterviewQuestion {
  const language = preparedLanguage === 'SCRATCH' ? '스크래치' : '파이썬'
  return { id: `school-${playerId}`, source: 'MOCK', kind: 'SCHOOL',
    question: `지원서에 적은 경험이나 ${language} 준비 중 해결한 문제를 예로 들어, 대소고에 지원한 이유와 입학 후 배우고 싶은 것을 이야기해 주실래요?` }
}

export function companyQuestion(applicationId: string, company: string, role: string): InterviewQuestion {
  return { id: `company-${applicationId}`, source: 'MOCK', kind: 'COMPANY',
    question: `${company}의 ${roleLabels[role] ?? roleLabels.SW} 직무에 지원하셨군요. 프로젝트에서 어떤 기술로 문제를 해결했고, 팀과 협업하며 얻은 경험을 이 직무에 어떻게 활용하겠습니까?` }
}

export function gradeInterview(answer: string, role?: string): InterviewEvaluation {
  const text = answer.normalize('NFKC').toLowerCase()
  const rubric: Criterion[] = role ? [
    ['직무 기술 이해', roleTerms[role] ?? roleTerms.SW, ['설계', '구현', '선택', 'design'], ['원인', '분석', '병목', 'debug'], ['테스트', '검증', '로그', 'test']],
    ...companyCriteria,
  ] : schoolCriteria
  const criteria = rubric.map(([label, ...signals]) => ({
    label, score: 25 * signals.filter((group) => group.some((word) => text.includes(word))).length,
  }))
  const score = Math.round(criteria.reduce((total, item) => total + item.score, 0) / criteria.length)
  return { score, criteria }
}
