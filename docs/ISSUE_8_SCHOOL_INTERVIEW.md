# 이슈 #8 학교 면접 API·검증·연동

## 구현 상태와 의존성

이슈 댓글과 PR #11에는 학교 자유 답변 면접이 미구현으로 기록되어 있지만, 질문·제출의 기본 연결은 PR #16에 구현되어 있습니다. 이 작업은 PR #16의 공통 InterviewService를 재사용합니다. 브랜치는 `feature/school-interview-completion`, 기반은 PR #16의 `66cabd7`입니다. 작업 시작 당시 OPEN이던 PR #16은 최종 확인 시 MERGED 상태입니다. 기존 main의 초기 로컬 커밋과 다른 작업 폴더를 변경하지 않았습니다.

## 요청과 응답

모든 경로는 `/api/v1/players/{playerId}` 기준입니다. 변경 요청에는 UUID인 `Idempotency-Key`와 최신 `X-Expected-Version`을 사용합니다. 질문·결과 GET은 버전을 바꾸지 않습니다. 공통 응답은 `{ "success": true, "data": ..., "error": null }` 형태입니다.

1. `POST /api/v1/players`에 이름을 보내 회차를 생성합니다(201).
2. `POST /events/AD01/choices`에 `{"choiceCode":"GENERAL"}`을 보냅니다.
3. `POST /events/AD02/choices`에 서류와 선택적 지원 동기를 보냅니다.

```json
{
  "choiceCode": "BALANCED",
  "motivation": "가족 카페의 재고 관리 불편을 줄이는 백엔드를 배우고 싶어 지원했습니다."
}
```

`motivation`은 AD02에서만 허용하며 보내는 경우 공백 아닌1~1200자입니다. 기존 요청에는 필수가 아닙니다. 저장된 지원 동기가 없는 구 회차에 개인 사유를 지어내지 않습니다.

4. AD03·AD04에 각각 `{"choiceCode":"PREPARED"}`를 제출합니다. AD04 응답의 `details.interviewQuestion`에도 준비된 질문이 들어 있습니다.
5. `GET /events/AD05/interview`를 조회합니다.

```json
{
  "questionId": "00000000-0000-4000-8000-000000000001",
  "question": "지원 동기와 활동 경험, 문제 해결 과정, 입학 후 계획을 설명해 주세요. 제출한 지원 동기: ... 최근 활동: AD03: ...",
  "source": "MOCK",
  "kind": "SCHOOL"
}
```

위 ID·질문은 형식 예시입니다. 제출 시에는 실제 조회 응답의 UUID를 사용합니다. 회차·학교/회사·회사 지원 ID별 질문을 저장하고 반복 조회에 그대로 반환합니다. 학교 질문에는 저장된 지원 동기와 앞선 활동 장면을 반영합니다. AI 키 없이 MOCK으로 동작하고 생성 실패·시간 초과의 FALLBACK에서도 같은 맥락을 사용합니다. 이미 발급된 구 질문을 새로 바꾸지 않습니다.

6. `POST /events/AD05/choices`로 자유 답변을 제출합니다.

```json
{
  "choiceCode": "ANSWER",
  "questionId": "00000000-0000-4000-8000-000000000001",
  "answer": "동아리 프로젝트에서 제가 서버 개발을 맡아 재고 조회 API를 작성했습니다. 중복 요청 오류를 로그로 재현하고 원인을 분석해 멱등키 처리를 구현했습니다. 같은 요청을 반복하는 회귀 테스트로 수정 결과를 확인했습니다. 입학 후 매주 Java와 자료구조를 학습하고 팀 프로젝트를 배포할 계획입니다."
}
```

이 답변은 형식 예시이며 만점을 보장하는 정답이 아닙니다. 실제 HTTP 테스트의 전체 요청·응답은 `build/reports/issue8/successful-flow.json`에 저장됩니다.

7. 제출 응답의 `data.details.admissionResult`와 `GET /events/AD05/result`에서 같은 판정 스냅샷을 확인합니다. 1차 결과는 `/events/AD02/result`입니다.

