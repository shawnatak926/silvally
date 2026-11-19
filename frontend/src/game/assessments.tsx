import type { AssessmentResult, Stat, StatSetback, StressLoad } from '../api/contracts'
import { stats } from './labels'

export function AssessmentResults({ results }: { results?: AssessmentResult[] | null }) {
  if (!results?.length) return null
  return (
    <ul className="assessment-results" aria-label="학교 평가 결과">
      {results.map(({ kind, title, semester, score, gradeBefore, gradeAfter }) => {
        const change = gradeAfter - gradeBefore
        const changeLabel = change ? `${change > 0 ? '+' : '−'}${Math.abs(change)}` : '변동 없음'
        return <li key={kind}>
          <strong>{semester}학기 {title} · {score}점</strong>
          <small>성적 {gradeBefore} → {gradeAfter} ({changeLabel})</small>
        </li>
      })}
    </ul>
  )
}


export function StatSetbacks({ results }: { results?: StatSetback[] | null }) {
  if (!results?.length) return null
  return (
    <ul className="assessment-results stat-setbacks" aria-label="능력치 하락 기록">
      {results.map((item) => <li key={item.reasonCode}>
        <strong>{item.reasonCode === 'OVERWORK' ? '피로 누적' : '연습 부족'}</strong>
        <span>{Object.entries(item.statDelta).map(([stat, delta]) => `${stats[stat as Stat]} ${delta}`).join(' · ')}</span>
        <small>{item.reason}</small>
      </li>)}
    </ul>
  )
}


export function StressPressure({ load }: { load?: StressLoad | null }) {
  if (!load?.total || !load.sources.some((source) => source.code !== 'SCHOOL_ROUTINE')) return null
  return (
    <ul className="assessment-results" aria-label="스트레스 변화 이유">
      <li><strong>{load.total < 0 ? '덜어 낸 부담' : '쌓인 부담'} · 스트레스 {load.total > 0 ? '+' : ''}{load.total}</strong>
        {load.sources.filter((source) => source.delta !== 0).map((source) =>
          <small key={source.code}>{source.reason} ({source.delta > 0 ? '+' : ''}{source.delta})</small>)}
      </li>
    </ul>
  )
}
