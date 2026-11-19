export function eventRandom(id: string, key: string) {
  let state = 0
  for (const char of (id + '/' + key).split('')) state = Math.imul(state, 31) + char.charCodeAt(0) | 0
  const nextInt = (bound: number) => {
    state = Math.imul(state, 1664525) + 1013904223 | 0
    return (state >>> 0) % bound
  }
  return {
    nextInt,
    shuffled<T>(input: readonly T[]) {
      const result = [...input]
      for (let index = result.length - 1; index > 0; index--) {
        const other = nextInt(index + 1)
        const value = result[index]
        result[index] = result[other]
        result[other] = value
      }
      return result
    },
  }
}
