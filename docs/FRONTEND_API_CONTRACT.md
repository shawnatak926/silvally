# 프론트 연동 API 계약 (#26)

기존 대소고나 프론트가 별도 로컬 서버 대신 silvally Spring 서버에 연결될 때 지켜야 할 공통 계약입니다.
프론트 소스는 이 저장소의 `frontend/`에 있습니다. 아래 서버 계약과 프론트의 어댑터 연결·실제 전체 진행 검증은 구분하며, API 주소만 바꾸면 모든 기능이 호환된다고 가정하지 않습니다.

## 1. 실행 환경

| 구분 | 프론트 API 주소 | 서버 설정 |
| --- | --- | --- |
| 개발 | Vite 개발 서버의 `/api` 프록시 → `http://localhost:8080` | 설정 없음 (같은 출처라 CORS 불필요) |
| 운영·분리 도메인 | 빌드 시 지정한 API 주소 (예: `VITE_API_BASE_URL`) | `CORS_ALLOWED_ORIGINS=https://front.example` (쉼표로 여러 개) |

- 서버 실행: `./gradlew bootRun` (Java 21, H2 메모리 DB, 재시작 시 데이터 초기화)
- AI 서사 키는 서버 환경 변수 `NARRATIVE_LLM_API_KEY`(`NARRATIVE_LLM_BASE_URL`, `NARRATIVE_LLM_MODEL`)에만 둡니다. 키가 없으면 템플릿 서사로 동작하며, 이 결과를 실제 LLM 호출 검증으로 보고하지 않습니다.
- CORS 허용 시 요청 헤더 `Content-Type`, `Idempotency-Key`, `X-Expected-Version`, 노출 헤더 `Location`만 엽니다.

## 2. 공통 규칙

### 응답 형식

```json
{ "success": true, "data": { }, "error": null }
{ "success": false, "data": null, "error": { "code": "VERSION_CONFLICT", "message": "...", "details": { } } }
```

### 변경 요청 헤더

| 헤더 | 규칙 |
| --- | --- |
| `Idempotency-Key` | 모든 생성·변경 요청에 필수. 사용자 동작 1회마다 새 UUID. 네트워크 재시도는 같은 키를 다시 보내며, 서버는 저장된 같은 응답을 돌려줍니다. |
| `X-Expected-Version` | 회차 생성 외 모든 변경 요청에 필수. 마지막으로 받은 `player.version`. |

- 성공한 변경 응답의 `player`가 최신 상태입니다. 프론트는 이 값으로 상태를 교체하고 능력치·시험·합격·채용 결과를 직접 계산하지 않습니다.
- 늦게 도착한 응답은 `player.version`이 현재 보유 값보다 작으면 버립니다. AI 서사를 기다리는 동안에도 같은 규칙으로 최신 상태를 덮어쓰지 않습니다.

### 오류 코드

| 코드 | HTTP | 프론트 처리 |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | `details.header`(헤더 누락), `details.parameter`(쿼리 누락·형식 오류), 필드명(본문 검증)을 표시 |
| `INVALID_TARGET` | 400 | 행동 대상 형식 오류 |
| `NOT_FOUND` | 404 | 회차·리소스 없음 → 회차 목록으로 |
| `VERSION_CONFLICT` | 409 | `details.expectedVersion`·`currentVersion` 확인 후 `GET /players/{id}` 재조회, 자동 진행 중단 |
| `IDEMPOTENCY_CONFLICT` | 409 | 같은 키로 다른 본문을 보냄 → 새 키로 다시 요청 |
| `CHOICE_REQUIRED` | 409 | 필수 선택 대기. `details.pendingChoice`·`options` 또는 `details.events` |
| `WRONG_PHASE` / `WRONG_STEP` | 409 | 단계 불일치 → 재조회 |
| `INSUFFICIENT_AP` / `LOCKED` | 409 | 선택 불가 사유 표시 |
| `FINAL_PATH_REQUIRED` / `ENDING_NOT_READY` | 409 | 졸업 진로 직접 선택 / 엔딩 대기 |
| `INTERNAL_ERROR` | 500 | 재시도 안내 |

## 3. 엔드포인트

