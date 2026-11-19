import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { PendingCommand } from './api/client'

const journalKey = 'daesogo.pending-command.v1'

function initialCommand(): PendingCommand | null {
  try {
    const command: PendingCommand | null = JSON.parse(sessionStorage.getItem(journalKey) ?? 'null')
    if (command && command.key && command.path.startsWith('/players') && ['POST', 'PUT'].includes(command.method)) return command
  } catch {
    // A fresh tab can start without a journal.
  }
  return null
}

interface GameUi {
  dialog: 'notebook' | 'history' | 'turn' | 'photo' | null
  pending: PendingCommand | null
  active: boolean
  setDialog: (dialog: GameUi['dialog']) => void
  journal: (command: PendingCommand | null) => void
  setActive: (active: boolean) => void
}

export const useGameUi = create<GameUi>((set) => ({
  dialog: null,
  pending: initialCommand(),
  active: false,
  setDialog: (dialog) => set({ dialog }),
  journal: (pending) => {
    if (pending) sessionStorage.setItem(journalKey, JSON.stringify(pending))
    else sessionStorage.removeItem(journalKey)
    set({ pending })
  },
  setActive: (active) => set({ active }),
}))

export const useSavedGame = create<{ playerId: string | null; save: (id: string | null) => void }>()(
  persist((set) => ({ playerId: null, save: (playerId) => set({ playerId }) }), { name: 'daesogo.saved-player.v1' }),
)
