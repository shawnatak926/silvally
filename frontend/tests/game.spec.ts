import { expect, test, type Page } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { applicationTurn, codingTurn, interviewTurn, resultTurn, semesterTurns, yearTurns } from '../src/game/schedule'

const schoolAnswer = '제가 대소고에 지원한 이유는 사람들이 쓰기 쉬운 앱을 개발하는 진로를 탐색하고 싶기 때문입니다. 자기소개서에는 팀 프로젝트에서 맡은 파이썬 입력 기능을 구현한 경험을 썼습니다. 문제가 생겼을 때 오류를 재현하고 로그를 비교해 원인을 찾았습니다. 테스트로 해결을 확인한 뒤 선생님의 피드백을 받아 수정했습니다. 입학 후에는 프로그래밍을 배우고 친구와 협업해 작은 서비스를 만들어 보겠습니다.'
const companyAnswer = '팀 프로젝트에서 프론트엔드 화면과 API 연결을 맡았습니다. React로 화면을 만들고 사용성을 확인했습니다. 오류를 재현하고 로그를 비교해 원인을 분석했습니다. 테스트로 결과를 검증해 응답 시간을 개선했습니다. 팀원과 인터페이스를 합의하고 리뷰 피드백을 반영했습니다. 이 직무에서도 서비스가 안정적으로 동작하도록 화면과 API 개발 경험을 활용하겠습니다.'

async function select(page: Page, label: string, submit = '선택 확정') {
  await page.getByRole('radio', { name: label, exact: true }).check()
  await page.getByRole('button', { name: submit, exact: true }).click()
}

function choiceHeading(page: Page, name: string) {
  return page.getByRole('region', { name: '지금 열린 선택' }).getByRole('heading', { name, exact: true })
}

async function enterBeforeSchool(page: Page) {
  await page.goto('/')
  await page.getByLabel('이름').fill('플레이어 A')
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('button', { name: '지원 시작' }).click()
  await expect(page.getByRole('meter', { name: '성적', exact: true })).toHaveAttribute('value', /^(6[0-9]|7[0-9]|80)$/)
  for (const label of [
    '일반전형', '코딩 활동을 앞세우기', '피드백을 받아 다시 쓰기',
    '프로젝트 역할과 증거 정리', '파이썬 연습',
  ]) await select(page, label)
  await expect(page.getByRole('button', { name: '면접 답변 제출' })).toBeDisabled()
  await page.getByLabel('면접 답변').fill(schoolAnswer)
  await page.getByRole('button', { name: '면접 답변 제출' }).click()
  await expect(choiceHeading(page, '학교 탐색')).toBeVisible()
}

async function enterSchool(page: Page) {
  await enterBeforeSchool(page)
  await expect(page.getByRole('checkbox', { name: /둘러보기$/ })).toHaveCount(7)
  await page.getByRole('checkbox', { name: '프로젝트실 둘러보기' }).check()
  await page.getByRole('checkbox', { name: '축구장 둘러보기' }).check()
  await page.getByRole('button', { name: '탐색 마치기' }).click()
  await page.getByRole('checkbox', { name: '행사 안내 웹 만들기' }).check()
  await page.getByRole('checkbox', { name: '분류 데이터 체험' }).check()
  await page.getByRole('button', { name: '체험 마치기' }).click()
  for (const label of ['iOS 화면 배치', 'API 오류 해결', '화면 사용성 설명']) {
    await select(page, label)
  }
  await expect(choiceHeading(page, '희망 직무 선택')).toBeVisible()
  await expect(page.getByRole('radio')).toHaveCount(11)
  await page.getByRole('radio', { name: /웹 개발자/ }).check()
  await page.getByRole('button', { name: '이 직무로 시작' }).click()
  await expect(page.getByText('1학년 · 1턴', { exact: true })).toBeVisible()
  await setFocus(page, '희망 직무 연습')
  await page.getByRole('button', { name: '일시 정지', exact: true }).click()
}