기본 경로 `/api/v1`. ✎는 `Idempotency-Key`와 `X-Expected-Version`이 필요한 요청입니다 (회차 생성은 `Idempotency-Key`만, 회차 삭제는 `X-Expected-Version`만).

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| POST | `/players` | 회차 생성 (201, `Location`) |
| GET | `/players/{id}` | 회차 상태 (`PlayerView`) |
| PUT ✎ / DELETE | `/players/{id}` | 이름 변경 / 삭제 |
| GET | `/players/{id}/progress` | 수강·자격·프로젝트·학년별 시험 요약 |
| GET | `/players/{id}/history?afterId=&limit=` | 인생 기록 페이지 (limit 1~50) |
| POST | `/players/{id}/narratives/retry` | PENDING·FAILED 서사 재시도 |
| GET | `/players/{id}/events`, `/events/{code}` | 현재 가능한 사건 목록·상세 |
| POST ✎ | `/players/{id}/events/{code}/choices` | 입학(AD01~AD05)·비포스쿨(BS01~BS05)·재학 사건 선택 |
| GET | `/players/{id}/events/AD05/interview`, `/events/{code}/result` | 학교 면접 질문, 저장된 전형 결과 |
| GET / PUT ✎ | `/players/{id}/career-preference` | 전체 11개 직무 적합도순 조회 / 자유 확정·기존 조건에 따른 변경 |
| GET | `/players/{id}/turns/readiness` | 턴 진행 가능 여부와 차단 사유 |
| POST ✎ | `/players/{id}/turns/advance` | 턴 종료 |
| GET / POST ✎ | `/players/{id}/year-transition` | 학년 전환 필수 선택(MAJOR·TRACK) |
| GET / POST ✎ | `/players/{id}/actions` | 행동 목록 / 실행 |
| GET / POST ✎ | `/players/{id}/courses`, `/courses/{code}/enroll·attend·exam` | 방과후 과정·자격증 |
| GET / POST ✎ | `/players/{id}/projects`, `/projects/{code}/start·work·portfolio` | 프로젝트 |
| GET / POST ✎ | `/players/{id}/narsha`, `/narsha/actions` | 나르샤 주제 후보·학년별 기록 조회, 작업·멘토링(AP 1) |
| GET | `/players/{id}/field-learning`, `/field-learning/summary` | 현장실습·실리콘밸리 상태 |
| GET / POST ✎ | `/players/{id}/applications`, `/applications/{appId}/steps`, `/applications/{appId}/interview` | 채용 |
| GET / POST ✎ | `/players/{id}/inventory`, `/inventory/{item}/use` | 인벤토리 |
| POST ✎ / GET | `/players/{id}/graduation`, `/players/{id}/ending` | 졸업 확정 (`path`: AUTO·EMPLOYMENT·UNEMPLOYED·UNIVERSITY·INDIE_DEV) / 엔딩 |
| GET | `/catalog/{kind}?playerId=` | 카탈로그 (`playerId` 필수) |

## 4. 프론트 사용 필드와 서버 계약 차이

| 프론트가 쓰는 상태 | 서버 계약 | 구분 |
| --- | --- | --- |
| 지원 가능 전형 `admissionEligibility` | `PlayerView.admissionEligibility` (GENERAL·MEISTER·LOCAL·SOCIAL → boolean) | 서버 추가 |
| 학기 집중 `semesterFocus` | `PlayerView.school.semesterFocus` (현재 학기 선택 능력치 P·C·A, 미선택 시 null) | 서버 추가 |
| 방학·캠프 `vacation` | `school.vacationPending`, `school.practiceRemaining` (캠프 실습 잔여 턴) | 서버 추가 |
| 학년 선택 대기 | `school.pendingChoice` (MAJOR·TRACK) + `GET /year-transition` 선택지 | 서버 추가 |
| 나르샤 | `school.narshaStage`·`narshaRank`·`softwarewaveAttended`는 가장 최근 프로젝트의 요약 값입니다. 학년별 주제·역할·작업/멘토링·완성도·등위·전시 여부는 `GET /narsha`의 `projects[]`(`progress.narshaProjects`, 졸업 요약 `narshaProjects`도 같은 값) | 서버 추가 |
| 나르샤 시작 입력 | `NARSHA_START`(JOIN)·`NARSHA_RESUME`(RESUME) 선택 본문에 `narsha: {topicCode, customTopic?, role?}`가 필요합니다. `topicCode`는 `GET /narsha`의 `topics[].code` 또는 `CUSTOM`(직접 입력, 공백 정리 후 2~80자). 시작이 아닌 선택에 보내면 `VALIDATION_ERROR` | 서버 추가 |
| 시험 준비도 | `school.examPreparation` | 서버 추가 |
| 성적 G | `stats.G`, 시험 결과는 턴 응답 `details.exam`과 `progress.yearSummaries[].exams` | 기존 + 서버 추가 |
| 수강 목록 `enrollments` | `ProgressView.enrollments` (이전 `enrolments`는 응답된 적 없는 미구현 필드였음) | 서버 수정 |
| 학년 표현 | `phase`(ADMISSION_1·2, BEFORE_SCHOOL, CAREER_PICK, SCHOOL, GRAD_READY, ENDED) + `year`·`turn` | 어댑터 정규화 |
| 비포스쿨 일차·복수 선택 | `beforeSchoolDay`; 1~2일차는 `choiceCodes` 2개, 3~5일차는 `choiceCode` 1개 | 어댑터 정규화 |
| 직무 코드 | `initialCareerCode`·`currentCareerCode`, 추천은 `career-preference.recommendations`(항목마다 `label`·`summary`). 직무는 11종이며 `AI`는 "인공지능 개발자", 새 코드 `AI_NATIVE`는 "AI 네이티브 개발자"입니다. 프론트 표시 코드 매핑에 `AI_NATIVE`를 추가해야 합니다 | 기존 + 서버 추가 |
| 학과·집중 수업 | `GET /year-transition`의 `options[]`(학과: `SOFTWARE` 소프트웨어개발과·`AI` 인공지능소프트웨어과, 3학년 집중 수업 5종)와 `notice`. `EMBEDDED`·`SECURITY`는 이전 저장 회차용이라 새로 고를 수 없습니다. 학과·집중 수업은 희망 직무를 정하거나 제한하지 않습니다 | 서버 추가 |
| 인생 기록 story | `HistoryPage.items[]`(`logId`, `narrativeStatus` PENDING·SAVED·FAILED, `narrative`) + `nextAfterId` | 어댑터 정규화 |
| AI 생성 상태 | 항목별 `narrativeStatus`; 재시도는 `POST /narratives/retry` 후 history 재조회 | 기존 |
| 턴 진행 가능 여부 | `GET /turns/readiness` | 서버 추가 |

