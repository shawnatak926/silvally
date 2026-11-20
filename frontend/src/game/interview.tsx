import { useId, useState } from 'react'
import type { InterviewQuestion } from '../api/contracts'
import { usePlayerData } from '../api/hooks'
import { Loading, Problem } from './ui'

export function InterviewForm({ playerId, path, busy, submit }: {
  playerId: string
  path: string
  busy: boolean
  submit: (questionId: string, answer: string) => void
}) {
  const question = usePlayerData<InterviewQuestion>(playerId, path)
  const [answer, setAnswer] = useState('')
  const answerId = useId()
  if (question.error) return <Problem error={question.error} retry={() => { void question.refetch() }} />
  if (question.isPending) return <Loading text="면접 질문을 불러오는 중…" />
  const { id, question: prompt } = question.data
  const reply = answer.trim()
  return (
    <form className="interview-form" onSubmit={(event) => {
      event.preventDefault()
      if (reply && !busy) submit(id, reply)
    }}>
      <p className="interview-question">{prompt}</p>
      <label htmlFor={answerId}>면접 답변</label>
      <textarea id={answerId} rows={5} maxLength={1200} required value={answer}
        placeholder="지원 이유나 경험을 구체적으로 설명해 주세요." onChange={(event) => setAnswer(event.target.value)} />
      <small className="muted">{answer.length} / 1,200자</small>
      <button className="btn primary wide" type="submit" disabled={busy || !reply}>면접 답변 제출</button>
    </form>
  )
}
