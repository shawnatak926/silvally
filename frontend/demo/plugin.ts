import type { IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin } from 'vite'
import type { AdmissionTrack, CatalogEntry } from '../src/api/contracts.ts'
import { appendDemoStory } from './story.ts'
import { admissionTracks, calendar, companies, projects, roles } from './data.ts'
import { fieldLearningSummary } from './field-learning.ts'
import { schoolLifeCatalog } from './school-life.ts'
import { actions, admissionInterview, createGame, DemoError, employmentInterview, events, execute, fieldLearningRoute,
  friendRelationship, inventory, preferences, qualifications, trackEnabled, type DemoGame } from './game.ts'

const prefix = '/api/v1'
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function response(res: ServerResponse, status: number, payload: unknown) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' })
  res.end(JSON.stringify(payload))
}

async function bodyOf(req: IncomingMessage): Promise<Record<string, unknown>> {
  req.setEncoding('utf8')
  let json = ''
  for await (const chunk of req) {
    json += chunk.toString()
    if (json.length > 8_192) throw new DemoError(413, 'INVALID_BODY', '요청이 너무 큽니다.')
  }
  if (!json) return {}
  try {
    const parsed: unknown = JSON.parse(json)
    if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) return parsed as Record<string, unknown>
  } catch {
    // Invalid input is returned as a normal JSON error envelope.
  }
  throw new DemoError(400, 'VALIDATION_ERROR', '요청 형식이 올바르지 않습니다.')
}

function catalog(kind: string, game: DemoGame): CatalogEntry[] {
  switch (kind) {
    case 'admission-tracks': return Object.entries(admissionTracks).map(([code, track]) => ({
      code, title: track.title, details: { enabled: trackEnabled(game, code as AdmissionTrack), eligibilityChance: track.eligibilityChance },
    }))
    case 'career-roles':
      if (!game.view.initialCareerCode) throw new DemoError(409, 'CAREER_CATALOG_LOCKED', '희망 직무를 먼저 선택해 주세요.')
      return roles.map(([code, title]) => ({ code, title }))
    case 'courses': return [{ code: 'C_INFO', title: '정보처리기능사' }]
    case 'projects': return Object.entries(projects).map(([code, item]) => ({ code, title: item.title }))
    case 'companies': return Object.entries(companies).map(([code, item]) => ({
      code, title: item.title, details: { role: item.role }, requirements: { skill: item.cut[0], portfolio: item.cut[1] },
    }))
    case 'items': return [{ code: 'RECOVERY_KIT', title: '회복 키트' }]
    case 'school-life': return schoolLifeCatalog
    case 'actions': return [...new Set(actions(game).map((item) => item.code))].map((code) => ({ code, title: code }))
    default: throw new DemoError(400, 'INVALID_KIND', '카탈로그 종류를 확인해 주세요.')
  }
}

function read(game: DemoGame, suffix: string, params: URLSearchParams) {
  const event = suffix.match(/^\/events\/([^/]+)$/)
  if (event) {
    const found = events(game, event[1] === 'TERM_FOCUS').find((item) => item.code === event[1])
    if (!found) throw new DemoError(404, 'NOT_FOUND', '현재 열려 있는 이벤트가 아닙니다.')
    return found
  }
  const interview = suffix.match(/^\/applications\/([^/]+)\/interview$/)
  if (interview) return employmentInterview(game, interview[1])
  switch (suffix) {
    case '': return game.view
    case '/events': return events(game)
    case '/events/AD05/interview': return admissionInterview(game)
    case '/actions': return actions(game)
    case '/inventory': return inventory(game)
    case '/career-preference': return preferences(game)
    case '/applications': return game.applications
    case '/progress': return { player: game.view, enrollments: game.enrollments, projects: game.projects,
      qualifications: qualifications(game), fieldLearningRoute: fieldLearningRoute(game),
      fieldLearning: fieldLearningSummary(game.fieldLearning, fieldLearningRoute(game)), friendRelationship: friendRelationship(game),
      narsha: game.narsha ?? null,
      narshaProjects: [...(game.narshaProjects ?? []), ...(game.narsha ? [game.narsha] : [])],
      yearSummaries: [1, 2, 3].flatMap((year) => game.snapshots.filter((entry) => entry.year === year).slice(-1)), snapshots: game.snapshots }
    case '/history': {
      const after = Number(params.get('afterId') ?? 0)
      const limit = Number(params.get('limit') ?? 20)
      if (!Number.isInteger(after) || after < 0 || !Number.isInteger(limit) || limit < 1 || limit > 50) {
        throw new DemoError(400, 'VALIDATION_ERROR', '기록 조회 범위를 확인해 주세요.')
      }
      const list = game.history.filter((item) => item.logId > after)
      const items = list.slice(0, limit)
      return { items, nextAfterId: list.length > limit ? items.at(-1)?.logId : null }
    }
    case '/ending':
      if (!game.ending) throw new DemoError(409, 'ENDING_NOT_READY', '아직 엔딩이 확정되지 않았습니다.')
      return game.ending
    default: throw new DemoError(404, 'NOT_FOUND', '요청을 찾을 수 없습니다.')
  }
}