test('모바일에서 장소와 전공 체험을 두 가지씩 선택하고 각각 기록한다', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 780 })
  await enterBeforeSchool(page)
  const panel = page.getByRole('region', { name: '지금 열린 선택' })
  const journal = page.getByRole('region', { name: '인생 기록' })
  const finish = panel.getByRole('button', { name: '탐색 마치기' })
  const makerRoom = panel.getByRole('checkbox', { name: '메이커실 둘러보기' })
  const field = panel.getByRole('checkbox', { name: '축구장 둘러보기' })
  await expect(finish).toBeDisabled()
  await expect(panel.getByText(/3D 프린터와 출력물/)).toBeVisible()
  await makerRoom.check()
  await expect(finish).toBeDisabled()
  await panel.getByRole('checkbox', { name: '강당 둘러보기' }).check()
  await expect(field).toBeDisabled()
  await makerRoom.uncheck()
  await expect(field).toBeEnabled()
  await makerRoom.check()
  await expect(panel.getByText('선택 2 / 2')).toBeVisible()
  await expect(choiceHeading(page, '학교 탐색')).toBeVisible()
  await expect(journal.getByText('비포스쿨 첫날 · 강당 둘러보기')).toHaveCount(0)
  await finish.click()
  await expect(choiceHeading(page, '전공 체험')).toBeVisible()
  await expect(panel.getByRole('checkbox')).toHaveCount(8)
  await panel.getByRole('checkbox', { name: '급식 메뉴 API 만들기' }).check()
  await expect(panel.getByRole('button', { name: '체험 마치기' })).toBeDisabled()
  await panel.getByRole('checkbox', { name: '미니게임 조작 만들기' }).check()
  await panel.getByRole('button', { name: '체험 마치기' }).click()
  await expect(choiceHeading(page, '기초 프로그래밍')).toBeVisible()
  await expect(panel.getByRole('radio')).toHaveCount(6)
  await page.reload()
  for (const title of ['비포스쿨 첫날 · 메이커실 둘러보기', '비포스쿨 첫날 · 강당 둘러보기',
    '전공 체험 · 급식 메뉴 API 만들기', '전공 체험 · 미니게임 조작 만들기']) {
    await expect(journal.getByText(title, { exact: true })).toBeVisible()
  }
})

async function resolveStress(page: Page) {
  for (let attempt = 0; attempt < 3; attempt++) {
    const choices = page.locator('[data-event-code="STRESS_SUPPORT"], [data-event-code="SCHOOL_DOUBTS"]')
    if (!await choices.isVisible()) return
    await choices.getByRole('button').first().click()
    await expect(page.getByText('저장됨', { exact: true })).toBeVisible()
  }
}

async function endTurn(page: Page) {
  await resolveStress(page)
  const advanced = page.waitForResponse((response) => response.url().endsWith('/turns/advance') && response.request().method() === 'POST')
  await page.getByRole('button', { name: '턴 종료', exact: true }).click()
  const confirmation = page.getByRole('dialog', { name: '턴을 끝낼까요?' })
  if (await confirmation.isVisible()) {
    await confirmation.getByRole('button', { name: '다음 턴으로' }).click()
  }
  await advanced
  await expect(page.getByText('저장됨', { exact: true })).toBeVisible()
  await resolveStress(page)
}

async function setFocus(page: Page, focus: string) {
  const panel = page.getByRole('region', { name: '이번 학기에 집중할 것' })
  await panel.getByRole('radio', { name: focus, exact: true }).check()
  await panel.getByRole('button', { name: '이번 학기 집중 정하기' }).click()
  await expect(panel.getByText(focus, { exact: true })).toBeVisible()
}

async function decide(page: Page, option: string) {
  const dialog = page.getByRole('region', { name: '중대 결정' })
  const radio = dialog.getByRole('radio', { name: option, exact: true })
  if (await radio.count()) {
    await radio.check()
    await dialog.getByRole('button', { name: '이 주제로 시작' }).click()
  } else await dialog.getByRole('button', { name: option, exact: true }).click()
}

