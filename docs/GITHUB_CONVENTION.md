# GitHub 컨벤션

## 1. 브랜치

| 브랜치 | 용도 |
| --- | --- |
| `main` | 항상 빌드되는 제출용 브랜치. 직접 push 하지 않고 PR로만 합친다 |
| `feature/<기능>` | 기능 구현 (예: `feature/event-create-api`) |
| `fix/<내용>` | 오류 수정 (예: `fix/event-null-response`) |
| `refactor/<대상>` | 리팩터링 |
| `test/<대상>` | 테스트 코드 작성 |
| `docs/<대상>` | 문서, 보고서 자료 |

- 브랜치 이름은 영문 소문자와 하이픈(`-`)만 쓴다.
- 하나의 브랜치에는 하나의 작업만 담는다.
- 합친 브랜치는 삭제한다.

## 2. 커밋 메시지

### 형식

```
<type>(<scope>): <제목>

<본문 - 선택. 무엇을, 왜 바꿨는지>
```

### type

| type | 의미 |
| --- | --- |
| `feat` | 기능 구현 |
| `fix` | 오류 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 코드 작성·수정 |
| `docs` | 문서 수정 (README, 보고서 자료) |
| `chore` | 빌드 설정, 의존성, 설정 파일 |
| `style` | 포맷·공백 등 동작과 무관한 변경 |

### 규칙

1. `scope`에는 변경 대상(도메인 또는 계층)을 쓴다. 예: `event`, `common`, `build`
2. 제목은 50자 이내로, 무엇을 했는지 구체적으로 쓴다. 끝에 마침표를 붙이지 않는다.
3. 제목만 보고 변경 대상과 내용을 알 수 있어야 하고, 실제 코드 변경과 일치해야 한다.
4. 한 커밋에는 독립적으로 설명할 수 있는 작업 하나만 담는다.
5. 같은 작업을 인위적으로 쪼개거나, 의미 없는 변경(공백, 빈 커밋)만으로 커밋하지 않는다.
6. 빌드가 깨진 상태로 커밋하지 않는다. 커밋 전에 `./gradlew build`를 실행한다.

### 예시

```
feat(event): 행사 등록 POST API와 요청·응답 DTO 구현
fix(event): 존재하지 않는 행사 조회 시 500 대신 404 반환
test(event): 행사 서비스 조회 실패 예외 테스트 추가
refactor(common): 컨트롤러별 중복 응답 생성을 ApiResponse로 통일
chore(build): H2 의존성과 JPA 감사 설정 추가
```

### 나쁜 예시

```
update
수정
feat: 작업 중
fix: 오타 수정 (한 파일의 공백만 변경)
```

## 3. 커밋 횟수 (평가 기준)

- 1주차(11/9~11/13) 개인별 유효 커밋 2건 이상
- 2주차(11/16~11/20) 개인별 유효 커밋 2건 이상
- 유효 커밋은 실제 기능 구현, 오류 수정, 리팩터링, 테스트 작성처럼 의미 있는 작업 단위여야 한다.

## 4. Pull Request

1. 제목은 커밋 메시지와 같은 형식으로 쓴다. 예: `feat(event): 행사 CRUD API 구현`
2. PR 템플릿(`.github/pull_request_template.md`)의 항목을 채운다.
3. 합치기 전에 `./gradlew build`가 성공해야 한다.
4. 팀원 1명 이상의 확인을 받고 합친다.
5. 합칠 때는 `Squash and merge`가 아니라 `Rebase and merge` 또는 `Merge commit`을 써서 개인 커밋 이력이 남게 한다.

## 5. 코드 네이밍 (접미사 통일)

| 종류 | 이름 | 예 |
| --- | --- | --- |
| 컨트롤러 | `~Controller` | `EventController` |
| 서비스 인터페이스 | `~Service` | `EventService` |
| 서비스 구현체 | `~ServiceImpl` | `EventServiceImpl` |
| 리포지토리 | `~Repository` | `EventRepository` |
| 엔티티 | 접미사 없음 | `Event` |
| 요청 DTO | `~Request` | `EventCreateRequest` |
| 응답 DTO | `~Response` | `EventResponse` |
| 테스트 | `~Test` | `EventServiceTest` |

- 패키지는 `com.silvally.<도메인>.<controller|service|repository|entity|dto>`로 나눈다.
- 공통 클래스(`ApiResponse`, `ErrorResponse`, `BaseEntity`, `GlobalExceptionHandler`)는 `com.silvally.common`에 두고 재사용한다.
- 요청·응답에는 DTO만 쓰고 엔티티를 외부에 노출하지 않는다.

## 6. 제출 전 체크리스트

- [ ] `./gradlew build`가 성공한다
- [ ] H2 더미 데이터 `data.sql`이 들어 있다
- [ ] 저장소가 Public으로 전환되어 있다
- [ ] 실행에 필요한 조치(환경 변수, 계정, 초기 데이터)를 보고서에 적었다
- [ ] 개인별 유효 커밋이 주차별 2건 이상이다