```json
{
  "eventCode": "AD05",
  "resultCode": "PASSED",
  "admissionTrack": "GENERAL",
  "appliedTrack": "GENERAL",
  "decision": {
    "passed": true,
    "shieldActivated": false,
    "rank": 31,
    "capacity": 72,
    "score": 81.65,
    "generalScore": 81.65,
    "generalRank": 31
  },
  "componentScores": { "scratch": 95.0, "python": 95.0, "basic": 22.0, "interview": 100.0 },
  "interview": {
    "score": 100,
    "components": { "motivation": 25, "experience": 25, "problemSolving": 25, "schoolPlan": 25 },
    "source": "RULE_BASED",
    "rubricVersion": "school-evidence-v1",
    "criteria": {
      "problemSolving": {
        "score": 25,
        "maxScore": 25,
        "evidence": ["문제 원인이 중복 요청임을 확인해서 멱등키 처리로 수정했고 ... 검증했습니다"],
        "reasons": ["문제 상황의 서술 확인: 5점", "원인 분석·재현 근거 확인: 5점", "원인에 따른 수정 행동 확인: 5점", "수정 후 검증 과정 확인: 5점", "해결 결과 확인: 5점"]
      }
    }
  },
  "reasonCodes": ["TRACK_RANK_WITHIN_CAPACITY", "INTERVIEW_WEIGHT_25_PERCENT"],
  "nextPhase": "BEFORE_SCHOOL",
  "beforeSchoolDay": 1
}
```

응답은 구조를 설명하기 위한 발췌 예시입니다. 실제 `criteria`에는 네 항목이 모두 들어가며, 순위는 회차에 고정된 경쟁자 점수에 따라 달라집니다. 81.65는 시험95/95·기초22·면접100의 25/30/20/25 가중합입니다. 이번 실행 예시의 순위31은 보장값이 아닙니다.

## 평가와 보호막

- 항목별0~25, 총0~100입니다. 한국어 완결 문장에서 확인한 근거를 각5점으로 평가합니다. 단순 단어 반복이나 답변 길이는 가산하지 않습니다.
- 지원 동기: 지원 이유, 개인 계기, 구체적 대상, 경험·목표 연결, 학교 학습과의 연결.
- 활동 경험: 경험 서술, 본인 역할·행동, 작업 대상, 협업·과정, 결과 확인.
- 문제 해결: 문제 상황, 분석·재현, 원인에 따른 수정, 검증 과정, 해결 결과.
- 입학 후 계획: 계획 서술, 시기·주기, 학습 대상, 수행 행동, 산출물·목표.
- `criteria`에 답변 인용과 충족/미충족 설명을 남깁니다. 기본 게임용 루브릭이며 AI의 의미 이해나 경험의 진위를 검증하는 기능이 아닙니다. 사실 확인·모든 동의어·영어 답변의 동등한 이해는 보장하지 않습니다.
- 1차144/최종72와 동점 경쟁자 우선 규칙을 유지합니다. 보호막은 한 번 생성한 같은 경쟁자 배열과 이미 제출한 점수를 일반 기준으로 비교하며 답변 평가나 추첨을 반복하지 않습니다.
- 최종 가중치는 일반·특별전형 공통이어서 최종 보호막의 일반 점수/순위도 같습니다. 보호막 통과를 만들기 위해 다른 최종 가중치를 지어내지 않았습니다. 서류의 전형별 가중치가 달라 1차 보호막 통과는 가능합니다.
- 보호막 응답에는 `GENERAL_SHIELD_APPLIED`, `GENERAL_SHIELD_PASSED/FAILED` 사유를 저장합니다. 원 지원 전형은 유지하고 실제 비교한 기준은 `appliedTrack`에 표시합니다.
- 현재 입학 규칙상 면접 제출의 AP·능력치 직접 보상은 없습니다. 클라이언트가 점수·합격·능력치 필드를 보내도 서버 계산을 대체하지 않습니다.