async function settleSchoolChoices(page: Page, year: number, turn: number, major: string, track: string) {
  await resolveStress(page)
  if (year === 2 && turn === 1) await decide(page, major)
  if (year === 3 && turn === 1) await decide(page, track)
  if ((year > 1 && turn === 1) || turn === semesterTurns + 1) await setFocus(page, '희망 직무 연습')
  if (year === 1 && turn === 8) await decide(page, '기숙사 생활 통합 시스템')
  if (year === 2 && turn === 2) {
    await decide(page, 'AI 영상 개인정보 보호 시스템')
    await decide(page, '멘토 피드백으로 핵심 기능 다듬기')
  }
  if (year === 2 && turn === 22) {
    await decide(page, '부스에서 방문객에게 설명하기')
    await expect(page.getByRole('region', { name: '나르샤 프로젝트 기록' })).toContainText(/[1-8]위/)
  }
  if (year === 2 && turn === 23) {
    const qualification = page.getByRole('region', { name: '나르샤 프로젝트 기록' }).getByText('소프트웨이브 참가 가능')
    if (await qualification.isVisible()) await decide(page, '소프트웨이브에서 결과물 전시하기')
  }
}

async function advanceRegularTurn(page: Page, turn: number) {
  await endTurn(page)
  if (turn === semesterTurns || turn === yearTurns) await decide(page, '방학 동안 쉬기')
}

async function reachCounseling(page: Page, major: string, track: string) {
  for (let year = 1; year <= 3; year++) {
    for (let turn = 1; turn <= (year === 3 ? applicationTurn - 1 : yearTurns); turn++) {
      await expect(page.getByText(`${year}학년 · ${turn}턴`, { exact: true })).toBeVisible()
      await settleSchoolChoices(page, year, turn, major, track)
      await advanceRegularTurn(page, turn)
    }
  }
  await expect(choiceHeading(page, '공고를 함께 읽어 볼까요?')).toBeVisible()
  const projects = page.getByRole('region', { name: '나르샤 프로젝트 기록' })
  await expect(projects.getByText('1학년 나르샤', { exact: true })).toBeVisible()
  await expect(projects.getByText('2학년 나르샤', { exact: true })).toBeVisible()
  await expect(projects.getByText('기숙사 생활 통합 시스템', { exact: true })).toBeVisible()
  await expect(projects.getByText('AI 영상 개인정보 보호 시스템', { exact: true })).toBeVisible()
}

