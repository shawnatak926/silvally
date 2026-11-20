import { expect, test, type Page } from '@playwright/test'
import { createGame, events, execute } from '../demo/game'
import { calendar } from '../demo/data'

test.describe.configure({ mode: 'parallel' })
test.setTimeout(45000)

async function openMild(page: Page) {
  await page.goto('/')
  await page.getByLabel('이름', { exact: true }).fill('자동 선택 검증')
  await page.getByRole('button', { name: '학교생활 시작', exact: true }).click()
  await expect(page.getByRole('heading', { name: '희망 직무 선택' })).toBeVisible()
  await expect(page.getByRole('progressbar', { name: '자동 선택까지 남은 시간' })).toBeVisible()
}

for (const width of [360, 1280]) {
  test(`${width}px 선택한 직무는 입력이 멈춘 뒤 10초에 한 번만 확정한다`, async ({ page }) => {
    await page.setViewportSize({ width, height: 850 })
    await openMild(page)
    let sent = 0
    page.on('request', request => { if (request.method() === 'PUT' && request.url().endsWith('/career-preference')) sent++ })
    const chosen = page.getByRole('radio').last()
    const role = await chosen.inputValue()
    await chosen.check()
    await page.waitForTimeout(6000)
    expect(Number(await page.getByRole('progressbar', { name: '자동 선택까지 남은 시간' }).getAttribute('value'))).toBeGreaterThan(3500)
    await page.mouse.wheel(0, 80)
    await page.waitForTimeout(9000)
    expect(sent).toBe(0)
    await expect(page.getByRole('heading', { name: '희망 직무 선택' })).toBeVisible()
    const saved = page.waitForResponse(r => r.request().method() === 'PUT' && r.url().endsWith('/career-preference'))
    await page.waitForTimeout(1000)
    expect((await (await saved).json()).data.player.initialCareerCode).toBe(role)
    await expect(page.getByRole('region', { name: '이번 학기에 집중할 것' })).toBeVisible()
    expect(sent).toBe(1)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  })
}

test('미선택 시에는 허용 직무를 고르고 자동 선택 끄기·켜기가 적용된다', async ({ page }) => {
  await openMild(page)
  const allowed = await page.getByRole('radio').evaluateAll(inputs => inputs.map(input => (input as HTMLInputElement).value))
  await page.getByRole('button', { name: '자동 선택 끄기' }).click()
  await page.waitForTimeout(12000)
  await expect(page.getByRole('heading', { name: '희망 직무 선택' })).toBeVisible()
  await page.getByRole('button', { name: '자동 선택 켜기' }).click()
  const saved = page.waitForResponse(r => r.request().method() === 'PUT' && r.url().endsWith('/career-preference'))
  await page.waitForTimeout(10000)
  expect(allowed).toContain((await (await saved).json()).data.player.initialCareerCode)
})

test('수첩을 열면 멈추고 닫은 뒤 10초를 다시 세며 수동 확정은 중복하지 않는다', async ({ page }) => {
  await openMild(page)
  await page.waitForTimeout(6000)
  await page.getByRole('button', { name: '전체 기록', exact: true }).click()
  await page.waitForTimeout(12000)
  await expect(page.getByRole('heading', { name: '희망 직무 선택' })).toBeVisible()
  await page.getByRole('button', { name: '닫기', exact: true }).click()
  await page.getByRole('radio').first().check()
  const saved = page.waitForResponse(r => r.request().method() === 'PUT' && r.url().endsWith('/career-preference'))
  await page.getByRole('button', { name: '이 직무로 시작' }).click()
  expect((await saved).status()).toBe(200)
  await expect(page.getByRole('region', { name: '이번 학기에 집중할 것' })).toBeVisible()
  await page.waitForTimeout(5000)
  await expect(page.getByRole('button', { name: '이번 학기 집중 정하기' })).toBeVisible()
})

test('학교 탐색은 선택한 장소를 유지하고 부족한 개수만 채운다', async ({ page }) => {
  const game = createGame({ name: '학교 탐색 검증', edition: 'SPICY' }, true)
  Object.assign(game.view, { phase: 'BEFORE_SCHOOL', beforeSchoolDay: 1 })
  const base = `/api/v1/players/${game.view.id}`
  const commands: string[][] = []
  await page.route('**/api/v1/**', async route => {
    const request = route.request(); const url = new URL(request.url()); const suffix = url.pathname.slice(base.length)
    let data: unknown
    if (request.method() === 'POST') {
      const body = request.postDataJSON(); commands.push(body.choiceCodes)
      game.view.version++; data = execute(game, suffix, body)
    } else if (url.pathname === base) data = game.view
    else if (suffix === '/events') data = events(game)
    else if (suffix === '/story') data = { entries: [] }
    else if (suffix === '/progress') data = { player: game.view, projects: [], qualifications: [], enrollments: [], snapshots: [], yearSummaries: [] }
    else if (url.pathname === '/api/v1/catalog/calendar') data = calendar
    else data = []
    await route.fulfill({ json: { success: true, data, error: null } })
  })
  await page.goto(`/play/${game.view.id}`)
  const chosen = page.getByRole('checkbox', { name: '메이커실 둘러보기' })
  await chosen.check()
  await page.waitForTimeout(10000)
  await expect(page.getByRole('heading', { name: '전공 체험', exact: true })).toBeVisible()
  expect(commands).toHaveLength(1)
  expect(commands[0]).toHaveLength(2)
  expect(new Set(commands[0]).size).toBe(2)
  expect(commands[0]).toContain('MAKER_ROOM')
})

test('자동 선택의 저장이 실패하면 같은 선택을 반복 전송하지 않는다', async ({ page }) => {
  await openMild(page)
  let attempts = 0
  await page.route('**/career-preference', async route => {
    if (route.request().method() !== 'PUT') return route.continue()
    attempts++
    await route.fulfill({ status: 400, json: { success: false, data: null,
      error: { code: 'VALIDATION_ERROR', message: '자동 선택 저장 실패 검증', details: {} } } })
  })
  await expect(page.getByRole('alert')).toContainText('자동 선택 저장 실패 검증', { timeout: 15000 })
  await page.waitForTimeout(11000)
  expect(attempts).toBe(1)
  await expect(page.getByText('자동 선택 대기', { exact: true })).toBeVisible()
})