## 오류·저장 계약

- 400 `VALIDATION_ERROR`: 다른/잘못된 질문 UUID, 다른 회차 질문, 빈/공백/1200자 초과 답변, 형식 오류.
- 409 `WRONG_STEP/WRONG_PHASE`: 잘못된 진행 단계, 이미 완료한 면접의 새 제출, 아직 없는 결과 조회.
- 409 `VERSION_CONFLICT`: 오래된 버전으로 새 명령 제출.
- 409 `IDEMPOTENCY_CONFLICT`: 같은 키로 다른 본문 제출.
- 1자·1200자 답변은 형식상 허용합니다. 충분한 경험 근거가 없으면 채점은0점일 수 있습니다.
- 같은 키·같은 본문은 최초 제출 때의 버전 헤더로 재시도해도 저장 응답을 반환합니다. 버전·현재 단계·질문 완료 검사보다 재시도 확인을 먼저 수행합니다.
- 질문 완료 표시, 평가, 단계 전이, 활동/장면/명령 결과는 같은 트랜잭션에서 저장됩니다. 오류 시 일부만 남지 않습니다.
- 합격/불합격 모두 당시 점수·순위·사유·근거를 ActivityRecord의 `admissionResult`에 저장합니다. 결과 GET은 현재 상태로 재평가하지 않습니다.
- 과거 결과에 새 루브릭 근거가 없으면 원래 점수만 반환하고 `LEGACY_RESULT_WITHOUT_RUBRIC_EVIDENCE`로 표시합니다. 과거 결과를 새 기준으로 덮어쓰지 않습니다.

## 연동 및 검증 범위

- 프론트: AD02에서 선택적 motivation 전달, AD05에서 조회한 questionId와 answer 제출, 결과는 `details.admissionResult` 또는 결과 GET 사용.
- 공통 면접/회사: PR #16의 InterviewService와 InterviewValidationResult 계약을 유지합니다. 회사 최종 점수·채용 상태 계산은 변경하지 않습니다.
- 로컬 실제 임의 포트 HTTP 서버 + JDK HttpClient + H2로 전체 요청 경로를 검증합니다. MockMvc/Service 테스트도 함께 실행합니다. 실제 외부 AI 키/네트워크 연결이나 모바일 화면 연동까지 검증한 것은 아닙니다.


## 변경 파일

- 입력·질문: `ChooseEventRequest.java`, `InterviewServiceImpl.java`, `InterviewQuestionGenerator.java`, `InterviewConfig.java`, `EventFlowServiceImpl.java`.
- 평가·판정: `SchoolInterviewEvaluator.java`, `SchoolInterviewEvaluationView.java`, `AdmissionPolicy.java`, `Player.java`.
- 결과 API: `EventController.java`, `AdmissionResultView.java`, `AdmissionResultService.java`, `AdmissionResultServiceImpl.java`.
- 새 검증: `SchoolMotivationContextTest.java`, `SchoolInterviewEvaluatorTest.java`, `SchoolInterviewResultTest.java`, `SchoolAdmissionHttpTest.java`, `support/AdmissionAnswers.java`.
- 기존 검증 보완: `InterviewFallbackServiceTest.java`, `InterviewServiceTest.java`, `AdmissionEventServiceTest.java`, `AdmissionPolicyTest.java`, `SchoolInterviewServiceTest.java`.

## 실행 결과

- 질문 맥락 기능: `./gradlew build` 성공, 전체106건 통과.
- 평가·결과 저장 기능: `./gradlew build` 성공, 전체117건 통과.
- 실제 포트 HTTP 전체 입학·예외·동시 제출: SchoolAdmissionHttpTest 5건 통과.
- 최종 `./gradlew build`: 전체122건 통과, 실패/오류0. 기존102건을 포함합니다.
- 실제 HTTP 예시 파일: `build/reports/issue8/successful-flow.json`(테스트 재실행 시 해당 회차의 질문 ID·순위로 갱신).