async function finishWithoutOffer(page: Page) {
  for (let turn = applicationTurn; turn <= yearTurns; turn++) {
    await expect(page.getByText(`3학년 · ${turn}턴`, { exact: true })).toBeVisible()
    if (turn === semesterTurns + 1) await setFocus(page, '영어와 글로벌 진로')
    await advanceRegularTurn(page, turn)
  }
  await expect(choiceHeading(page, '졸업 후 진로')).toBeVisible()
  await select(page, '다시 취업 준비', '졸업 확정')
  await expect(page).toHaveURL(/\/ending\//)
  await expect(choiceHeading(page, '다음 지원을 준비합니다')).toBeVisible()
}

test('집중 목표를 유지하며 진행하고 캠프는 방학 안에서만 두 턴을 쓴다', async ({ page }) => {
  test.setTimeout(45_000)
  await page.setViewportSize({ width: 360, height: 780 })
  await enterSchool(page)
  await expect(page.getByText('추가 활동', { exact: true })).toHaveCount(0)
  await expect(page.getByText('1학기 · 1/12턴')).toBeVisible()
  await page.getByRole('button', { name: '자동 진행', exact: true }).click()
  await page.getByRole('button', { name: '룸메이트와 생활 규칙 정하기 · 1 AP', exact: true }).click()
  await expect(page.locator('.clock-label')).toHaveText('시간이 흐르는 중')
  await page.getByRole('button', { name: '다음 결정까지' }).click()
  const dialog = page.getByRole('region', { name: '중대 결정' })
  await expect(dialog.getByRole('heading', { name: '1학년 나르샤 · 주제 선택' })).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText('1학년 · 8턴', { exact: true })).toBeVisible()
  const declines = page.getByRole('list', { name: '능력치 하락 기록' })
  await expect(declines.first()).toContainText('연습 부족')
  await expect(declines.first()).toContainText(/백엔드 -[12]/)
  await expect(page.locator('.clock-label')).toContainText('일시 정지 상태')
  const id = new URL(page.url()).pathname.split('/').at(-1)
  const endpoint = `/api/v1/players/${id}`
  const current = (await (await page.request.get(endpoint)).json()).data
  for (const mode of ['AUTO', 'MANUAL']) {
    const rejected = await page.request.post(`${endpoint}/turns/advance`, {
      headers: { 'Idempotency-Key': randomUUID(), 'X-Expected-Version': String(current.version) }, data: { mode },
    })
    expect(rejected.status()).toBe(409)
  }
  await dialog.getByRole('radio', { name: '기타 · 직접 주제 입력' }).check()
  const startTopic = dialog.getByRole('button', { name: '이 주제로 시작' })
  await expect(startTopic).toBeDisabled()
  await dialog.getByLabel('프로젝트 주제', { exact: true }).fill('AI 실시간 수어 통역 시스템')
  await startTopic.click()
  await expect(page.getByRole('region', { name: '나르샤 프로젝트 기록' })).toContainText('1학년 나르샤')
  await expect(page.getByRole('region', { name: '나르샤 프로젝트 기록' })).toContainText('AI 실시간 수어 통역 시스템')
  await page.reload()
  await expect(page.getByRole('region', { name: '나르샤 프로젝트 기록' })).toContainText('AI 실시간 수어 통역 시스템')
  await expect(declines.first()).toContainText('연습 부족')
  for (let turn = 8; turn <= 12; turn++) await endTurn(page)
  await expect(dialog.getByRole('heading', { name: '여름방학' })).toBeVisible()
  await decide(page, '전공강화캠프 참가')
  await expect(page.getByRole('heading', { name: '전공강화캠프 · 1/2턴' })).toBeVisible()
  await endTurn(page)
  await expect(page.getByRole('heading', { name: '전공강화캠프 · 2/2턴' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('heading', { name: '전공강화캠프 · 2/2턴' })).toBeVisible()
  await expect(page.getByText('1학년 · 12턴', { exact: true })).toBeVisible()
  await endTurn(page)
  await expect(page.getByText('1학년 · 13턴', { exact: true })).toBeVisible()
  await expect(page.getByText('2학기 · 1/12턴')).toBeVisible()
  await setFocus(page, '영어와 글로벌 진로')
  const journal = page.getByRole('list', { name: '이전 장면' })
  await expect(journal.getByRole('heading', { name: '여름방학 · 전공강화캠프 수료' })).toBeVisible()
  await expect(journal.getByRole('list', { name: '학교 평가 결과' }).filter({ hasText: '1학기 기말고사' })).toHaveCount(1)
  await expect(page.getByRole('list', { name: '스트레스 변화 이유' }).filter({ hasText: '중간고사' }).first()).toContainText('스트레스 +')
})

test('특별전형 보호막이 같은 서류를 일반전형으로 판정하고 다음 단계로 이어진다', async ({ page, request }) => {
  let id: string | undefined
  for (let i = 0; i < 20 && !id; i++) {
    const response = await request.post('/api/v1/players', {
      headers: { 'Idempotency-Key': randomUUID() },
      data: { name: '특별전형 보호막 검증' },
    })
    expect(response.status()).toBe(201)
    const { data } = await response.json()
    if (data.admissionEligibility.includes('MEISTER')) id = data.id
  }
  expect(id).toBeDefined()
  await page.setViewportSize({ width: 360, height: 780 })
  await page.goto(`/play/${id}`)
  const panel = page.getByRole('region', { name: '지금 열린 선택' })
  await expect(panel.getByText('게임 내 지원 자격 추첨 70%')).toBeVisible()
  await expect(panel.getByText('게임 내 지원 자격 추첨 20%')).toBeVisible()
  await expect(panel.getByText('게임 내 지원 자격 추첨 50%')).toBeVisible()
  for (const choice of ['마이스터인재', '코딩 활동을 앞세우기', '초안 제출', '간단한 기록 제출']) await select(page, choice)
  await expect(choiceHeading(page, '코딩 실습 준비')).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: /보호막 발동.*같은 결과로 일반전형 통과/ })).toBeVisible()
  await page.reload()
  await expect(choiceHeading(page, '코딩 실습 준비')).toBeVisible()
  await select(page, '파이썬 연습')
  await page.getByLabel('면접 답변').fill(schoolAnswer)
  await page.getByRole('button', { name: '면접 답변 제출' }).click()
  await expect(choiceHeading(page, '학교 탐색')).toBeVisible()
  await expect(page).toHaveURL(new RegExp(`/play/${id}$`))
})

