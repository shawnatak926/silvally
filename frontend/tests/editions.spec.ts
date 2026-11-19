import { expect, test } from '@playwright/test'
import { createGame, events, execute } from '../demo/game'
import { appendDemoStory } from '../demo/story'
import type { StoryEntry } from '../src/api/contracts'

for (const width of [360, 1280]) {
  test(`${width}px 순한맛은 1학년부터 시작하고 버전별로 이어한다`, async ({ page }) => {
    await page.setViewportSize({ width, height: 850 })
    await page.goto('/')
    await expect(page.getByRole('radio', { name: '순한맛', exact: true })).toBeChecked()
    await page.getByLabel('이름', { exact: true }).fill('첫 학기 학생')
    const created = page.waitForResponse(r => r.url().endsWith('/players') && r.request().method() === 'POST')
    await page.getByRole('button', { name: '학교생활 시작', exact: true }).click()
    const player = (await (await created).json()).data
    expect(player.edition).toBe('MILD'); expect(player.year).toBe(1); expect(player.turn).toBe(1)
    expect(player.phase).toBe('CAREER_PICK')
    const choices = page.getByRole('region', { name: '지금 열린 선택' })
    await expect(choices.getByRole('heading', { name: '희망 직무 선택' })).toBeVisible()
    await expect(choices.getByRole('radio')).toHaveCount(3)
    await expect(page.locator('#current-story')).not.toContainText('비포스쿨')
    await choices.getByRole('radio').first().check()
    await choices.getByRole('button', { name: '이 직무로 시작' }).click()
    await page.reload()
    await expect(page.getByRole('region', { name: '이번 학기에 집중할 것' })).toBeVisible()
    await page.goto('/')
    await expect(page.getByRole('link', { name: '이어서 하기' })).toHaveAttribute('href', `/play/${player.id}`)
    await page.getByRole('radio', { name: '매운맛', exact: true }).check()
    await expect(page.getByRole('link', { name: '이어서 하기' })).toHaveCount(0)
    await page.getByLabel('이름', { exact: true }).fill('입학부터 학생')
    await page.getByRole('button', { name: '지원 시작', exact: true }).click()
    await expect(page.getByRole('heading', { name: '지원 전형 선택' })).toBeVisible()
    await page.goto('/')
    await expect(page.getByRole('link', { name: '이어서 하기' })).toHaveAttribute('href', `/play/${player.id}`)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
    await page.screenshot({ path: `/tmp/daesogo-editions-${width}.png`, fullPage: true })
  })
}

test('순한맛 자퇴 요청을 거절하고 두 버전 모두 3학년 캠프를 제공하지 않는다', () => {
  const mild = createGame({ name: '순한맛', edition: 'MILD' }, true)
  Object.assign(mild.view, { phase: 'Y1', year: 1, turn: 4, semesterFocus: { code: 'FOCUS_BALANCE', label: '균형', semester: 1 } })
  mild.view.stats.S = 95
  const support = events(mild)[0]
  expect(support.code).toBe('STRESS_SUPPORT')
  expect(support.choices.every(c => !c.code.includes('WITHDRAWAL'))).toBeTruthy()
  expect(() => execute(mild, '/events/SCHOOL_DOUBTS/choices', { choiceCode: 'CONSIDER_WITHDRAWAL' })).toThrow()
  for (const edition of ['MILD', 'SPICY']) for (const season of ['SUMMER', 'WINTER'] as const) {
    const game = createGame({ name: '방학', edition }, true)
    Object.assign(game.view, { phase: 'Y3', year: 3, major: 'SW', track: 'FE', turn: season === 'SUMMER' ? 12 : 24, vacation: { season, campTurn: 0 } })
    expect(events(game)[0].choices.map(c => c.code)).not.toContain('MAJOR_CAMP')
    expect(() => execute(game, '/events/VACATION/choices', { choiceCode: 'MAJOR_CAMP' })).toThrow()
    const stories: StoryEntry[] = []
    execute(game, '/events/VACATION/choices', { choiceCode: 'VACATION_REST' })
    appendDemoStory(game, stories, '/events/VACATION/choices')
    expect(stories[0].text).not.toContain('캠프')
  }
})
