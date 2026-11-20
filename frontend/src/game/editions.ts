export type GameEdition = 'MILD' | 'SPICY'
export const editionName = (edition: GameEdition) => edition === 'MILD' ? '순한맛' : '매운맛'
export const lockedEdition = import.meta.env.VITE_GAME_EDITION as GameEdition | undefined
