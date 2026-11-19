package com.silvally.entity;

import com.silvally.common.BaseEntity;
import com.silvally.service.StatRules;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "player")
public class Player extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 24)
    private String name;

    @Column(nullable = false)
    private String rulesVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Phase phase;

    @Column(name = "school_year", nullable = false)
    private int year;

    @Column(name = "school_turn", nullable = false)
    private int turn;

    @Column(nullable = false)
    private int ap;

    @Embedded
    private Stats stats;

    @Embedded
    private GrowthProgress growthProgress = new GrowthProgress();

    @Column(length = 16)
    private String trainedStats;

    @org.hibernate.annotations.ColumnDefault("0")
    @Column(nullable = false)
    private int examPreparation;

    private String major;

    private String track;

    @Column(nullable = false)
    private String applicationProfile;

    private String admissionTrack;

    private Integer beforeSchoolDay;

    private String initialCareerCode;

    private String currentCareerCode;

    @Lob
    private String recommendationJson;

    @Column(nullable = false)
    private int kitCount;

    @Column(nullable = false)
    private int resumeLevel;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private String seed;

    private String endingCode;

    @Lob
    private String endingJson;

    @Column(unique = true)
    private String createKey;

    private String createRequestHash;

    @Lob
    private String createResponseJson;

    private UUID lastCommandId;

    private String pendingChoice;
    private String semesterFocus;
    private int focusYear;
    private int focusSemester;
    private boolean vacationPending;
    private int practiceRemaining;
    private String narshaStage;
    private Integer narshaRank;
    private boolean softwarewaveAttended;

    public String getSemesterFocus() { return semesterFocus; }
    public boolean hasSemesterFocus() { return semesterFocus != null && focusYear == year && focusSemester == (turn <= 12 ? 1 : 2); }
    public void chooseSemesterFocus(String focus) { semesterFocus = focus; focusYear = year; focusSemester = turn <= 12 ? 1 : 2; }
    public boolean isVacationPending() { return vacationPending; }
    public void awaitVacation() { vacationPending = true; }
    public void chooseVacation(boolean camp) { vacationPending = false; practiceRemaining = camp ? 2 : 0; }
    public int getPracticeRemaining() { return practiceRemaining; }
    public void finishPracticeTurn() { if (practiceRemaining > 0) practiceRemaining--; }
    public String getNarshaStage() { return narshaStage; }
    public void setNarshaStage(String stage) { narshaStage = stage; }
    public Integer getNarshaRank() { return narshaRank; }
    public void rankNarsha(int rank) { narshaRank = rank; narshaStage = "RANKED"; }
    public boolean isSoftwarewaveAttended() { return softwarewaveAttended; }
    public void attendSoftwarewave() { softwarewaveAttended = true; }


    // 회차 생성 담당도 동일한 초기 상태를 사용할 수 있는 팩터리입니다.
    public static Player admission(String name, String profile) {
        Player player = new Player();
        player.name = name;
        player.applicationProfile = profile == null ? "OPEN" : profile;
        player.rulesVersion = "v1.2";
        player.phase = Phase.ADMISSION_1;
        player.seed = UUID.randomUUID().toString();
        player.stats = StatRules.rollInitial(StatRules.statSeed(player.seed));
        return player;
    }

    private Boolean meisterEligible;
    private Boolean localEligible;
    private Boolean socialEligible;
    private String admissionEvent = "AD01";
    private double scratchScore;
    private double pythonScore;
    private double basicScore;

    @jakarta.persistence.PrePersist
    public void initializeEligibility() {
        if (meisterEligible != null) return;
        var random = new java.util.SplittableRandom(seed.hashCode());
        meisterEligible = random.nextDouble() < .70;
        localEligible = random.nextDouble() < .20;
        socialEligible = random.nextDouble() < .50;
    }

    public boolean eligible(String track) {
        initializeEligibility();
        return switch (track) {
            case "GENERAL" -> true;
            case "MEISTER" -> meisterEligible;
            case "LOCAL" -> localEligible;
            case "SOCIAL" -> socialEligible;
            default -> false;
        };
    }
    public void selectAdmissionTrack(String track) { admissionTrack = track; admissionEvent = "AD02"; }
    public void passDocuments() { phase = Phase.ADMISSION_2; admissionEvent = "AD03"; }
    public void prepare(String event, double score) {
        if (event.equals("AD03")) { scratchScore = score; admissionEvent = "AD04"; }
        else { pythonScore = score; basicScore = (stats.getP() + stats.getA()) / 2.0; admissionEvent = "AD05"; }
    }
    public java.util.Map<String, Double> admissionComponents(double interview) {
        return java.util.Map.of("scratch", scratchScore, "python", pythonScore, "basic", basicScore, "interview", interview);
    }
    public double finalScore(double interview) {
        return scratchScore * .25 + pythonScore * .30 + basicScore * .20 + interview * .25;
    }
    public void finishAdmission(boolean passed) {
        phase = passed ? Phase.BEFORE_SCHOOL : Phase.ENDED;
        beforeSchoolDay = passed ? 1 : null;
        if (!passed) endingCode = "ADMISSION_FAILED";
    }
    public void nextBeforeSchoolDay() {
        beforeSchoolDay++;
        if (beforeSchoolDay > 5) { phase = Phase.CAREER_PICK; beforeSchoolDay = null; }
    }
    public String getAdmissionEvent() { return admissionEvent; }
    public String getAdmissionTrack() { return admissionTrack; }
    public String getRulesVersion() { return rulesVersion; }
    public Phase getPhase() { return phase; }
    public Integer getBeforeSchoolDay() { return beforeSchoolDay; }
    public String getSeed() { return seed; }
    public Stats getStats() { return stats; }
    public boolean isTrained(String stat) { return trainedStats != null && trainedStats.contains("," + stat + ","); }
    public void markTrained(String stat) {
        if (!isTrained(stat)) trainedStats = (trainedStats == null ? "," : trainedStats) + stat + ",";
    }
    public void resetTrainedStats() { trainedStats = null; }
    public int getExamPreparation() { return examPreparation; }
    public void addExamPreparation(int amount) {
        examPreparation = Math.min(StatRules.PREPARATION_CAP, examPreparation + amount);
    }
    public void resetExamPreparation() { examPreparation = 0; }
    public GrowthProgress getGrowthProgress() {
        if (growthProgress == null) growthProgress = new GrowthProgress();
        return growthProgress;
    }
    public int getYear() { return year; }
    public int getTurn() { return turn; }
    public void markCommand(UUID commandId) { lastCommandId = commandId; }
    public int getAp() { return ap; }
    public String getMajor() { return major; }
    public String getTrack() { return track; }

    public void startSchool(int apPerTurn) {
        phase = Phase.SCHOOL;
        year = 1;
        turn = 1;
        ap = apPerTurn;
    }

    public void spendAp(int cost) {
        ap -= cost;
    }

    public void nextTurn(int apPerTurn) {
        turn++;
        ap = apPerTurn;
    }

    public void nextYear(int apPerTurn) {
        year++;
        turn = 1;
        ap = apPerTurn;
    }

    public String getPendingChoice() { return pendingChoice; }
    public String getEndingCode() { return endingCode; }
    public String getEndingJson() { return endingJson; }

    public void finishGraduation(String endingCode, String endingJson) {
        this.phase = Phase.ENDED;
        this.endingCode = endingCode;
        this.endingJson = endingJson;
        this.ap = 0;
    }
    public int getResumeLevel() { return resumeLevel; }
    public String getRecommendationJson() { return recommendationJson; }
    public void storeRecommendation(String recommendationJson) { this.recommendationJson = recommendationJson; }
    public void changeCareer(String roleCode) { this.currentCareerCode = roleCode; }
    /** 희망 직무를 처음 확정하고 재학을 시작한다. */
    public void confirmCareer(String roleCode, int apPerTurn) {
        this.initialCareerCode = roleCode;
        this.currentCareerCode = roleCode;
        startSchool(apPerTurn);
    }
    public String getInitialCareerCode() { return initialCareerCode; }
    public String getCurrentCareerCode() { return currentCareerCode; }

    public void requireChoice(String choiceType) {
        pendingChoice = choiceType;
    }

    public void chooseMajor(String major) {
        this.major = major;
        pendingChoice = null;
    }

    public void chooseTrack(String track) {
        this.track = track;
        pendingChoice = null;
    }

    public void readyToGraduate() {
        phase = Phase.GRAD_READY;
        ap = 0;
    }
    public com.silvally.dto.response.PlayerView view() {
        return new com.silvally.dto.response.PlayerView(id, name, rulesVersion, phase.name(), year, turn,
            ap, version, stats.values(), applicationProfile, admissionTrack, beforeSchoolDay,
            initialCareerCode, currentCareerCode, major, track, resumeLevel, admissionEligibility(),
            new com.silvally.dto.response.PlayerView.SchoolState(hasSemesterFocus() ? semesterFocus : null,
                    vacationPending, practiceRemaining, pendingChoice, narshaStage, narshaRank,
                    softwarewaveAttended, examPreparation));
    }

    public java.util.Map<String, Boolean> admissionEligibility() {
        var result = new java.util.LinkedHashMap<String, Boolean>();
        for (String track : java.util.List.of("GENERAL", "MEISTER", "LOCAL", "SOCIAL")) result.put(track, eligible(track));
        return result;
    }

    protected Player() {
    }

    /**
     * API 회차 생성. admission()과 같은 초기 상태에 멱등 처리용 생성 키·요청 해시를 더한다.
     */
    public static Player create(String name, String applicationProfile,
                                String createKey, String createRequestHash) {
        Player player = admission(name, applicationProfile);
        player.createKey = createKey;
        player.createRequestHash = createRequestHash;
        return player;
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        return version;
    }

    public String getCreateRequestHash() {
        return createRequestHash;
    }

    public String getCreateResponseJson() {
        return createResponseJson;
    }
}
