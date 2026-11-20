import type { AssessmentResult } from '../src/api/contracts.ts'
import { semesterTurns as semesterLength } from '../src/game/schedule.ts'
import type { DemoGame } from './game.ts'

const titles: Record<AssessmentResult['kind'], string> = {
  MIDTERM: '중간고사', PERFORMANCE: '수행평가', FINAL: '기말고사',
}
const cap = (value: number) => Math.max(0, Math.min(100, value))

export function prepareAssessment(game: DemoGame, amount: number) {
  game.assessmentPrep = Math.min(6, (game.assessmentPrep ?? 0) + amount)
}

export function evaluateAssessments(game: DemoGame): AssessmentResult[] {
  const { turn, stats } = game.view
  const period = (turn - 1) % semesterLength + 1
  const semester = Math.floor((turn - 1) / semesterLength) + 1
  const kinds: AssessmentResult['kind'][] = period === 6 ? ['MIDTERM'] : period === 10 ? ['PERFORMANCE'] : period === semesterLength ? ['FINAL'] : []
  const { A, P, C, H, S } = stats
  const condition = (H - 60) * .10 - S * .18
  const prep = game.assessmentPrep ?? 0
  const results = kinds.map((kind) => {
    const base = kind === 'PERFORMANCE' ? 42 + .30 * A + .25 * P + .15 * C : 40 + .45 * A + .20 * P
    const score = cap(Math.round(base + 2 * prep + condition))
    const gradeBefore = stats.G
    const gradeAfter = cap(gradeBefore + Math.round((score - gradeBefore) / 3))
    stats.G = gradeAfter
    return { kind, title: titles[kind], semester, score, gradeBefore, gradeAfter }
  })
  if (period === semesterLength) delete game.assessmentPrep
  return results
}
