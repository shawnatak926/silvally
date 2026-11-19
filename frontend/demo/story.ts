import type { StoryEntry } from '../src/api/contracts.ts'
import { beforeSchool, clubEvent, majorEvent, narshaExpoEvent, narshaTopicEvent, softwaveEvent, teamGoalEvent, trackEvent, yearTwoNarshaEvent } from './data.ts'
import { fieldLearningScene } from './field-learning.ts'
import { events, type DemoGame } from './game.ts'
import { schoolScenes } from './school-life.ts'

const checkpoints = [
  ['교실의 첫 자리', '자리를 고르는 소리 사이로 첫 수업이 시작된다. 가방에서 실습 노트를 꺼낸다.'],
  ['실습실의 오류 메시지', '실습 시간에 실행 창이 멈춘다. 코드에서 무엇이 달라졌는지 다시 살펴본다.'],
  ['방과후의 빈자리', '정규 수업이 끝나고 실습실에 빈자리가 생겼다. 남아서 과제를 하거나 기숙사로 돌아갈 수 있다.'],
  ['첫해의 마지막 노트', '첫해에 적어 둔 실습 노트를 펼친다. 지워진 코드를 다시 읽으니 막혔던 부분을 찾기 쉬워졌다.'],
  ['전공을 고르는 날', '두 학과의 과제 예시가 책상에 놓였다. 어떤 문제를 더 오래 붙들어 보고 싶은지 생각해 본다.'],
  ['팀 프로젝트 회의', '팀 회의에서 이번 주에 끝낼 작업을 정한다. 기능을 더 넣자는 의견과 시연을 먼저 하자는 의견이 갈린다.'],
  ['발표를 앞둔 실습실', '시연 순서를 맞추고 팀원들과 각자 맡은 작업을 확인한다.'],
  ['2학년의 끝', '팀이 공유 폴더를 정리한다. 마친 일과 아직 남은 문제가 한 화면에 보인다.'],
  ['마지막 해의 수업', '졸업을 앞두고 더 깊게 배울 실무 수업을 고른다.'],
  ['경험을 이력서로', '빈 이력서에 지금까지 만든 작품과 자기 역할을 한 줄씩 적어 본다.'],
  ['상담실 앞의 공고', '상담실 책상 위에 국내 실습 안내와 실리콘밸리 준비 안내가 놓여 있다. 오늘 어떤 자료를 먼저 읽을지 고를 수 있다.'],
  ['코딩 테스트를 보는 주', '채용 일정에서 코딩 테스트 날짜를 확인한다. 이전에 지원했다면 전형 단계도 살펴볼 수 있다.'],
  ['면접 준비', '프로젝트에 대해 질문을 받으면 어디서부터 설명할지 짧게 적어 본다.'],
  ['결과를 확인하는 주', '졸업 일정이 가까워졌다. 지원 결과가 나온 자리도, 더 준비할 일이 있는 자리도 있다.'],
]

const narshaMilestones = [
  [narshaTopicEvent, '1학년 나르샤 주제'],
  [yearTwoNarshaEvent, '2학년 나르샤 주제'],
  [teamGoalEvent, '2학년 나르샤 멘토링'],
  [narshaExpoEvent, 'ICT융합 엑스포'],
  [softwaveEvent, '소프트웨이브'],
] as const

function schoolPeriod(year: number, turn: number): [string, string] {
  const slot = year === 3 ? (turn < 6 ? 0 : turn < 12 ? 1 : turn < 16 ? 2 : turn < 20 ? 3 : turn < 24 ? 4 : 5)
    : turn < 8 ? 0 : turn < 14 ? 1 : turn < 20 ? 2 : 3
  const [title, text] = checkpoints[(year - 1) * 4 + slot] ?? ['학교생활', '오늘의 일정을 확인한다.']
  return [title, text]
}

