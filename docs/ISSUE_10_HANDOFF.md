# 김일강 이슈 #10 연동 안내

## 학교 사건·시간 진행

- `EventService.list/detail/choose`: 현재 학년·턴·진행 상태에 맞는 사건과 선택을 기존 `/events` API에 연결합니다. `EventFlowService`는 입학/비포스쿨/재학 사건을 분기합니다.
- `EventService.requireNoPendingEvent(Player)`: 김우주 담당 턴 진행기가 필수 사건을 먼저 확인합니다. 오류 상세의 `events`에 대기 중인 사건 코드가 들어갑니다.
- `SchoolRules.TURNS_PER_YEAR=24`. 1~12턴/13~24턴은 각 학기입니다. `FOCUS_Y{year}_S{semester}` 선택은 AP0/즉시 성장0, 선택한 P/A/C +1을 정규 턴에 반영합니다.
- `VACATION_Y{year}_SUMMER/WINTER`: 12/24턴 성장 후 같은 정규 턴에서 선택 대기. REST는 다음 학기/학년으로 전이, CAMP는 별도 실습 2턴을 `/turns/advance`로 수행한 뒤 전이합니다. 실습은 P+2/C+1의 가상 게임 보상이며 정규 턴 성장과 집중 목표를 중복 적용하지 않습니다.
- 나르샤: `NARSHA_START`(1-8), `NARSHA_WRAP`(1-23), `NARSHA_RESUME`(2-2), `NARSHA_RANK`(2-22). 참가 이력이 있는 다음 단계만 열리며 순위는 한 번 저장합니다. 2-23의 `SOFTWAREWAVE`는 저장된 1~3위만 선택 가능합니다.
- 순위는 실제 학교의 선발 기준이 아닌 가상 게임 규칙입니다. 팀 역량(P/B/C 평균)에서 산출하며 `rankSource=GAME_RULE`로 표시합니다. 이전 코드에는 원본 팀 순위 데이터가 없습니다.
- 선택 결과의 `MutationResult.details`와 `ActivityRecord.evidenceJson`에 `scene`, `sceneSource=MOCK`, `sceneStatus=READY`, `tags`, 실제 `statDelta`를 전달합니다. 응답/명령 로그의 `sceneId`는 활동 이력 ID입니다. 공통 명령 로그에 선택/장면을 함께 붙이므로 일반 학교 사건은 이중 저장하지 않습니다.

## 공통 면접

- 학교 질문: `GET /api/v1/players/{id}/events/AD05/interview`. AD04를 완료하면 질문도 준비합니다.
- 회사 질문: `GET /api/v1/players/{id}/applications/{applicationId}/interview`. 현재 코드의 회사 단계 계약(CODE_PASSED, 3학년 5턴)을 검사하며 회사 전형 점수/상태를 변경하지 않습니다.
- 질문은 회차·종류·학교/회사 지원 ID별로 저장하고 최근 활동 장면을 참고합니다. 기본 `InterviewQuestionGenerator`는 키가 필요 없는 MOCK입니다. AI 어댑터는 같은 인터페이스를 제공하면 됩니다. 생성 실패/잘못된 출력/최대7초 초과는 FALLBACK 질문을 한 번 저장합니다.
- `InterviewService.validateAnswer(...)`는 비소모 검증용입니다. 실제 제출은 호출 서비스의 `CommandExecutor` 트랜잭션 안에서 `submitAnswer(Player, kind, subjectCode, questionId, answer)`를 호출합니다. 회차 잠금·질문 완료 표시를 수행하고 `InterviewValidationResult`에 질문/정규화 답변/출처를 반환합니다.
- 조성혁 담당 채용 서비스는 해당 DTO를 받아 회사 채점과 전형 상태 변경을 같은 트랜잭션에서 처리해야 합니다. 공통 서비스는 회사 점수를 계산하지 않습니다.
- AD05 본문은 `choiceCode=ANSWER`, `questionId`, `answer`입니다. 답변1~1200자, 다른 회차/질문/면접/지원 ID, 이미 완료된 면접을 거절합니다. 같은 멱등키 재시도는 완료 표시 검사 전에 저장 응답을 반환합니다.
- 학교 점수는 AI 평가가 아닌 공개된 기본 `RULE_BASED` 평가기입니다. 지원 동기·활동·문제 해결·입학 후 계획의 항목별0~25, 총0~100이며 기존 최종 전형의 면접25%에 반영합니다. 실제 자유 답변 채점 품질 고도화는 별도입니다.

