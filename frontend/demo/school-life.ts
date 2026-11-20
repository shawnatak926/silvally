import type { SchoolLifeCatalogEntry, SchoolLifeSource, Stats } from '../src/api/contracts.ts'
import content from './school-content.json' with { type: 'json' }

export const schoolSources = content.schoolSources satisfies Record<string, SchoolLifeSource>
export const schoolLifeCatalog: SchoolLifeCatalogEntry[] = Object.entries(schoolSources)
  .map(([code, source]) => ({ code, ...source }))
export const schoolScenes = content.schoolScenes

interface SchoolActivity {
  years: number[]
  minTurn: number
  maxTurn: number
  event: { code: string; title: string; situation: string; required: boolean; source: string;
    choices: { code: string; label: string; delta: Partial<Stats>; tag: string | null; cost: number; description: string }[] }
}

export function schoolActivityEvents(year: number, turn: number) {
  return (content.schoolActivities as SchoolActivity[])
    .filter((item) => item.years.includes(year) && turn >= item.minTurn && turn <= item.maxTurn)
    .map(({ event }) => ({ ...event, source: schoolSources[event.source as keyof typeof schoolSources],
      choices: event.choices.map((choice) => ({ ...choice, tag: choice.tag ?? undefined })) }))
}
