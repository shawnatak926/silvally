import type { NarshaView, SchoolLifeCatalogEntry, SchoolLifeSource } from '../api/contracts'
import { usePlayerData } from '../api/hooks'

const narshaStages: Record<NarshaView['status'], string> = {
  PLANNING: '기획·개발 중',
  YEAR_ONE_COMPLETE: '프로젝트 완료',
  MENTORED: '2학년 개발·검증',
  PRESENTED: '발표 완료',
}

function SourceCitation({ source }: { source: SchoolLifeSource }) {
  const title = source.year ? `${source.title} (${source.year})` : source.title
  return (
    <>
      <p>{source.fact}</p>
      {(/^https?:\/\//i.test(source.url) || /^\/school-sources\/[a-z0-9-]+\.pdf(?:#page=\d+)?$/.test(source.url))
        ? <a href={source.url} target="_blank" rel="noopener noreferrer">{title}<span className="source-new-tab"> · 새 탭</span></a>
        : <span>{title}</span>}
    </>
  )
}

export function EventSource({ source }: { source?: SchoolLifeSource | null }) {
  if (!source) return null
  return (
    <div className="event-source">
      <p className="eyebrow">학교 자료에서 확인한 내용</p>
      <SourceCitation source={source} />
      <p className="school-life-note">이 사건의 시기, 선택지, 보상은 게임 설정입니다.</p>
    </div>
  )
}

export function SchoolLifeCatalog({ playerId }: { playerId: string }) {
  const catalog = usePlayerData<SchoolLifeCatalogEntry[]>(playerId, 'catalog/school-life', true,
    `/catalog/school-life?playerId=${encodeURIComponent(playerId)}`)
  return (
    <details className="story-card rail-card school-life-sources">
      <summary>학교생활 근거 자료{catalog.data?.length ? <span>{catalog.data.length}건</span> : null}</summary>
      <p className="school-life-note">2027학년도 안내 PDF와 학교 공개 자료를 참고했습니다. 자료 안의 과거 사례·운영 계획을 구분하며, 사건의 턴·선택·보상은 게임 설정입니다.</p>
      {catalog.isError ? <div className="source-error"><p>학교 자료를 불러오지 못했습니다.</p>
        <button className="btn" type="button" onClick={() => { void catalog.refetch() }}>자료 다시 불러오기</button></div>
        : catalog.isPending ? <p className="rail-copy" role="status">학교 자료를 불러오는 중입니다.</p>
          : catalog.data.length ? <ul className="school-life-source-list">{catalog.data.map((source) => (
            <li key={source.code}><SourceCitation source={source} /></li>
          ))}</ul> : <p className="rail-copy">등록된 학교 자료가 없습니다.</p>}
    </details>
  )
}

export function NarshaRecord({ record }: { record?: NarshaView | null }) {
  if (!record) return null
  return (
    <div className="narsha-record">
      <div className="narsha-record-heading"><strong>{record.schoolYear ? `${record.schoolYear}학년 나르샤` : '나르샤 프로젝트'}</strong><span>{narshaStages[record.status]}</span></div>
      <dl>
        <div><dt>주제</dt><dd>{record.topicLabel}</dd></div>
        {typeof record.yearOneWorkTurns === 'number' && <div><dt>1학년</dt><dd>프로젝트 진행 {record.yearOneWorkTurns}/16턴</dd></div>}
        {typeof record.yearTwoWorkTurns === 'number' && <div><dt>2학년</dt><dd>발표 준비 {record.yearTwoWorkTurns}/20턴</dd></div>}
        {record.rank != null && <div><dt>순위</dt><dd>{record.rank}위</dd></div>}
        {record.rank != null && record.rank <= 3 && <div><dt>전시</dt><dd>{record.softwaveStatus === 'ATTENDED' ?
          '소프트웨이브 참가' : record.softwaveStatus === 'DECLINED' ? '교내 기록으로 남김' : '소프트웨이브 참가 가능'}</dd></div>}
        {record.mentorChoice && <div><dt>멘토링</dt><dd>{record.mentorChoice}</dd></div>}
        {record.presentationChoice && <div><dt>발표 방식</dt><dd>{record.presentationChoice}</dd></div>}
        {typeof record.quality === 'number' && <div><dt>완성도</dt><dd>{record.quality}점</dd></div>}
      </dl>
    </div>
  )
}
