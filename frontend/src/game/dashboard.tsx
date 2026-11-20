import type { PlayerView, ProgressView } from '../api/contracts'
import { usePlayerData } from '../api/hooks'
import { useGameUi } from '../state'
import { yearTurns } from './schedule'
import { label, phaseLabel } from './labels'
import { FieldLearningRecord, qualificationName } from './records'
import { NarshaRecord, SchoolLifeCatalog } from './school-life'
import { CampusPhoto, StatList } from './ui'

const schoolTurns = [yearTurns, yearTurns, yearTurns]
const totalTurns = schoolTurns.reduce((total, turns) => total + turns, 0)

function schoolProgress(player: PlayerView) {
  if (player.year < 1) return undefined
  if (player.phase === 'GRAD_READY' || (player.phase === 'ENDED' && player.endReason !== 'WITHDRAWAL')) return totalTurns
  const earlierTurns = schoolTurns.slice(0, player.year - 1).reduce((total, turns) => total + turns, 0)
  return Math.min(totalTurns, earlierTurns + Math.max(0, player.turn - 1))
}

function goals(player: PlayerView) {
  switch (player.phase) {
    case 'ADMISSION_1': return ['지원 전형과 서류 준비']
    case 'ADMISSION_2': return ['코딩·기초능력 전형과 학교 면접']
    case 'BEFORE_SCHOOL': return ['5일 동안 학교와 전공 체험']
    case 'CAREER_PICK': return ['11개 직무 중 첫 희망 직무 선택']
    case 'GRAD_READY': return ['졸업 후 진로 결정']
    case 'ENDED': return []
  }
  switch (player.year) {
    case 1: return ['기초 수업과 기숙사 생활 적응', '친구와 협업하고 첫 프로젝트 경험하기']
    case 2: return [player.major ? `${label(player.major)} 수업 경험` : '2학년 학과 선택', '팀 프로젝트와 자격증 준비']
    case 3: return [player.track ? `${label(player.track)} 실무 수업 경험` : '3학년 수업 선택', '현장학습과 졸업 후 진로 탐색']
    default: return []
  }
}

export function DashboardLinks() {
  return (
    <nav className="dashboard-links" aria-label="보조 패널 바로 가기">
      <a className="btn" href="#student-profile">학생 정보</a>
      <a className="btn" href="#school-context">학교생활 현황</a>
    </nav>
  )
}

export function StudentRail({ player }: { player: PlayerView }) {
  const setDialog = useGameUi((state) => state.setDialog)
  const progress = usePlayerData<ProgressView>(player.id, '/progress')
  const featuredProjects = progress.data?.projects.filter((item) => item.status === 'COMPLETE' || item.status === 'IN_PROGRESS')

  return (
    <aside className="story-rail student-rail" id="student-profile" aria-label="학생 정보" tabIndex={-1}>
      <section className="story-card rail-card student-card">
        <p className="eyebrow">학생 정보</p>
        <h2>{player.name}</h2>
        <dl className="student-details">
          <div><dt>{player.edition === 'MILD' ? '시작 시점' : '입학 전형'}</dt><dd>{player.edition === 'MILD' ? '1학년 1학기' : label(player.admissionTrack)}</dd></div>
          <div><dt>희망 직무</dt><dd>{label(player.currentCareerCode)}</dd></div>
          <div><dt>2학년 학과</dt><dd>{label(player.major)}</dd></div>
          <div><dt>3학년 수업</dt><dd>{label(player.track)}</dd></div>
        </dl>
        <div className="rail-actions">
          <button className="btn" type="button" onClick={() => setDialog('notebook')}>학생 수첩</button>
          <button className="btn" type="button" onClick={() => setDialog('history')}>전체 기록</button>
        </div>
      </section>
      <section className="story-card rail-card rail-stats">
        <h2>핵심 능력치</h2>
        <StatList values={player.stats} />
        {player.year > 0 && <p className="rail-copy">성적은 중간·기말고사와 수행평가 결과에 따라 바뀝니다.</p>}
      </section>
      <section className="story-card rail-card">
        <h2>주요 성과</h2>
        {progress.data ? <>
          <h3>자격증 <span>{progress.data.qualifications.length}</span></h3>
          <p className="rail-copy">{progress.data.qualifications.map(qualificationName).join(', ') || '취득한 자격증이 없습니다.'}</p>
          <h3>프로젝트 <span>{featuredProjects?.length ?? 0}</span></h3>
          {featuredProjects?.length ? <ul className="rail-list">{featuredProjects.slice(0, 3).map((item) => (
            <li key={item.id}>{label(item.projectCode)} <small>{label(item.status)}</small></li>
          ))}{featuredProjects.length > 3 && <li>외 {featuredProjects.length - 3}건</li>}</ul>
            : <p className="rail-copy">진행하거나 완성한 프로젝트가 없습니다.</p>}
        </> : <p className="rail-copy">{progress.isError ? '성과를 불러오지 못했습니다.' : '성과를 불러오는 중입니다.'}</p>}
      </section>
    </aside>
  )
}

