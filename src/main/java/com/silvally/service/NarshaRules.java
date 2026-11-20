package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.dto.request.NarshaInput;
import com.silvally.dto.response.NarshaView;
import com.silvally.entity.NarshaProject;
import com.silvally.entity.Player;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 나르샤 규칙. 주제 후보·역할, 직접 입력 검증, 완성도·순위 계산을 모아 상태를 바꾸지 않는다.
 *
 * <p>주제 후보는 학교 자료(입학설명회 25~26쪽의 나르샤 안내, 수업 자료 4쪽의 주제 규모 예시)를 참고한 가상
 * 게임용 예시이며 실제 학생 작품이 아니다. 완성도·순위 산식도 학교 기준이 아닌 게임용 가상 규칙이다.</p>
 */
@Component
public class NarshaRules {
    public static final String CUSTOM_TOPIC = "CUSTOM";
    public static final String DEFAULT_ROLE = "TEAM_MEMBER";
    public static final String RANK_SOURCE = "GAME_RULE";
    public static final int TOPIC_MIN_LENGTH = 2;
    public static final int TOPIC_MAX_LENGTH = 80;
    /** 순위를 매기는 2학년 프로젝트에서 소프트웨이브에 나갈 수 있는 상위 팀 수. */
    public static final int EXHIBIT_RANK_LIMIT = 3;
    static final int WORK_POINT = 2;
    static final int WORK_CAP = 6;
    static final int MENTORING_POINT = 3;
    static final int MENTORING_CAP = 2;

    public static final String NOTICE = "나르샤 주제·완성도·순위는 학교 공식 기준이 아닌 게임용 가상 규칙입니다. "
            + "주제 후보는 실제 학생 작품이 아닌 예시이며, 직접 입력한 주제는 기록용 문자열로만 저장됩니다.";

    private static final List<NarshaView.Role> ROLES = List.of(
            new NarshaView.Role("PLANNER", "기획·일정 관리"),
            new NarshaView.Role("FRONTEND", "화면 개발"),
            new NarshaView.Role("BACKEND", "서버·데이터 개발"),
            new NarshaView.Role("DESIGN", "디자인·사용자 경험"),
            new NarshaView.Role("AI_DATA", "AI·데이터"),
            new NarshaView.Role("QA_DOC", "검증·문서화"),
            new NarshaView.Role(DEFAULT_ROLE, "팀원"));

    private static final List<NarshaView.Topic> TOPICS = List.of(
            new NarshaView.Topic("EXPENSE_PLATFORM", "개인·모임 지출 관리 플랫폼",
                    "개인 가계부와 모임 정산을 한곳에서 관리하는 서비스",
                    List.of("지출 기록·통계", "모임 정산", "알림", "관리자 화면"), List.of("PLANNER", "FRONTEND", "BACKEND", "QA_DOC")),
            new NarshaView.Topic("INTERVIEW_PLATFORM", "IT 면접 연습 플랫폼",
                    "질문 생성·답변 기록·피드백을 제공하는 면접 연습 서비스",
                    List.of("질문 은행", "답변 기록", "피드백 요약", "회원 관리"), List.of("PLANNER", "BACKEND", "AI_DATA", "DESIGN")),
            new NarshaView.Topic("TRAVEL_PLATFORM", "여행 일정 공유 플랫폼",
                    "일정 계획과 경비 공유, 후기 기록을 묶은 여행 서비스",
                    List.of("일정 편집", "경비 공유", "지도 연동", "후기"), List.of("PLANNER", "FRONTEND", "BACKEND", "DESIGN")),
            new NarshaView.Topic("PRIVACY_VISION", "영상 속 개인정보 가림 도구",
                    "영상에서 얼굴·글자 같은 개인정보를 찾아 가려 주는 도구",
                    List.of("객체 인식", "처리 파이프라인", "결과 미리보기", "성능 측정"), List.of("AI_DATA", "BACKEND", "FRONTEND", "QA_DOC")),
            new NarshaView.Topic("AI_CALL_TRAINING", "AI 전화 응대 훈련 서비스",
                    "가상 고객과 통화하며 응대 연습을 하고 평가를 받는 서비스",
                    List.of("음성 대화", "시나리오 관리", "평가 리포트", "운영 대시보드"), List.of("AI_DATA", "BACKEND", "DESIGN", "PLANNER")),
            new NarshaView.Topic("CAMPUS_COMMUNITY", "교내 정보 공유 커뮤니티",
                    "공지·동아리·분실물을 모아 보는 학교 생활 서비스",
                    List.of("게시판", "알림", "동아리 모집", "관리자 기능"), List.of("PLANNER", "FRONTEND", "BACKEND", "DESIGN")));

    private static final List<Map<String, String>> SOURCES = List.of(
            Map.of("code", "NARSHA", "title", "입학설명회 · 1·2학년 나르샤, 25~26쪽",
                    "fact", "1학년 2학기 자유·클론코딩 프로젝트와 교내 발표, 2학년 1~2학기 자유 프로젝트와 ICT융합엑스포·서울 소프트웨이브, 팀 4~7명으로 안내됩니다."),
            Map.of("code", "TOPIC_SCALE", "title", "수업 자료 4쪽 주제 예시",
                    "fact", "다중 기능·역할 분담·협업이 필요한 주제의 규모를 참고했습니다. 학교 수상작이나 실제 학생 작품을 플레이어가 만든 것으로 서술하지 않습니다."));

    public List<NarshaView.Topic> topics() {
        return TOPICS;
    }

    public List<NarshaView.Role> roles() {
        return ROLES;
    }

