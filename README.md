# silvally

## 인생 기록 AI 서사 설정

서사 생성은 서버 환경변수로만 설정합니다. API 키가 없으면 고정 문장 템플릿(mock) 모드로 실행됩니다.

| 환경변수 | 설명 | 기본값 |
| --- | --- | --- |
| `NARRATIVE_LLM_API_KEY` | OpenAI 호환 Chat Completions API 키. 비어 있으면 템플릿 모드 | (없음) |
| `NARRATIVE_LLM_BASE_URL` | OpenAI 호환 엔드포인트 기본 URL (예: `https://.../v1`) | OpenAI 기본 URL |
| `NARRATIVE_LLM_MODEL` | 모델 식별자 | `k-exaone_v2.5` |
| `NARRATIVE_LLM_TIMEOUT` | 요청 제한 시간 | `20s` |

키는 소스·프론트·로그에 남기지 않습니다. 서사의 능력치·합격·채용 등 게임 결과는 서버 판정만을 근거로 하며, AI는 장면 묘사와 NPC 대사만 생성합니다.