export function SchoolRail({ player }: { player: PlayerView }) {
  const setDialog = useGameUi((state) => state.setDialog)
  const progress = usePlayerData<ProgressView>(player.id, '/progress')
  const relationship = progress.data?.friendRelationship
  const portfolio = progress.data?.projects.filter((item) => item.inPortfolio)
  const completed = schoolProgress(player)
  const currentGoals = goals(player)

  return (
    <aside className="story-rail school-rail" id="school-context" aria-label="학교생활 현황" tabIndex={-1}>
      <section className="story-card rail-card school-card">
        <p className="eyebrow">학교생활 현황</p>
        <h2>{player.endReason === 'WITHDRAWAL' ? '재학 생활 종료' : completed === undefined ? phaseLabel(player) : completed === totalTurns ? '재학 일정 완료' : `${player.year}학년 일정`}</h2>
        {completed !== undefined ? <>
          <div className="school-progress">
            <span>재학 일정</span><strong>{completed} / {totalTurns}턴 완료</strong>
            <progress max={totalTurns} value={completed} aria-label="재학 일정 진행률" />
          </div>
          <p className="rail-copy">{player.endReason === 'WITHDRAWAL' ? '자퇴로 재학 생활을 마쳤습니다.' : player.phase === 'ENDED' ? '졸업 완료' : completed === totalTurns ? '졸업 결정 대기' : `졸업까지 재학 일정 ${totalTurns - completed}턴`}</p>
        </> : player.phase === 'ENDED' ? <p className="rail-copy">입학 전형이 종료되었습니다.</p>
          : <p className="rail-copy">입학 후 재학 일정이 시작됩니다.</p>}
        {currentGoals.length > 0 && <><h3>{player.year ? '이번 학년의 목표' : '현재 목표'}</h3>
          <ul className="rail-goals">{currentGoals.map((goal) => <li key={goal}>{goal}</li>)}</ul></>}
      </section>
      {progress.data?.narsha && <section className="story-card rail-card narsha-card" aria-label="나르샤 프로젝트 기록">
        {progress.data.narshaProjects?.length
          ? progress.data.narshaProjects.map((record, i) => <NarshaRecord key={record.schoolYear ?? i} record={record} />)
          : <NarshaRecord record={progress.data.narsha} />}
      </section>}
      <section className="story-card rail-card">
        <h2>친구와 관계</h2>
        {relationship && relationship.milestones > 0 ? <div className="friend-record">
          <strong>{relationship.npc}</strong>
          <span>교류 {relationship.milestones}회</span>
          <span>친밀도 {relationship.rapport} / 5</span>
        </div> : <p className="rail-copy">{progress.isError ? '교류 기록을 불러오지 못했습니다.' : progress.isPending ? '교류 기록을 불러오는 중입니다.' : '아직 표시할 교류 기록이 없습니다.'}</p>}
      </section>
      <section className="story-card rail-card">
        <h2>진로 이력</h2>
        <dl className="student-details">
          <div><dt>첫 희망</dt><dd>{label(player.initialCareerCode)}</dd></div>
          <div><dt>현재 희망</dt><dd>{label(player.currentCareerCode)}</dd></div>
        </dl>
        <FieldLearningRecord route={progress.data?.fieldLearningRoute} experience={progress.data?.fieldLearning} />
        <h3>자격증과 포트폴리오</h3>
        {progress.data ? <>
          <p className="rail-copy">자격증 {progress.data.qualifications.length}건 · 등록 작품 {portfolio?.length ?? 0}건</p>
          {!!portfolio?.length && <ul className="rail-list">{portfolio.map((item) => <li key={item.id}>{label(item.projectCode)}</li>)}</ul>}
        </> : <p className="rail-copy">{progress.isError ? '성과를 불러오지 못했습니다.' : '성과를 불러오는 중입니다.'}</p>}
      </section>
      <SchoolLifeCatalog playerId={player.id} />
      <figure className="campus-card">
        <CampusPhoto className="campus-photo" />
        <figcaption><span>대소고 캠퍼스</span><button className="btn" type="button" onClick={() => setDialog('photo')}>사진 보기</button></figcaption>
      </figure>
    </aside>
  )
}