    public List<Map<String, String>> sources() {
        return SOURCES;
    }

    public Map<String, Object> customTopicRule() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("topicCode", CUSTOM_TOPIC);
        rule.put("minLength", TOPIC_MIN_LENGTH);
        rule.put("maxLength", TOPIC_MAX_LENGTH);
        rule.put("normalization", "앞뒤 공백을 지우고 연속 공백·줄바꿈은 공백 하나로 합친 뒤 글자 수를 셉니다.");
        rule.put("note", "직접 입력한 주제는 기록용 문자열이며 능력치·점수·순위에 영향을 주지 않습니다.");
        return rule;
    }

    /** 선택한 주제·역할을 검증해 저장할 값으로 바꾼다. */
    public Resolved resolve(NarshaInput input) {
        if (input == null || input.topicCode() == null || input.topicCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "나르샤 주제를 선택하거나 직접 입력해야 합니다.");
        }
        String role = input.role() == null || input.role().isBlank() ? DEFAULT_ROLE : input.role();
        if (ROLES.stream().noneMatch(r -> r.code().equals(role))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "제시된 역할이 아닙니다: " + role);
        }
        if (CUSTOM_TOPIC.equals(input.topicCode())) {
            return new Resolved(NarshaProject.SOURCE_CUSTOM, CUSTOM_TOPIC, normalizeTopic(input.customTopic()), role);
        }
        if (input.customTopic() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "후보 주제를 고른 경우 직접 입력 주제를 함께 보낼 수 없습니다.");
        }
        NarshaView.Topic topic = TOPICS.stream().filter(t -> t.code().equals(input.topicCode())).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "제시된 주제 후보가 아닙니다: " + input.topicCode()));
        return new Resolved(NarshaProject.SOURCE_CANDIDATE, topic.code(), topic.title(), role);
    }

    /** 공백을 정리한 뒤 길이와 문자 종류를 검사한다. */
    public String normalizeTopic(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "직접 입력 주제를 입력해야 합니다.");
        }
        String topic = raw.strip().replaceAll("\\s+", " ");
        int length = topic.codePointCount(0, topic.length());
        if (length < TOPIC_MIN_LENGTH || length > TOPIC_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "직접 입력 주제는 " + TOPIC_MIN_LENGTH + "~" + TOPIC_MAX_LENGTH + "자여야 합니다.");
        }
        boolean hidden = topic.codePoints().anyMatch(cp -> Character.isISOControl(cp)
                || Character.getType(cp) == Character.FORMAT || Character.getType(cp) == Character.UNASSIGNED);
        if (hidden) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "직접 입력 주제에 사용할 수 없는 문자가 있습니다.");
        }
        return topic;
    }

    /** P·B·C 평균. 팀 프로젝트에 직접 쓰이는 기술·협업 능력만 본다. */
    public int teamStat(Player player) {
        return (player.getStats().getP() + player.getStats().getB() + player.getStats().getC()) / 3;
    }

    /** 완성도 = 팀 능력치 평균 + 작업(최대 6회×2) + 멘토링(최대 2회×3). 0~100. */
    public int quality(int teamStat, int workCount, int mentoringCount) {
        int bonus = Math.min(workCount, WORK_CAP) * WORK_POINT + Math.min(mentoringCount, MENTORING_CAP) * MENTORING_POINT;
        return Math.max(0, Math.min(100, teamStat + bonus));
    }

    /** 완성도 91 이상 1위, 이후 10점마다 한 계단씩 내려간다. 게임용 가상 순위다. */
    public int rank(int quality) {
        return 1 + (100 - quality) / 10;
    }

    public boolean canExhibit(Integer rank) {
        return rank != null && rank <= EXHIBIT_RANK_LIMIT;
    }

    /** 스트레스 계산이 읽는 현재 학년 프로젝트 진행 중 여부. 서비스가 맞춰 두는 Player 상태를 따른다. */
    public static boolean developing(Player player) {
        return player.getYear() == 1 && "STARTED".equals(player.getNarshaStage())
                || player.getYear() == 2 && "RESUMED".equals(player.getNarshaStage());
    }

    /** 발표·순위 발표가 가까운 마감 구간 여부. */
    public static boolean deadline(Player player) {
        return developing(player) && player.getTurn() >= (player.getYear() == 1 ? 20 : 19)
                || player.getYear() == 1 && player.getTurn() == 23 && "WRAPPED".equals(player.getNarshaStage())
                || player.getYear() == 2 && player.getTurn() == 22 && "RANKED".equals(player.getNarshaStage());
    }

    public static Map<String, Object> view(NarshaProject project) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("year", project.getYear());
        row.put("status", project.getStatus().name());
        row.put("topicSource", project.getTopicSource());
        row.put("topicCode", project.getTopicCode());
        row.put("topic", project.getTopic());
        row.put("role", project.getRole());
        row.put("startTurn", project.getStartTurn());
        row.put("endTurn", project.getEndTurn());
        row.put("workCount", project.getWorkCount());
        row.put("mentoringCount", project.getMentoringCount());
        row.put("quality", project.getQuality());
        row.put("rank", project.getRank());
        row.put("rankSource", project.getRankSource());
        row.put("exhibited", project.isExhibited());
        row.put("ruleType", RANK_SOURCE);
        return row;
    }

    public Optional<NarshaView.Topic> topic(String code) {
        return TOPICS.stream().filter(t -> t.code().equals(code)).findFirst();
    }

    /** 검증을 마친 주제·역할. */
    public record Resolved(String source, String code, String topic, String role) {
    }
}