## 검증 범위

Service 테스트는 H2 저장·재조회·명령 재시도·예외 후 상태 보존을 검증합니다. 학교 면접은 기존 이벤트 선택 HTTP 계약도 검증합니다. 외부 AI 서비스의 실제 연결이나 회사 채점/졸업 판정 서비스의 미구현 로직까지 완성했다고 간주하지 않습니다.

## 국내·실리콘밸리 현장학습

- 기존 서버 기획서의 가상 게임 규칙을 적용했습니다. 3학년 3턴 `J01`에서 DOMESTIC 또는 SILICON_VALLEY 중 하나를 한 번 신청합니다. 국내는 신청 시 선발, 해외는 신청 당시 EN50 이상이면 선발입니다. 별도 합격률/추첨이나 실제 학교의 공식 선발 기준은 추가하지 않았습니다.
- `Stats.EN`은 초기22, 0~100이며 기존 정규 턴 성장에 일반+2/2학년+3을 추가합니다. 이전 능력치와 생성자 계약을 유지합니다.
- `FieldLearningView`는 신청 시기/경로/영어 스냅샷, 선발 여부, 미선발 사유, 준비 여부·턴, 완료 여부·턴을 구분합니다. 경로는 신청 후 변경할 수 없습니다. 미선발도 같은 선택을 소비한 확정 결과이며, 이후 영어가 올라가도 다시 선발하지 않습니다.
- `EventService.choose(...,"J01",...)`가 기존 이벤트 선택 API와 공통 CommandExecutor에서 신청을 처리합니다. 같은 멱등키는 최초 응답을 반환하며 다른 본문이나 새 키로 경로를 바꾸면 거절합니다.
- `FieldLearningService.onTurnEnd(Player)`는 김우주 담당 SchoolTurnService의 정규 턴 종료에서 호출합니다. 3턴 종료 SELECTED→PREPARING, 4턴 종료 PREPARING→COMPLETED입니다. NOT_SELECTED에는 준비/완료/참가 보상을 주지 않습니다.
- 완료 보상은 국내 P+2/C+3, 해외 EN+4/C+3입니다. 완료 상태 전이와 실제 능력치 차이, `J01_PREPARE`/`J01_COMPLETE` 활동·인생 기록을 같은 명령 트랜잭션에 저장하고 재시도에서 중복 적용하지 않습니다.
- `GET /api/v1/players/{id}/field-learning`은 프로그램 상태를 반환합니다. `GET .../field-learning/summary`와 `FieldLearningService.summary(UUID)`는 `FieldLearningSummaryView(program, milestones)`를 반환합니다. 최신 main의 GraduationSummaryBuilder도 기존 FIELD_ 이력과 함께 J01/J01_ 이력 및 증거 데이터를 fieldTrips 졸업 스냅샷에 포함합니다. 공통 HistoryService도 같은 milestone의 결과·장면·실제 보상 데이터를 재사용할 수 있습니다.
- 회차 삭제 시 현장학습 엔티티·활동·명령 기록도 함께 정리합니다.

## 최종 확인 및 담당 경계

- 학교 사건·공통 면접 커밋: `./gradlew build`, 전체69건 통과.
- 현장학습 추가 후: `./gradlew build`, 전체77건 통과(실패/오류0). FieldLearningServiceTest 8건에는 해외 미선발 후 영어가50을 넘어도 완료·보상 없음, 준비→완료, 재시도, 경로 변경 거절, 요약 HTTP/회차 삭제를 포함합니다.
- 회사 채점·전체 인생 기록 API의 기존 담당 TODO는 유지합니다. 최신 main의 실제 졸업 판정·요약 생성은 그대로 재사용하고 현장학습 기록만 연결했습니다. 이번 작업은 해당 서비스가 호출할 수 있는 검증·현장학습 요약 DTO와 영속 이력을 제공합니다. 외부 AI 연결은 실제 키/네트워크로 검증하지 않았고 mock 및 생성 실패·시간 초과 경계만 검증했습니다.

- PR 준비 중 최신 main의 졸업 기능을 반영했습니다. 완료/미선발 현장학습이 실제 졸업 스냅샷과 재조회에도 보존되는 통합 테스트를 추가했습니다.
- 최신 main 통합 후 최종 `./gradlew build`: 전체93건 통과(실패/오류0).
