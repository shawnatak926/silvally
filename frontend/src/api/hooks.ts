import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useGameUi, useSavedGame } from '../state'
import { ApiError, newCommandKey, request, resultPlayer, sendCommand, type PendingCommand } from './client'
import { playerPath, type PlayerView, type StoryView } from './contracts'

const queryKey = (id: string, suffix: string) => ['player', id, suffix] as const

export function usePlayerData<T>(id: string, suffix: string, enabled = true, path = playerPath(id) + suffix) {
  return useQuery({
    queryKey: queryKey(id, suffix),
    queryFn: ({ signal }) => request<T>(path, { signal }),
    enabled: Boolean(id) && enabled,
    retry: false,
  })
}

export function useStory(id: string) {
  return useQuery({
    queryKey: queryKey(id, '/story'),
    queryFn: ({ signal }) => request<StoryView>(`${playerPath(id)}/story`, { signal }),
    retry: false,
    refetchInterval: (query) => query.state.data?.entries.some((entry) => entry.status === 'PENDING') ? 1500 : false,
  })
}

export function useCommands() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const mutation = useMutation({ mutationFn: sendCommand, retry: false })

  function refreshPlayerData(id: string) {
    // Story generation and reads have their own lifecycle.
    void queryClient.invalidateQueries({ queryKey: queryKey(id, '/story') })
    return queryClient.invalidateQueries({
      queryKey: ['player', id],
      predicate: (query) => query.queryKey[2] !== '/story',
    })
  }

  async function execute(command: PendingCommand) {
    const ui = useGameUi.getState()
    if (ui.active || (ui.pending && ui.pending.key !== command.key)) return null
    ui.setActive(true)
    const targetId = command.path.match(/^\/players\/([^/]+)/)?.[1]
    try {
      ui.journal(command)
      const result = await mutation.mutateAsync(command)
      const player = resultPlayer(result)
      ui.journal(null)
      if (player.demoScenario !== 'COUNSELING') useSavedGame.getState().save(player.id)
      await queryClient.cancelQueries({ queryKey: ['player', player.id] })
      queryClient.setQueryData<PlayerView>(queryKey(player.id, ''), (current) =>
        current && current.version > player.version ? current : player,
      )
      await refreshPlayerData(player.id)
      if (command.path === '/players') navigate(`/play/${player.id}`)
      return result
    } catch (error) {
      if (error instanceof ApiError && error.status >= 400 && error.status < 500 && error.status !== 408) {
        ui.journal(null)
        if (targetId) await refreshPlayerData(decodeURIComponent(targetId))
      }
      throw error
    } finally {
      ui.setActive(false)
    }
  }

  return {
    run: (method: PendingCommand['method'], path: string, body?: PendingCommand['body'], version?: number) =>
      execute({ key: newCommandKey(), method, path, body, expectedVersion: version }),
    retry: () => {
      const command = useGameUi.getState().pending
      return command ? execute(command) : Promise.resolve(null)
    },
  }
}
