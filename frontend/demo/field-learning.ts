import type { FieldLearningExperience, FieldLearningRoute, Stats } from '../src/api/contracts.ts'
import { fieldLearningRoutes, overseasEnglishCutoff } from '../src/game/field-learning.ts'

import { applicationTurn as preparationTurn, codingTurn as participationTurn } from '../src/game/schedule.ts'

export function selectFieldLearning(route: FieldLearningRoute, english: number): FieldLearningExperience {
  const schedule = { preparationTurn, participationTurn }
  if (route === 'DOMESTIC') return { route, status: 'SELECTED', ...schedule }
  const score = { selectionScore: english, requiredScore: overseasEnglishCutoff }
  if (english < overseasEnglishCutoff) return { route, status: 'NOT_SELECTED', ...score }
  return { route, status: 'SELECTED', ...score, ...schedule }
}

export function advanceFieldLearning(experience: FieldLearningExperience | undefined, year: number, turn: number): {
  experience: FieldLearningExperience
  resultCode: 'FIELD_LEARNING_PREPARED' | 'FIELD_LEARNING_COMPLETED'
  delta: Partial<Stats>
} | null {
  if (!experience || year !== 3) return null
  if (turn === preparationTurn && experience.status === 'SELECTED') {
    return { experience: { ...experience, status: 'PREPARED' }, resultCode: 'FIELD_LEARNING_PREPARED', delta: {} }
  }
  if (turn === participationTurn && experience.status === 'PREPARED') {
    return {
      experience: { ...experience, status: 'COMPLETED', completedYear: year, completedTurn: turn },
      resultCode: 'FIELD_LEARNING_COMPLETED',
      delta: experience.route === 'DOMESTIC' ? { P: 2, C: 3 } : { EN: 4, C: 3 },
    }
  }
  return null
}

export function fieldLearningSummary(experience: FieldLearningExperience | undefined, route: FieldLearningRoute | null) {
  return experience ?? (route ? { route, status: 'INTEREST_RECORDED' as const } : null)
}

export function fieldLearningEnding(experience: FieldLearningExperience | undefined, roleEndingCode: string, employed: boolean) {
  if (experience?.status !== 'COMPLETED') return { endingCode: roleEndingCode, bodyPrefix: '' }
  const domestic = experience.route === 'DOMESTIC'
  const bodyPrefix = domestic ? '국내 현장실습에서 멘토와 오류를 찾아 고친 일을 떠올립니다. ' :
    '실리콘밸리 현장학습에서 영어로 작업을 설명하고 피드백을 받은 일을 떠올립니다. '
  if (!employed) return { endingCode: roleEndingCode, bodyPrefix }
  return { endingCode: domestic ? 'JOB_AFTER_DOMESTIC' : 'JOB_AFTER_SILICON_VALLEY', bodyPrefix, roleEndingCode }
}

export function fieldLearningScene(experience: FieldLearningExperience): [string, string] | null {
  const route = fieldLearningRoutes[experience.route]
  switch (experience.status) {
    case 'SELECTED':
      return [`${route.name} 참가 확정`, '결과를 확인하고 준비 일정과 참가 날짜를 수첩에 적었다.']
    case 'NOT_SELECTED':
      return [`${route.name} 선발 결과`, '이번 선발 기준에 미치지 못했다. 다음에 연습할 영어 발표 부분을 수첩에 표시했다.']
    case 'PREPARED':
      return [`${route.name} 준비`, route.prepared]
    case 'COMPLETED':
      return [`${route.name} 참가 기록`, route.completed]
    case 'INTEREST_RECORDED':
      return null
  }
}
