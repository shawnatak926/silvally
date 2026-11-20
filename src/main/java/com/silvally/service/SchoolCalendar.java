package com.silvally.service;

import com.silvally.common.exception.*;
import com.silvally.dto.request.NarshaInput;
import com.silvally.entity.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** 정규 24턴과 방학 실습을 분리하며 기존 턴/학년 전이 API에서 호출합니다. */
@Component
public class SchoolCalendar {
    private final CertificateService certificates;
    private final NarshaService narsha;
    public SchoolCalendar(CertificateService certificates, NarshaService narsha) {
        this.certificates = certificates; this.narsha = narsha;
    }
    public String focusCode(Player p) { return "FOCUS_Y" + p.getYear() + "_S" + (p.getTurn() <= 12 ? 1 : 2); }
    public String vacationCode(Player p) { return "VACATION_Y" + p.getYear() + (p.getTurn() == 12 ? "_SUMMER" : "_WINTER"); }
    public boolean available(Player p, String code) {
        if (p.isVacationPending()) return code.equals(vacationCode(p));
        if (p.getPracticeRemaining() > 0) return false;
        if (!p.hasSemesterFocus()) return code.equals(focusCode(p));
        if (code.startsWith("FOCUS_") || code.startsWith("VACATION_")) return false;
        return switch (code) {
            case "NARSHA_WRAP" -> "STARTED".equals(p.getNarshaStage());
            // 2학년 시작은 1학년 참가·완료 여부와 무관하게 열린다.
            case "NARSHA_RESUME" -> true;
            case "NARSHA_RANK" -> "RESUMED".equals(p.getNarshaStage());
            case "SOFTWAREWAVE" -> "RANKED".equals(p.getNarshaStage()) && p.getNarshaRank() <= 3;
            default -> true;
        };
    }
    public Map<String, Integer> focusGrowth(Player p) {
        return p.hasSemesterFocus() ? Map.of(p.getSemesterFocus(), 1) : Map.of();
    }
    public String apply(Player p, String event, String choice, Map<String, Object> details) {
        return apply(p, event, choice, null, details);
    }
    public String apply(Player p, String event, String choice, NarshaInput narshaInput, Map<String, Object> details) {
        if (narshaInput != null && !(event.equals("NARSHA_START") && choice.equals("JOIN")
                || event.equals("NARSHA_RESUME") && choice.equals("RESUME")))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "나르샤 주제는 프로젝트를 시작하는 선택에만 보낼 수 있습니다.");
        if (event.startsWith("FOCUS_")) {
            String stat = switch (choice) { case "PROGRAMMING" -> "P"; case "COMMUNICATION" -> "C"; case "ACADEMIC" -> "A";
                default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR); };
            p.chooseSemesterFocus(stat);
            details.put("focusStat", stat);
        } else if (event.startsWith("VACATION_")) {
            if (choice.equals("CAMP") && p.getYear() >= 3) throw new BusinessException(ErrorCode.LOCKED, "전공역량 강화 캠프는 1·2학년만 참가합니다.");
            p.chooseVacation(choice.equals("CAMP"));
            if (p.getPracticeRemaining() == 0) return resumeAfterVacation(p, details);
            details.put("practiceRemaining", 2);
            return "CAMP_READY";
        } else switch (event) {
            case "NARSHA_START" -> { if (choice.equals("JOIN")) narsha.start(p, narshaInput, details); }
            case "NARSHA_WRAP" -> narsha.wrap(p, details);
            case "NARSHA_RESUME" -> { if (choice.equals("RESUME")) narsha.start(p, narshaInput, details); }
            // 공식 학교 순위가 아닌 서버의 가상 게임 규칙. 기록된 작업·멘토링과 팀 능력으로 완성도를 내 순위를 정합니다.
            case "NARSHA_RANK" -> narsha.rank(p, details);
            case "SOFTWAREWAVE" -> { if (choice.equals("ATTEND")) narsha.exhibit(p, details); }
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
