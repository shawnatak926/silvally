package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.entity.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** 정규 24턴과 방학 실습을 분리하며 기존 턴/학년 전이 API에서 호출합니다. */
@Component
public class SchoolCalendar {
    private final CertificateService certificates;
    public SchoolCalendar(CertificateService certificates) { this.certificates = certificates; }
    public String focusCode(Player p) { return "FOCUS_Y" + p.getYear() + "_S" + (p.getTurn() <= 12 ? 1 : 2); }
    public String vacationCode(Player p) { return "VACATION_Y" + p.getYear() + (p.getTurn() == 12 ? "_SUMMER" : "_WINTER"); }
    public boolean available(Player p, String code) {
        if (p.isVacationPending()) return code.equals(vacationCode(p));
        if (p.getPracticeRemaining() > 0) return false;
        if (!p.hasSemesterFocus()) return code.equals(focusCode(p));
        if (code.startsWith("FOCUS_") || code.startsWith("VACATION_")) return false;
        return switch (code) {
            case "NARSHA_WRAP" -> "STARTED".equals(p.getNarshaStage());
            case "NARSHA_RESUME" -> "WRAPPED".equals(p.getNarshaStage());
            case "NARSHA_RANK" -> "RESUMED".equals(p.getNarshaStage());
            case "SOFTWAREWAVE" -> "RANKED".equals(p.getNarshaStage()) && p.getNarshaRank() <= 3;
            default -> true;
        };
    }
    public Map<String, Integer> focusGrowth(Player p) {
        return p.hasSemesterFocus() ? Map.of(p.getSemesterFocus(), 1) : Map.of();
    }
    public String apply(Player p, String event, String choice, Map<String, Object> details) {
        if (event.startsWith("FOCUS_")) {
            String stat = switch (choice) { case "PROGRAMMING" -> "P"; case "COMMUNICATION" -> "C"; case "ACADEMIC" -> "A";
                default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR); };
            p.chooseSemesterFocus(stat);
            details.put("focusStat", stat);
        } else if (event.startsWith("VACATION_")) {
            p.chooseVacation(choice.equals("CAMP"));
            if (p.getPracticeRemaining() == 0) return resumeAfterVacation(p, details);
            details.put("practiceRemaining", 2);
            return "CAMP_READY";
        } else switch (event) {
            case "NARSHA_START" -> { if (choice.equals("JOIN")) p.setNarshaStage("STARTED"); }
            case "NARSHA_WRAP" -> p.setNarshaStage("WRAPPED");
            case "NARSHA_RESUME" -> p.setNarshaStage("RESUMED");
            case "NARSHA_RANK" -> {
                // 공식 학교 순위가 아닌 서버의 가상 게임 규칙. 평균 기술·협업 점수로 고정 순위를 산출합니다.
                int mean = (p.getStats().getP() + p.getStats().getB() + p.getStats().getC()) / 3;
                p.rankNarsha(1 + (100 - mean) / 10);
                details.put("rank", p.getNarshaRank()); details.put("rankSource", "GAME_RULE");
            }
            case "SOFTWAREWAVE" -> { if (choice.equals("ATTEND")) p.attendSoftwarewave(); }
            default -> { }
        }
        return "COMPLETED";
    }
    public boolean endsSemester(Player p) { return p.getTurn() == 12 || p.getTurn() == 24; }
    public String resumeAfterVacation(Player p, Map<String, Object> details) {
        if (p.getTurn() == 12) { p.nextTurn(SchoolRules.AP_PER_TURN); return "SEMESTER_ADVANCED"; }
        details.put("expiredCourses", certificates.expireAtYearEnd(p, p.getYear() + 1));
        if (p.getYear() == 3) { p.readyToGraduate(); return "GRADUATION_READY"; }
        p.nextYear(SchoolRules.AP_PER_TURN);
        p.requireChoice(p.getYear() == 2 ? "MAJOR" : "TRACK");
        details.put("pendingChoice", p.getPendingChoice());
        return "YEAR_ADVANCED";
    }
}
