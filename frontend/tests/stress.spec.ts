import { expect, test } from '@playwright/test'
import { createGame, events, execute } from '../demo/game'
import { appendDemoStory } from '../demo/story'
import { calendar } from '../demo/data'
import type { StoryEntry } from '../src/api/contracts'

for (const width of [360, 1280]) {
  test(`${width}px에서 스트레스 선택과 자퇴 재고·확정 화면을 진행한다`, async ({ page }) => {
    // The service tests exercise persistence and thresholds against Spring.
    // This fixture supplies the same state machine to verify both viewport layouts.
    const game = createGame({ demoScenario: 'COUNSELING' }, true)
    delete game.stressHandled
    game.view.stats.S = 60
    const base = `/api/v1/players/${game.view.id}`
    const stories: StoryEntry[] = [{ id: 'rested', year: 3, turn: 11, title: '일과를 마친 뒤의 휴식',
      text: '수업을 마치고 쉬며 부담을 조금 덜었다.', dialogue: [], source: 'MOCK', status: 'READY',
      stressLoad: { stressBefore: 20, stressAfter: 18, total: -2,
        sources: [{ code: 'DAILY_RECOVERY', reason: '일과를 마치고 쉬었다.', delta: -2 }] } }]
    await page.route('**/api/v1/**', async (route) => {
      const request = route.request()
      const url = new URL(request.url())
      const suffix = url.pathname.slice(base.length)
      let data: unknown
      if (request.method() === 'POST' && url.pathname.startsWith(base + '/events/')) {
        const expected = Number(request.headers()['x-expected-version'])
        expect(expected).toBe(game.view.version)
        game.view.version++
        data = execute(game, suffix, request.postDataJSON())
        appendDemoStory(game, stories, suffix)
      } else if (url.pathname === base) data = game.view
      else if (suffix === '/events') data = events(game)
      else if (suffix === '/story') data = { entries: stories }
      else if (suffix === '/ending') data = game.ending
      else if (suffix === '/applications') data = []
      else if (suffix === '/progress') data = {
        player: game.view, projects: [], qualifications: [], enrollments: [], snapshots: [], yearSummaries: [],
      }
      else if (url.pathname === '/api/v1/catalog/calendar') data = calendar
      else if (url.pathname.startsWith('/api/v1/catalog/')) data = []
      else throw new Error(`Unhandled fixture request ${request.method()} ${url.pathname}`)
      await route.fulfill({ json: { success: true, data, error: null } })
    })
    await page.setViewportSize({ width, height: 850 })
    await page.goto(`/play/${game.view.id}`)
    const dialog = page.getByRole('region', { name: '중대 결정' })
    await expect(dialog.getByRole('heading', { name: '잠시 숨을 고를 시간' })).toBeVisible()
    await expect(page.locator('.clock-label')).toContainText('일시 정지 상태')
    await expect(page.getByRole('dialog', { name: '중대 결정' })).toHaveCount(0)
    const recent = page.locator('#current-story')
    const choices = page.getByRole('region', { name: '선택지', exact: true })
    const past = page.getByRole('region', { name: '여태껏 했던 로그', exact: true })
    const [recentBox, choicesBox, pastBox] = await Promise.all([recent.boundingBox(), choices.boundingBox(), past.boundingBox()])
    expect(recentBox && choicesBox && recentBox.y + recentBox.height <= choicesBox.y).toBeTruthy()
    expect(choicesBox && pastBox && choicesBox.y + choicesBox.height <= pastBox.y).toBeTruthy()
    await expect(past).toContainText('아직 이전 로그가 없습니다.')
    const beforeReload = events(game)[0].choices.map((choice) => choice.label)
    await page.reload()
    expect(events(game)[0].choices.map((choice) => choice.label)).toEqual(beforeReload)
    const recovery = events(game)[0].choices[0]
    await dialog.getByRole('button', { name: recovery.label, exact: true }).click()
    await expect(page.getByRole('meter', { name: '스트레스', exact: true })).toHaveAttribute('value', String(game.view.stats.S))
    await expect(recent.getByRole('heading', { name: '학교생활의 부담을 나누다', exact: true })).toBeVisible()
    await expect(past.getByRole('heading', { name: '일과를 마친 뒤의 휴식', exact: true })).toBeVisible()
    await expect(past.getByRole('heading', { name: '학교생활의 부담을 나누다', exact: true })).toHaveCount(0)
    await expect(page.getByRole('list', { name: '스트레스 변화 이유' }).first()).toContainText('덜어 낸 부담 · 스트레스 -2')
    await expect(page.getByText('스트레스 +-2', { exact: false })).toHaveCount(0)
    await page.screenshot({ path: `/tmp/daesogo-story-order-${width}.png`, fullPage: true })

    game.view.stats.S = 90
    await page.reload()
    await expect(dialog.getByRole('heading', { name: '학교를 계속 다닐 수 있을까' })).toBeVisible()
    await dialog.getByRole('button', { name: '자퇴를 고민해 보기', exact: true }).click()
    await expect(dialog.getByRole('heading', { name: '자퇴를 결정하기 전에' })).toBeVisible()
    await page.reload()
    await expect(dialog.getByRole('heading', { name: '자퇴를 결정하기 전에' })).toBeVisible()
    await dialog.getByRole('button', { name: '도움을 받으며 학교생활 이어가기', exact: true }).click()
    await expect(page.getByRole('meter', { name: '스트레스', exact: true })).toHaveAttribute('value', '65')
    expect(game.view.phase).toBe('Y3')

    game.view.turn++
    game.view.stats.S = 90
    await page.reload()
    await dialog.getByRole('button', { name: '자퇴를 고민해 보기', exact: true }).click()
    await dialog.getByRole('button', { name: '자퇴를 확정하고 다음 길 준비하기', exact: true }).click()
    await expect(page).toHaveURL(/\/ending\//)
    await expect(page.getByRole('heading', { name: '잠시 다른 길을 선택했습니다' })).toBeVisible()
    await expect(page.getByText('3학년 13턴에 자퇴를 선택했습니다.')).toBeVisible()
    await expect(page.getByText('졸업 완료', { exact: true })).toHaveCount(0)
    await page.reload()
    await expect(page.getByRole('heading', { name: '잠시 다른 길을 선택했습니다' })).toBeVisible()
  })
}
