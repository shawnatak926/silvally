package com.silvally.service;

import com.silvally.entity.CommandLog;
import com.silvally.entity.CourseEnrollment;
import com.silvally.entity.CourseStatus;
import com.silvally.entity.Player;
import com.silvally.repository.CommandLogRepository;
import com.silvally.repository.CourseEnrollmentRepository;
import com.silvally.repository.PlayerProjectRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class NarrativeContextCollector {

    private final CommandLogRepository commandLogs;
    private final PlayerProjectRepository projects;
    private final CourseEnrollmentRepository enrollments;

    public NarrativeContextCollector(CommandLogRepository commandLogs, PlayerProjectRepository projects,
                                     CourseEnrollmentRepository enrollments) {
        this.commandLogs = commandLogs;
        this.projects = projects;
        this.enrollments = enrollments;
    }

    public NarrativeContext collect(Player player, CommandLog sceneLog) {
        List<NarrativeContext.Project> playerProjects = projects.findByPlayerId(player.getId()).stream()
                .map(p -> new NarrativeContext.Project(p.getProjectCode(), p.getYear(), p.getStatus().name(),
                        p.getQuality()))
                .sorted(Comparator.comparingInt(NarrativeContext.Project::year)
                        .thenComparing(NarrativeContext.Project::code))
                .toList();
        List<String> certificates = enrollments.findByPlayerId(player.getId()).stream()
                .filter(e -> e.getStatus() == CourseStatus.PASSED)
                .map(CourseEnrollment::getCourseCode)
                .sorted()
                .toList();
        List<NarrativeContext.RecentChoice> recentChoices = commandLogs
                .findTop5ByPlayerIdAndKindAndIdLessThanOrderByIdDesc(player.getId(), "EVENT", sceneLog.getId())
                .reversed().stream()
                .map(log -> new NarrativeContext.RecentChoice(log.getYear(), log.getTurn(),
                        log.getBeforeSchoolDay(), log.getSubjectCode(), log.getChoiceCode(), log.getResultCode()))
                .toList();
        return new NarrativeContext(player.getName(), player.getPhase().name(), player.getYear(), player.getTurn(),
                player.getBeforeSchoolDay(), player.getStats().values(), player.getAdmissionTrack(),
                player.getMajor(), player.getTrack(), player.getCurrentCareerCode(), playerProjects,
                certificates, recentChoices);
    }
}