목록에 없는 상태는 프론트에서 합격·완료로 임의 채우지 않습니다.

## 5. 자동 진행 순서

```
loop:
  r = GET /players/{id}/turns/readiness
  if r.version != 보유 player.version → GET /players/{id} 후 다시
  if r.canAdvance → POST /turns/advance (새 Idempotency-Key, X-Expected-Version = r.version)
  else 자동 진행을 멈추고 r.blockers[0]을 화면에 표시
    PHASE         : code가 GRAD_READY면 졸업 화면, 그 외 단계는 해당 화면
    YEAR_CHOICE   : code(MAJOR·TRACK), options 중 선택 → POST /year-transition
    REQUIRED_EVENT: code(FOCUS_*, VACATION_*, NARSHA_*, J01 등) → GET /events/{code} 후 선택
  선택 완료 후 사용자가 켜 둔 자동 진행 의도를 복원
```

- 서버는 필수 선택이 없는 선택적 사건을 `blockers`에 넣지 않습니다. 웹의 일반 자동 진행은 표시된 선택을 최대 10초 기다린 뒤 기존 선택 API로 제출합니다. '다음 결정까지'는 선택적 사건을 건너뛰고 서버가 차단한 필수 선택에서 멈춥니다.
- 서버는 같은 판정으로 턴 요청을 `CHOICE_REQUIRED`로 막으므로, 프론트가 확인을 빠뜨려도 중복 진행되지 않습니다.
- 전공강화캠프 실습 2턴 동안에는 `canAdvance=true`이며 `turn`은 그대로이고 `school.practiceRemaining`만 줄어듭니다.

### 무입력 자동 선택

- 10초 카운트다운과 입력 감지는 웹이 담당합니다. 선택한 항목이 있으면 그 항목을, 없으면 현재 `enabled=true`인 허용 항목 중 하나를 선택합니다. 서버의 결과 판정은 재사용하며 별도 서버 타이머나 능력치 계산을 추가하지 않습니다.
- 자동 제출도 수동 제출과 같은 `Idempotency-Key`·`X-Expected-Version`을 전송합니다. 저장 중·오류 상태에서는 다음 자동 제출을 중단합니다. 현재 웹의 팀 API 정규화는 후속 실제 연동 검증이 필요합니다.
- 비포스쿨 복수 선택은 이미 고른 항목을 유지하고 부족한 개수만 채웁니다. 사용자가 직접 입력해야 하는 면접 답변, 졸업 확정, 자퇴 관련 항목의 자동 추첨은 하지 않습니다. 나르샤 기타 주제는 임의로 만들지 않습니다.
- 클릭·키 입력·스크롤 시 10초로 초기화하고, 대화창·백그라운드 탭·저장·오류 중에는 멈춥니다. 사용자는 자동 선택을 끄거나 다시 켤 수 있습니다.

## 6. 인생 기록

- `afterId=0`부터 `nextAfterId`가 null일 때까지 이어 읽습니다. `logId`는 오름차순이며 재조회해도 같습니다.
- `narrativeStatus`가 PENDING이면 생성 중, FAILED면 실패 표시와 재시도 버튼, SAVED면 `narrative`를 그대로 보여 줍니다. 저장된 서사는 새로고침 후에도 동일합니다.

## 7. 검증

- 서버: `./gradlew build`
- `FullPlaythroughHttpTest`는 임의 포트의 실제 서버에서 위 자동 진행 순서대로 회차 생성 → 입학 판정·학교 면접 → 비포스쿨 5일 → 전체 11개 직무 중 희망 직무 선택 → 1~3학년 → 졸업·엔딩까지 진행합니다. 정규 72턴, 캠프 2추가턴, MAJOR·TRACK 선택, 학년별 시험 6회, 재조회 일치, 이력 ID 안정성을 국내 현장실습·실리콘밸리 경로 각각에서 확인합니다. 키 없는 템플릿 서사 기준입니다.
- 프론트 빌드, PC·모바일 화면 확인, 취업 엔딩·자퇴 경로, 학교 근거 카탈로그(#23·#28)의 검증 환경은 `frontend/README.md`와 관련 이슈에 별도로 기록합니다. 데모·별도 로컬 Spring 테스트를 팀 서버 연결 완료로 간주하지 않습니다.
