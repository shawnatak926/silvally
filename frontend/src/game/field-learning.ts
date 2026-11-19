import type { FieldLearningExperience, FieldLearningRoute } from '../api/contracts.ts'

export const overseasEnglishCutoff = 50

export const fieldLearningRoutes: Record<FieldLearningRoute, {
  name: string
  choice: string
  preview: string
  record: string
  prepared: string
  completed: string
}> = {
  DOMESTIC: {
    name: '국내 현장실습',
    choice: '국내 현장실습 신청하기',
    preview: '기업 현장에서 실무 과제와 안전 수칙을 익히는 게임 경로입니다. 교내 산학협력 프로젝트와 해외 파견은 별도이며, 취업은 이후 채용 결과로 결정됩니다.',
    record: '가상 실습 공고를 읽고 담당 직무와 안전 수칙을 확인했습니다.',
    prepared: '실습을 앞두고 안전 수칙과 기업에서 맡을 과제를 확인했습니다.',
    completed: '기업 멘토와 과제에서 발견한 오류를 고치고 수정 내용을 정리했습니다.',
  },
  SILICON_VALLEY: {
    name: '실리콘밸리 현장학습',
    choice: '실리콘밸리 현장학습 지원하기',
    preview: `온라인 사전 협업과 영어 준비를 거쳐 현지 과제를 경험하는 게임 경로입니다. MVP는 영어 ${overseasEnglishCutoff}점부터 선발하는 가상 규칙을 사용하며 실제 학교의 다단계 선발과 다릅니다.`,
    record: '영어 프로젝트 소개와 원격 코드 리뷰 준비를 살펴봤습니다.',
    prepared: '글로벌 온라인 인턴십 안내를 바탕으로 원격 협업 과제를 연습하고, 프로젝트를 영어로 소개하며 코드 리뷰 질문을 정리했습니다.',
    completed: '영어로 프로젝트에서 맡은 작업을 설명하고 코드 리뷰에서 피드백을 받았습니다.',
  },
}

export function isFieldLearningRoute(value: unknown): value is FieldLearningRoute {
  return value === 'DOMESTIC' || value === 'SILICON_VALLEY'
}

const fieldLearningStatuses: Record<FieldLearningExperience['status'], string> = {
  INTEREST_RECORDED: '관심 기록',
  SELECTED: '참가 확정',
  NOT_SELECTED: '미선발',
  PREPARED: '준비 완료',
  COMPLETED: '참가 완료',
}

export function fieldLearningStatusLabel(status: FieldLearningExperience['status']) {
  return fieldLearningStatuses[status]
}

export function fieldLearningDetail(experience: FieldLearningExperience) {
  const route = fieldLearningRoutes[experience.route]
  switch (experience.status) {
    case 'SELECTED':
      return experience.preparationTurn && experience.participationTurn
        ? `3학년 ${experience.preparationTurn}턴을 마치면 준비하고 ${experience.participationTurn}턴을 마치면 참가 기록이 남습니다.`
        : '다음 재학 일정에서 준비와 참가가 진행됩니다.'
    case 'NOT_SELECTED':
      return experience.selectionScore != null && experience.requiredScore != null
        ? `게임 내 영어 ${experience.selectionScore}점으로 선발 기준 ${experience.requiredScore}점에 미치지 못했습니다.`
        : '게임 내 영어 선발 기준에 미치지 못했습니다.'
    case 'PREPARED':
      return route.prepared
    case 'COMPLETED':
      return route.completed
    case 'INTEREST_RECORDED':
      return route.record
  }
}

export function isFieldLearningExperience(value: unknown): value is FieldLearningExperience {
  if (!value || typeof value !== 'object' || !('route' in value) || !('status' in value)) return false
  return isFieldLearningRoute(value.route) && typeof value.status === 'string' && Object.hasOwn(fieldLearningStatuses, value.status)
}