export function demoApi(fixedStats: boolean): Plugin {
  const games = new Map<string, DemoGame>()
  const story = new Map<string, Parameters<typeof appendDemoStory>[1]>()
  const saved = new Map<string, { fingerprint: string; status: number; data: unknown; playerId: string }>()
  const findGame = (id: string) => {
    const game = games.get(id)
    if (!game) throw new DemoError(404, 'NOT_FOUND', '이어 할 회차가 없습니다. 새 지원을 시작해 주세요.')
    return game
  }

  async function handle(req: IncomingMessage, res: ServerResponse) {
    const url = new URL(req.url ?? '/', 'http://localhost')
    const path = url.pathname.slice(prefix.length)
    const player = path.match(/^\/players\/([^/]+)(.*)$/)
    const kind = path.match(/^\/catalog\/([^/]+)$/)?.[1]
    const method = req.method
    let status = 200
    let data: unknown

    if (method === 'GET') {
      if (kind) {
        const game = findGame(url.searchParams.get('playerId') ?? '')
        data = kind === 'calendar' ? calendar : catalog(kind, game)
      }
      else if (player) {
        const game = findGame(player[1])
        data = player[2] === '/story' ? { entries: story.get(game.view.id) ?? [] } : read(game, player[2], url.searchParams)
      }
      else throw new DemoError(404, 'NOT_FOUND', '요청을 찾을 수 없습니다.')
    } else {
      const isCreate = method === 'POST' && path === '/players'
      const isCommand = player && (player[2] === '/career-preference' ? method === 'PUT' : method === 'POST')
      if (!isCreate && !isCommand) throw new DemoError(404, 'NOT_FOUND', '요청을 찾을 수 없습니다.')
      const key = req.headers['idempotency-key']
      if (typeof key !== 'string' || !uuid.test(key)) throw new DemoError(400, 'VALIDATION_ERROR', '명령 키가 필요합니다.')
      const body = await bodyOf(req)
      const fingerprint = JSON.stringify([method, path, Object.entries(body).sort(([a], [b]) => a.localeCompare(b))])
      const storedKey = `${player?.[1] ?? 'create'}:${key}`
      const existing = saved.get(storedKey)
      if (existing) {
        if (existing.fingerprint !== fingerprint) throw new DemoError(409, 'IDEMPOTENCY_CONFLICT', '이미 처리한 요청 키입니다.')
        if (isCreate) res.setHeader('Location', `${prefix}/players/${existing.playerId}`)
        return response(res, existing.status, { success: true, data: existing.data, error: null })
      }

      let game: DemoGame
      if (isCreate) {
        game = createGame(body, fixedStats)
        data = game.view
        status = 201
        res.setHeader('Location', `${prefix}/players/${game.view.id}`)
      } else {
        game = structuredClone(findGame(player![1]))
        const version = req.headers['x-expected-version']
        if (typeof version !== 'string' || !/^\d+$/.test(version)) throw new DemoError(400, 'VALIDATION_ERROR', '현재 회차 버전이 필요합니다.')
        if (Number(version) !== game.view.version) throw new DemoError(409, 'VERSION_CONFLICT', '다른 화면에서 진행되었습니다. 현재 상태를 새로 읽었습니다.')
        game.view.version++
        data = execute(game, player![2], body)
        if (player![2] === '/applications') status = 201
      }
      events(game)
      const entries = story.get(game.view.id) ?? []
      appendDemoStory(game, entries, isCreate ? '' : player![2])
      story.set(game.view.id, entries)
      games.set(game.view.id, game)
      saved.set(storedKey, { fingerprint, status, data: structuredClone(data), playerId: game.view.id })
    }
    response(res, status, { success: true, data, error: null })
  }

  return {
    name: 'daesogo-local-demo-api',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (!req.url?.startsWith(prefix + '/')) return next()
        void handle(req, res).catch((error: unknown) => {
          const issue = error instanceof DemoError ? error : new DemoError(500, 'INTERNAL_ERROR', '시연 서버에서 오류가 발생했습니다.')
          response(res, issue.status, { success: false, data: null, error: { code: issue.code, message: issue.message } })
        })
      })
    },
  }
}