function scene(game: DemoGame, suffix: string): [string, string] | null {
  const { phase, year, turn } = game.view
  const previous = game.history.at(-1)
  if (suffix.startsWith('/events/') && previous?.admission?.protection) {
    const result = previous.admission.passed ? '일반전형에서 통과했다.' : '일반전형에서도 탈락했다.'
    return ['특별전형 보호막 발동', `특별전형에서 탈락했다. 이미 제출한 결과를 일반전형 기준으로 확인했다. ${result}`]
  }
  if (suffix.startsWith('/events/') && previous?.dailyEvent) {
    const daily = previous.dailyEvent
    return [daily.title, `${daily.situation} ${daily.choiceLabel}.`]
  }
  if (suffix.startsWith('/events/') && previous?.stressEvent && phase !== 'ENDED') {
    const considering = previous.resultCode === 'WITHDRAWAL_CONSIDERED'
    return [considering ? '자퇴를 고민한 날' : '학교생활의 부담을 나누다',
      `${previous.stressEvent.choiceLabel}. ${considering ? '학교를 떠날지, 도움을 받으며 계속 다닐지 차분히 생각해 보기로 했다.' : '부담을 조금 덜고 다음 일정을 다시 살펴봤다.'}`]
  }
  if (previous?.vacation) {
    const season = previous.vacation.season === 'SUMMER' ? '여름방학' : '겨울방학'
    switch (previous.resultCode) {
      case 'CAMP_STARTED': return [`${season} · 전공강화캠프 신청`, '방학 중 두 차례 전공 실습을 이어가기로 했다.']
      case 'CAMP_DAY_DONE': return [`${season} · 전공강화캠프 1턴`, '실습에서 만든 기능을 테스트하고 다음에 고칠 부분을 적었다.']
      case 'CAMP_COMPLETED': return [`${season} · 전공강화캠프 수료`, '두 번째 실습에서 전날의 문제를 고치고 결과물을 정리했다.']
      case 'VACATION_RESTED': return [`${season} · 재충전`, '방학 동안 쉬면서 다음 일정에 필요한 체력을 회복했다.']
    }
  }
  if (previous?.semesterFocus && suffix === '/events/TERM_FOCUS/choices') {
    return ['이번 학기에 집중할 것', `이번 학기에는 ‘${previous.semesterFocus.label}’에 꾸준히 시간을 쓰기로 했다.`]
  }
  if (previous?.narsha?.status === 'YEAR_ONE_COMPLETE' && suffix === '/turns/advance') {
    return ['1학년 나르샤 마무리', `‘${previous.narsha.topicLabel}’의 결과물과 검증 기록을 정리하며 1학년 프로젝트를 마무리했다. ${(previous.setbacks ?? []).map((item) => item.reason).join(' ')}`]
  }
  if (suffix === '/turns/advance' && (previous?.routine || previous?.fieldLearning || previous?.assessments)) {
    const scene = schoolScenes.find((item) => item.year === previous.year && item.turn === previous.turn)
    const base = scene ? [scene.title, scene.premise] : schoolPeriod(previous.year, previous.turn)
    const field = previous.fieldLearning && fieldLearningScene(previous.fieldLearning)
    const season = previous.vacationOpening?.season === 'SUMMER' ? '여름방학' : previous.vacationOpening ? '겨울방학' : null
    const facts = [base[1], ...(previous.setbacks ?? []).map((item) => item.reason),
      ...(previous.stressLoad?.sources ?? []).map((item) => item.reason)]
    if (season) facts.push(`${season}이 시작됐다. 전공강화캠프에 참가하거나 쉴 수 있다.`)
    for (const result of previous.assessments ?? []) {
      const change = result.gradeAfter - result.gradeBefore
      facts.push(`${result.title} 결과를 받았다. 성적은 ${change > 0 ? '올랐다' : change < 0 ? '떨어졌다' : '그대로였다'}.`)
    }
    if (previous.routine?.friendContact) facts.push('민서와 수업 이야기를 나눴다.')
    for (const { action, result, subject } of previous.routine?.activities ?? []) {
      switch (action) {
        case 'PROJECT_START': facts.push(`${subject} 프로젝트를 시작했다.`); break
        case 'PROJECT_WORK': facts.push(`${subject} 작업을 진행했다.${result === 'COMPLETE' ? ' 작품을 완성했다.' : result === 'FAILED' ? ' 목표 완성도에 미치지 못했다.' : ''}`); break
        case 'PORTFOLIO_ADD': facts.push(`${subject} 작업을 포트폴리오에 정리했다.`); break
        case 'COURSE_ENROLL': facts.push(`${subject} 방과후 과정에 등록했다.`); break
        case 'ATTEND': facts.push(`${subject} 준비 수업에 출석했다.`); break
        case 'EXAM': facts.push(`${subject} 시험에 응시했다. 결과는 ${result === 'PASSED' ? '합격' : '불합격'}이었다.`); break
        case 'RESUME': facts.push('학교에서 쌓은 경험을 이력서에 정리했다.'); break
        case 'CODING_PREP': facts.push('코딩 테스트에 대비해 연습 문제를 풀었다.'); break
        case 'INTERVIEW_PREP': facts.push('면접을 대비해 맡았던 역할을 설명해 봤다.'); break
      }
    }
    if (field) facts.push(field[1])
    return [field?.[0] ?? (season ? `${season}의 시작` : base[0]), facts.join(' ')]
  }
  if (suffix === '/actions') {
    if (previous?.subjectCode === 'SOCIAL') return ['친구와 나눈 이야기', '민서와 오늘 막혔던 과제를 이야기했다. 다음에는 서로의 코드를 한 번씩 보기로 했다.']
    if (previous?.subjectCode === 'EXAM') return ['시험을 치른 날', '시험 결과를 확인하고 수첩에 이번 응시를 기록했다.']
    if (previous?.subjectCode === 'PROJECT_WORK' && ['COMPLETE', 'FAILED'].includes(previous.resultCode)) {
      return ['팀 프로젝트를 정리하다', '작동하는 부분과 다시 확인할 부분을 정리한다. 오늘까지 진행한 작업은 프로젝트 기록에 남았다.']
    }
    return null
  }
  if (phase === 'ENDED' && game.ending) return [game.view.endReason === 'WITHDRAWAL' ? '다음 배움을 준비하며' : year ? '교문을 나서며' : '전형을 마치며', game.ending.body]
  if (phase === 'GRAD_READY') return ['졸업을 앞둔 마지막 주', '재학 일정이 끝났다. 지금까지의 기록을 보고 졸업 이후에 할 일을 정한다.']
  if (phase === 'CAREER_PICK') return ['관심 있는 일을 고르는 날', '비포스쿨에서 해 본 일을 떠올린다. 다음에는 어떤 일을 더 깊게 해 보고 싶은지 고를 차례다.']
  if (suffix === '/events/BS01/choices' && previous?.subjectCode === 'BS01') {
    const stop = beforeSchool[0].choices.find((choice) => choice.code === previous.choiceCode)
    if (stop?.description) return [`비포스쿨 첫날 · ${stop.label}`, stop.description]
  }
  if (suffix === '/events/Y201/choices' || suffix === '/events/Y301/choices') {
    const major = suffix === '/events/Y201/choices'
    const selected = (major ? majorEvent : trackEvent).choices.find((choice) => choice.code === previous?.choiceCode)
    if (selected) return [`결정 · ${selected.label}`, major
      ? `2학년 학과로 ${selected.label}를 골랐다. 새 학과에서 해 보고 싶은 과제를 떠올린다.`
      : `3학년 수업으로 ${selected.label}를 골랐다. 졸업 전 어떤 작품을 남길지 생각해 본다.`]
  }
  if (previous?.subjectCode === 'J01' && suffix.startsWith('/events/')) {
    const field = game.fieldLearning && fieldLearningScene(game.fieldLearning)
    if (field) return field
  }
  if (previous?.narsha) {
    const milestone = narshaMilestones.find(([event]) => suffix === `/events/${event.code}/choices`)
    if (milestone) {
      const [event, title] = milestone
      const { topicLabel, rank } = previous.narsha
      const description = previous.choiceCode === 'NARSHA_CUSTOM'
        ? `직접 입력한 주제 ‘${topicLabel}’로 팀 프로젝트를 시작했다.`
        : event.choices.find((item) => item.code === previous.choiceCode)?.description ?? ''
      if (event === narshaExpoEvent && rank != null) {
        return [`2학년 나르샤 결과 · ${rank}위`, `${description} 팀 순위는 ${rank}위였다.${rank <= 3 ? ' 소프트웨이브에 참가할 기회가 생겼다.' : ''}`]
      }
      return [`${title} · ${topicLabel}`, `팀의 프로젝트 주제는 ‘${topicLabel}’이다. ${description}`.trim()]
    }
  }
  if (suffix === '/events/S103/choices') {
    const selected = clubEvent.choices.find((item) => item.code === previous?.choiceCode)
    if (selected?.description) return [`결정 · ${selected.label}`, selected.description]
  }
  if (previous?.subjectCode === 'S101' && suffix.startsWith('/events/')) {
    if (previous.choiceCode === 'PAIR_DEBUG') return ['민서와 찾은 단서', '오류 메시지를 함께 한 줄씩 읽었다. 각자 발견한 단서를 비교하니 혼자서는 지나쳤을 부분이 보였다.']
    return ['실습 노트의 표시', '오류가 난 위치를 실습 노트에 표시했다. 이어서 살펴볼 부분을 적어 둔다.']
  }
  if (suffix.startsWith('/applications')) return ['채용 전형 기록', '채용 전형에서 이번 단계의 결과를 확인했다. 다음 일정은 지원 기록에서 볼 수 있다.']
  if (phase.startsWith('ADMISSION') || phase === 'BEFORE_SCHOOL') {
    const current = events(game)[0]
    if (!current) return null
    if (current.code === 'AD00') {
      return ['접수 창을 열다', '지원 페이지에는 네 전형이 보이고 아직 아무 칸도 선택하지 않았다. 어떤 지원서를 쓸지 잠시 생각한다.']
    }
    if (current.code === 'AD05') return ['학교 면접', '면접관이 내 지원서를 내려놓고 질문을 건넸다. 이제 내가 겪은 일을 내 말로 답할 차례다.']
    const title = phase === 'BEFORE_SCHOOL' ? `비포스쿨 ${game.view.beforeSchoolDay}일의 기록` : '전형을 준비하는 날'
    return [title, current.situation]
  }
  if (suffix === '' || suffix === '/turns/advance' || suffix === '/career-preference' || suffix.startsWith('/events/')) {
    return schoolPeriod(year, turn)
  }
  return null
}

