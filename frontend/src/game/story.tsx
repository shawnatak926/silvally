import type { ReactNode } from 'react'
import type { PlayerView, StoryEntry } from '../api/contracts'
import { AssessmentResults, StatSetbacks, StressPressure } from './assessments'

export { DashboardLinks, SchoolRail, StudentRail } from './dashboard'

function period(entry: StoryEntry) {
  if (entry.year) return `${entry.year}학년 ${entry.turn}턴`
  if (entry.beforeSchoolDay) return `비포스쿨 ${entry.beforeSchoolDay}일 차`
  return '입학 전형'
}

function StoryText({ entry }: { entry: StoryEntry }) {
  return (
    <div className="story-text">
      <p>{entry.text}</p>
      <AssessmentResults results={entry.assessments} />
      <StatSetbacks results={entry.setbacks} />
      <StressPressure load={entry.stressLoad} />
      {entry.dialogue.length > 0 && <dl className="story-dialogue">
        {entry.dialogue.map((line, index) => <div key={index}><dt>{line.speaker}</dt><dd>{line.text}</dd></div>)}
      </dl>}
    </div>
  )
}

export function CurrentStory({ player, entry, loading, failed, retry, children }: {
  player: PlayerView; entry?: StoryEntry; loading: boolean; failed: boolean; retry: () => void; children?: ReactNode
}) {
  return (
    <article className="story-card current-story" id="current-story" aria-labelledby="current-story-title">
      <p className="section-label">최근 로그</p>
      {entry && <p className="story-kicker">{period(entry)}</p>}
      <h2 id="current-story-title" aria-live="polite">{entry?.title ?? `${player.name}의 학교생활`}</h2>
      {entry && <StoryText entry={entry} />}
      {children}
      {entry?.status === 'PENDING' && !failed && <p className="story-note" role="status">기록 작성 중</p>}
      {failed && <div className="story-note story-retry"><p>이야기를 불러오지 못했습니다.</p><button className="btn" type="button" onClick={retry}>이야기 다시 불러오기</button></div>}
      {!entry && !failed && <p className="story-note">{loading ? '이야기를 불러오고 있습니다.' : '게임을 진행하면 이곳에 장면이 기록됩니다.'}</p>}
    </article>
  )
}

export function StoryJournal({ entries }: { entries: StoryEntry[] }) {
  return (
    <section className="past-stories" id="past-stories" aria-labelledby="past-stories-title">
      <div className="journal-heading"><h2 id="past-stories-title">여태껏 했던 로그</h2><span>{entries.length}건</span></div>
      {entries.length ?
    <ol className="story-card journal-entries" aria-label="이전 장면">
      {entries.slice().reverse().map((entry) => <li key={entry.id}>
        <span className="journal-period">{period(entry)}</span>
        <div>
          <h3>{entry.title}</h3>
          <StoryText entry={entry} />
        </div>
      </li>)}
    </ol>
      : <p className="story-note">아직 이전 로그가 없습니다. 이야기가 진행되면 여기에 쌓입니다.</p>}
    </section>
  )
}
