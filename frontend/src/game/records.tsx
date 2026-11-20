import { useInfiniteQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { request } from '../api/client'
import {
  playerPath, type CareerPreferenceView, type FieldLearningExperience, type FieldLearningRoute, type HistoryPage,
  type ItemStack, type PlayerView, type ProgressView, type Qualification, type Snapshot,
} from '../api/contracts'
import { usePlayerData } from '../api/hooks'
import { AssessmentResults } from './assessments'
import { admissionProtection, label } from './labels'
import { fieldLearningDetail, fieldLearningRoutes, fieldLearningStatusLabel } from './field-learning'
import { careerCatalogPath, type RunPlayer } from './activities'
import { NarshaRecord } from './school-life'
import { ChoiceForm, Loading, Problem, StatList } from './ui'

export function qualificationName(item: Qualification | string) {
  return typeof item === 'string' ? label(item) : item.title ?? label(item.courseCode)
}

export function FieldLearningRecord({ route, experience }: {
  route?: FieldLearningRoute | null
  experience?: FieldLearningExperience | null
}) {
  const record = experience ?? (route ? { route, status: 'INTEREST_RECORDED' as const } : null)
  if (!record) return null
  return (
    <div className="field-record">
      <p><strong>{fieldLearningRoutes[record.route].name}</strong> · {fieldLearningStatusLabel(record.status)}</p>
      <p>{fieldLearningDetail(record)}</p>
    </div>
  )
}

export function Notebook({ player, busy, run }: { player: PlayerView; busy: boolean; run: RunPlayer }) {
  const inventory = usePlayerData<ItemStack[]>(player.id, '/inventory')
  const progress = usePlayerData<ProgressView>(player.id, '/progress')
  const preference = usePlayerData<CareerPreferenceView>(player.id, '/career-preference', Boolean(player.initialCareerCode))
  const catalog = useCareerCatalog(player.id, Boolean(preference.data?.canChange))
  return (
    <>
      <p className="record-line"><strong>{player.name}</strong> · {label(player.admissionTrack)} · {label(player.major)} · {label(player.track)}</p>
      <p className="muted">최초 희망 · {label(player.initialCareerCode)}<br />현재 희망 · {label(player.currentCareerCode)}</p>
      <h3>능력치</h3><StatList values={player.stats} />
      <h3>가방</h3>
      {inventory.isPending ? <Loading /> : inventory.error ? <Problem error={inventory.error} retry={() => { void inventory.refetch() }} /> :
        inventory.data.length ? inventory.data.map((item) => (
          <div className="action-row" key={item.itemCode}>
            <div><strong>{item.name} × {item.quantity}</strong>{item.reason && <small>{item.reason}</small>}</div>
            <button className="btn" type="button" disabled={busy || !item.enabled || !item.quantity}
              onClick={() => run('POST', `/inventory/${encodeURIComponent(item.itemCode)}/use`)}>사용</button>
          </div>
        )) : <p className="muted">가방이 비어 있습니다.</p>}
      <h3>성과</h3>
      {progress.error ? <Problem error={progress.error} retry={() => { void progress.refetch() }} /> : progress.isPending ? <Loading /> : (
        <>
          <p className="muted">자격증 · {progress.data.qualifications.map(qualificationName).join(', ') || '아직 없음'}</p>
          <FieldLearningRecord route={progress.data.fieldLearningRoute} experience={progress.data.fieldLearning} />
          {progress.data.narshaProjects?.length ? progress.data.narshaProjects.map((record, i) =>
            <NarshaRecord key={record.schoolYear ?? i} record={record} />) : <NarshaRecord record={progress.data.narsha} />}
          {progress.data.projects.map((project) => <p className="record-line" key={project.id}>{label(project.projectCode)} · {label(project.status)}</p>)}
        </>
      )}
      {preference.data?.canChange && (
        <>
          <h3>희망 직무 변경</h3>
          <p className="muted">재학 중 한 번 변경할 수 있으며 AP를 사용하지 않습니다.</p>
          <ChoiceForm options={preference.data.eligibleChangeRoleCodes.map((value) => ({ value, label: catalog.data?.find((item) => item.code === value)?.title ?? label(value) }))}
            submit="희망 직무 변경" busy={busy} onSubmit={(roleCode) => run('PUT', '/career-preference', { roleCode })} />
        </>
      )}
      <p className="notebook-footer"><Link className="btn" to="/">시작 화면</Link></p>
    </>
  )
}

function timelineLabel(entry: Snapshot) {
  return entry.beforeSchoolDay ? `비포스쿨 ${entry.beforeSchoolDay}일` : `${entry.year ?? ''}학년 ${entry.turn ?? ''}턴`
}

export function History({ player }: { player: PlayerView }) {
  const history = useInfiniteQuery({
    queryKey: ['player', player.id, '/history'],
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => request<HistoryPage>(`${playerPath(player.id)}/history?afterId=${pageParam}&limit=20`, { signal }),
    getNextPageParam: (page) => page.nextAfterId ?? undefined,
    retry: false,
  })
  const progress = usePlayerData<ProgressView>(player.id, '/progress')
  const entries = history.data?.pages.flatMap((page) => page.items).slice().reverse()
  return (
    <>
      {progress.data?.snapshots.length ? <details className="timeline">
        <summary>날짜별 성장 기록 · {progress.data.snapshots.length}개</summary>
        <ul>{progress.data.snapshots.map((entry, index) => (
          <li key={index}>{timelineLabel(entry)} · 프로그래밍 {entry.stats.P}
            {typeof entry.stats.G === 'number' && ` · 성적 ${entry.stats.G}`} · 체력 {entry.stats.H} · 스트레스 {entry.stats.S}</li>
        ))}</ul>
      </details> : null}
      {history.isPending ? <Loading /> : history.error ? <Problem error={history.error} retry={() => { void history.refetch() }} /> :
        entries?.length ? (
          <ol className="history-list">{entries.map((entry) => (
            <li key={entry.logId}>
              <span>{entry.beforeSchoolDay ? `비포스쿨 ${entry.beforeSchoolDay}일` : entry.year ? `${entry.year}학년 ${entry.turn}턴` : '입학 전형'}</span>
              <strong>{entry.resultCode === 'FIELD_LEARNING_PREPARED' ? '현장학습 준비 완료' :
                entry.resultCode === 'FIELD_LEARNING_COMPLETED' ? '현장학습 참가 완료' : label(entry.subjectCode)}</strong>
              {entry.choiceCode && <small>{label(entry.choiceCode)}</small>}
              {entry.admission && <small>{label(entry.admission.admissionTrack)} · {entry.admission.score}점 · {label(entry.resultCode)}
                {entry.admission.minimumScore !== undefined && ` · 통과 기준 ${entry.admission.minimumScore}점`}</small>}
              {entry.admission?.protection && <small>{admissionProtection(entry.admission.protection, entry.admission.passed === true)}</small>}
              <AssessmentResults results={entry.assessments} />
            </li>
          ))}</ol>
        ) : <p className="muted">기록할 선택이 아직 없습니다.</p>}
      {history.hasNextPage && <button className="btn wide" type="button" disabled={history.isFetchingNextPage}
        onClick={() => { void history.fetchNextPage() }}>기록 더 보기</button>}
    </>
  )
}

export function useCareerCatalog(id: string, enabled: boolean) {
  return usePlayerData<{ code: string; title: string }[]>(id, 'catalog/career-roles', enabled, careerCatalogPath(id))
}