test('작성 중인 장면을 보여주고 새 장면이 도착해도 입학 선택을 유지한다', async ({ page }) => {
  let ready = false
  await page.route('**/api/v1/players/*/story', async (route) => {
    const pending = !ready
    await route.fulfill({ json: { success: true, error: null, data: { entries: [{
      id: 'admission', year: 0, turn: 0, title: '교문 앞에서',
      text: pending ? '교문 너머에 학교가 보인다.' : '담장 너머로 실습실의 불이 보인다.',
      dialogue: pending ? [] : [{ speaker: '선생님', text: '들어와도 좋아요.' }],
      source: pending ? 'FALLBACK' : 'AI', status: pending ? 'PENDING' : 'READY',
    }] } } })
  })
  await page.goto('/')
  await page.getByLabel('이름').fill('입학생')
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('button', { name: '지원 시작' }).click()
  await expect(page.getByText('교문 너머에 학교가 보인다.')).toBeVisible()
  await page.getByRole('radio', { name: '일반전형' }).check()
  ready = true
  await expect(page.getByText('담장 너머로 실습실의 불이 보인다.')).toBeVisible()
  await expect(page.getByText('들어와도 좋아요.')).toBeVisible()
  await expect(page.getByRole('radio', { name: '일반전형' })).toBeChecked()
})

test('장면 서버가 오류를 반환해도 입학 선택을 저장할 수 있다', async ({ page }) => {
  await page.route('**/api/v1/players/*/story', (route) => route.fulfill({ status: 503, json: {} }))
  await page.goto('/')
  await page.getByLabel('이름').fill('장면 오류 체험')
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('button', { name: '지원 시작' }).click()
  await expect(page.getByRole('button', { name: '이야기 다시 불러오기' })).toBeVisible()
  await select(page, '일반전형')
  await expect(choiceHeading(page, '중학교 활동 기록')).toBeVisible()
  await expect(page.getByRole('radio', { name: '코딩 활동을 앞세우기' })).toBeEnabled()
})

test('PC에서 국내 실습 참가를 기록하고 졸업한다', async ({ page }) => {
  test.setTimeout(60_000)
  await enterSchool(page)
  const photo = page.getByRole('img', { name: /대소고 본관과 운동장/ })
  await expect(photo).toBeVisible()
  expect(await photo.evaluate((image: HTMLImageElement) => [image.naturalWidth, image.naturalHeight])).toEqual([1059, 595])
  await page.getByRole('button', { name: '사진 보기' }).click()
  const campus = page.getByRole('dialog', { name: '대소고 캠퍼스 사진' })
  await expect(campus.getByRole('img', { name: /대소고 본관과 운동장/ })).toBeVisible()
  await campus.getByRole('button', { name: '닫기' }).click()
  await page.getByRole('button', { name: '학생 수첩' }).click()
  await expect(page.getByRole('dialog', { name: '학생 수첩' })).toBeVisible()
  await page.getByRole('button', { name: '닫기', exact: true }).click()
  await reachCounseling(page, '소프트웨어개발과', '프론트엔드')
  const endpoint = '**/api/v1/players/*/events/J01/choices'
  const pendingResponse = page.waitForResponse(endpoint)
  await page.getByRole('button', { name: '국내 현장실습 신청하기' }).click()
  const response = await pendingResponse
  const { data } = await response.json()
  expect(data).toMatchObject({
    spentAp: 0, statDelta: {}, resultCode: 'FIELD_LEARNING_RECORDED',
    details: { fieldLearningRoute: 'DOMESTIC', fieldLearning: { route: 'DOMESTIC', status: 'SELECTED' } },
  })
  await expect(page.getByRole('status').filter({ hasText: '국내 현장실습 · 참가 확정' })).toBeVisible()
  const headers = response.request().headers()
  const replay = await page.request.post(response.url(), {
    headers: { 'idempotency-key': headers['idempotency-key'], 'x-expected-version': headers['x-expected-version'] },
    data: { choiceCode: 'DOMESTIC' },
  })
  expect(await replay.json()).toEqual(await response.json())
  const duplicate = await page.request.post(response.url(), {
    headers: { 'idempotency-key': crypto.randomUUID(), 'x-expected-version': String(data.player.version) },
    data: { choiceCode: 'SILICON_VALLEY' },
  })
  expect(duplicate.status()).toBe(409)
  await page.getByRole('button', { name: '학생 수첩' }).click()
  await expect(page.getByRole('dialog', { name: '학생 수첩' })).toContainText('국내 현장실습 · 참가 확정')
  await page.getByRole('button', { name: '닫기', exact: true }).click()
  await finishWithoutOffer(page)
  await page.reload()
  await expect(page.getByRole('region', { name: '지금 열린 선택' })).toContainText('국내 현장실습 · 참가 완료')
})