function tone(game: DemoGame, suffix: string): StoryEntry['tone'] {
  if (!suffix) return 'NEUTRAL'
  const outcome = game.history.at(-1)
  if (outcome?.setbacks?.length) return 'NEGATIVE'
  const result = outcome?.resultCode ?? ''
  if (['ADMISSION_REJECTED', 'REJECTED', 'FAILED', 'EXPIRED', 'WITHDRAWAL_CONSIDERED'].includes(result)) return 'NEGATIVE'
  if (['ADMISSION_PASSED', 'PASSED', 'COMPLETE', 'OFFERED'].includes(result)
    || outcome?.fieldLearning?.status === 'COMPLETED') return 'POSITIVE'
  if (suffix === '/events/J01/choices' && game.fieldLearning?.status === 'NOT_SELECTED') return 'NEGATIVE'
  for (const activity of outcome?.routine?.activities ?? []) {
    if (activity.result === 'PASSED' || activity.result === 'COMPLETE') return 'POSITIVE'
    if (activity.result === 'FAILED') return 'NEGATIVE'
  }
  return 'NEUTRAL'
}

export function appendDemoStory(game: DemoGame, entries: StoryEntry[], suffix: string) {
  const next = scene(game, suffix)
  if (!next) return
  const previous = game.history.at(-1)
  for (const activity of previous?.beforeSchoolActivities ?? []) {
    entries.push({ id: String(entries.length + 1), year: game.view.year, turn: game.view.turn,
      beforeSchoolDay: previous?.beforeSchoolDay, title: activity.title, text: activity.description,
      dialogue: [], source: 'MOCK', status: 'READY', tone: 'NEUTRAL' })
  }
  const period = (suffix === '/turns/advance' || previous?.vacation) && previous ? previous : game.view
  entries.push({ id: String(entries.length + 1), year: period.year, turn: period.turn, beforeSchoolDay: game.view.beforeSchoolDay,
    title: next[0], text: next[1], dialogue: [], source: 'MOCK', status: 'READY', tone: tone(game, suffix),
    assessments: suffix === '/turns/advance' ? previous?.assessments : undefined,
    setbacks: suffix === '/turns/advance' ? previous?.setbacks : undefined,
    stressLoad: suffix === '/turns/advance' ? previous?.stressLoad : undefined })
}
