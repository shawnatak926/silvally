package com.silvally.service;

import com.silvally.dto.response.ExamResultView;
import com.silvally.entity.ExamKind;
import com.silvally.entity.ExamResult;
import com.silvally.entity.Player;
import com.silvally.entity.Stats;
import com.silvally.repository.ExamResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final ExamResultRepository examResults;

    public ExamServiceImpl(ExamResultRepository examResults) {
        this.examResults = examResults;
    }

    @Override
    @Transactional
    public Optional<ExamResultView> onTurnEnd(Player player, List<StatChange> changes) {
        player.addExamPreparation(StatRules.preparationGain(player.getAp(),
                player.hasSemesterFocus() && "A".equals(player.getSemesterFocus())));
        Optional<ExamKind> exam = StatRules.examAt(player.getTurn());
        if (exam.isEmpty()) return Optional.empty();
        ExamKind kind = exam.get();
        int semester = StatRules.semesterOf(player.getTurn());
        if (examResults.existsByPlayerIdAndYearAndSemesterAndKind(player.getId(), player.getYear(), semester, kind)) {
            return Optional.empty();
        }
        Stats stats = player.getStats();
        Map<String, Integer> before = stats.values();
        int preparation = player.getExamPreparation();
        int score = StatRules.examScore(kind, stats, preparation);
        int gradeBefore = stats.getG();
        stats.changeGrade(StatRules.nextGrade(gradeBefore, score, kind));
        changes.addAll(StatChange.between(before, stats.values(), "EXAM_" + kind.name()));
        // SchoolLifeService owns exam pressure as part of the capped turn load.
        player.resetExamPreparation();
        ExamResult saved = examResults.save(new ExamResult(player, semester, kind, preparation, score,
                gradeBefore, stats.getG()));
        return Optional.of(ExamResultView.of(saved, StatRules.RULE_SOURCE));
    }

    @Override
    public List<ExamResultView> results(UUID playerId) {
        return examResults.findByPlayerIdOrderByIdAsc(playerId).stream()
                .map(result -> ExamResultView.of(result, StatRules.RULE_SOURCE))
                .toList();
    }
}