test('360px 터치 화면에서 전형을 고르고 새로고침 후 이어 한다', async ({ browser }) => {
  const context = await browser.newContext({ viewport: { width: 360, height: 780 }, isMobile: true, hasTouch: true })
  const page = await context.newPage()
  await page.goto('/')
  await page.getByLabel('이름').fill('모바일 학생')
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('button', { name: '지원 시작' }).tap()
  await page.getByText('일반전형', { exact: true }).tap()
  await page.getByRole('button', { name: '선택 확정' }).tap()
  await expect(choiceHeading(page, '중학교 활동 기록')).toBeVisible()
  await page.reload()
  await expect(choiceHeading(page, '중학교 활동 기록')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await context.close()
})

test('360px 화면에서 실리콘밸리 현장학습에 지원하고 참가 기록을 확인한다', async ({ browser }) => {
  const context = await browser.newContext({ viewport: { width: 360, height: 780 }, isMobile: true, hasTouch: true })
  try {
    const page = await context.newPage()
    const response = await page.request.post('/api/v1/players', {
      headers: { 'Idempotency-Key': randomUUID() },
      data: { demoScenario: 'COUNSELING' },
    })
    expect(response.status()).toBe(201)
    const { data } = await response.json()
    await page.goto(`/play/${data.id}`)
    await expect(page.getByText('3학년 · 12턴', { exact: true })).toBeVisible()
    await expect(page.getByText('게임 속 진로 체험 · 선택 이벤트')).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.getByRole('button', { name: '실리콘밸리 현장학습 지원하기' }).tap()
    await expect(page.getByRole('status').filter({ hasText: '실리콘밸리 현장학습 · 참가 확정' })).toBeVisible()
    await page.reload()
    await expect(choiceHeading(page, '공고를 함께 읽어 볼까요?')).toHaveCount(0)
    await expect(page.getByRole('heading', { name: '기업 지원' })).toBeVisible()
    await expect(page.getByRole('button', { name: '지원 · 1 AP' })).toHaveCount(6)
    await expect(page.getByRole('button', { name: '다음 결정까지' })).toBeDisabled()
    await finishWithoutOffer(page)
    const graduation = page.getByRole('region', { name: '지금 열린 선택' })
    await expect(graduation).toContainText('실리콘밸리 현장학습 · 참가 완료')
    await expect(graduation).not.toContainText('국내 현장실습 · 참가 완료')
  } finally {
    await context.close()
  }
})

test('시작 화면에서 상담 바로 체험 없이 기존 회차를 이어서 한다', async ({ page }) => {
  await page.goto('/')
  await page.getByLabel('이름').fill('기존 회차')
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('button', { name: '지원 시작' }).click()
  await expect(page).toHaveURL(/\/play\//)
  const savedGame = page.url()
  await page.goto('/')
  await expect(page.getByRole('button', { name: '3학년 상담 바로 체험' })).toHaveCount(0)
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('link', { name: '이어서 하기' }).click()
  await expect(page).toHaveURL(savedGame)
  await expect(choiceHeading(page, '지원 전형 선택')).toBeVisible()
})

test('응답이 유실되면 같은 명령 키와 버전으로 복구한다', async ({ page }) => {
  await page.goto('/')
  await page.getByLabel('이름').fill('재시도 학생')
  await page.getByRole('radio', { name: '매운맛', exact: true }).check()
  await page.getByRole('button', { name: '지원 시작' }).click()
  await expect(page.getByRole('radio', { name: '일반전형' })).toBeVisible()
  const headers: Record<string, string>[] = []
  await page.route('**/api/v1/players/*/events/AD00/choices', async (route) => {
    headers.push(route.request().headers())
    if (headers.length === 1) {
      await route.fetch()
      await route.abort('failed')
    } else {
      await route.continue()
    }
  })
  await select(page, '일반전형')
  await expect(page.getByRole('button', { name: '같은 요청 다시 보내기' })).toBeVisible()
  await page.reload()
  await page.getByRole('button', { name: '같은 요청 다시 보내기' }).click()
  await expect(choiceHeading(page, '중학교 활동 기록')).toBeVisible()
  expect(headers).toHaveLength(2)
  expect(headers[0]['idempotency-key']).toMatch(/^[a-f0-9-]{36}$/)
  expect(headers[0]['idempotency-key']).toBe(headers[1]['idempotency-key'])
  expect(headers[0]['x-expected-version']).toBe(headers[1]['x-expected-version'])
})

async function prepareRecruitmentCondition(page: Page) {
  // Recovery choices vary by run. Prepare through real commands rather than assuming identical health.
  const id = new URL(page.url()).pathname.split('/').at(-1)
  const base = `/api/v1/players/${id}`
  for (let attempt = 0; attempt < 3; attempt++) {
    const { data: player } = await (await page.request.get(base)).json()
    if (player.stats.H >= 40 && player.stats.S < 55) break
    expect(player.remainingAp).toBeGreaterThan(1)
    const response = await page.request.post(`${base}/actions`, {
      headers: { 'idempotency-key': randomUUID(), 'x-expected-version': String(player.version) },
      data: { actionCode: 'REST' },
    })
    expect(response.ok()).toBeTruthy()
  }
  await page.reload()
}

test('학기 집중과 정규 생활을 이어가고 72턴을 마쳐 채용 제안을 수락한다', async ({ page }) => {
  test.setTimeout(65_000)
  await enterSchool(page)
  await reachCounseling(page, '소프트웨어개발과', '프론트엔드')
  await page.getByRole('button', { name: '국내 현장실습 신청하기' }).click()
  await page.locator('.action-row').filter({ has: page.getByText('웹서비스 개발팀', { exact: true }) })
    .getByRole('button', { name: '지원 · 1 AP' }).click()
  await expect(page.getByText('웹서비스 개발팀 · 서류 통과')).toBeVisible()
  await advanceRegularTurn(page, applicationTurn)
  await setFocus(page, '희망 직무 연습')

  for (let turn = applicationTurn + 1; turn < codingTurn; turn++) await endTurn(page)
  await prepareRecruitmentCondition(page)
  await page.getByRole('button', { name: '코딩 테스트 · 1 AP' }).click()
  await expect(page.getByText('웹서비스 개발팀 · 코딩 통과')).toBeVisible()
  for (let turn = codingTurn; turn < interviewTurn; turn++) await endTurn(page)
  await prepareRecruitmentCondition(page)
  await page.getByRole('button', { name: '면접 · 1 AP' }).click()
  await page.getByLabel('면접 답변').fill(companyAnswer)
  await page.getByRole('button', { name: '면접 답변 제출' }).click()
  await expect(page.getByText('웹서비스 개발팀 · 면접 통과')).toBeVisible()
  for (let turn = interviewTurn; turn < resultTurn; turn++) await endTurn(page)
  await page.getByRole('button', { name: '결과 확인 · 1 AP' }).click()
  await expect(page.getByText('웹서비스 개발팀 · 채용 제안')).toBeVisible()
  await advanceRegularTurn(page, resultTurn)

  await select(page, '웹서비스 개발팀 제안 수락', '졸업 확정')
  await expect(choiceHeading(page, '첫 출근을 준비합니다')).toBeVisible()
  await expect(page.getByText('자격증 · 정보처리기능사')).toBeVisible()
  await expect(page.getByText('프로젝트 · 교내 행사 페이지')).toBeVisible()
  await expect(page.getByRole('region', { name: '지금 열린 선택' })).toContainText('국내 현장실습 · 참가 완료')
})
