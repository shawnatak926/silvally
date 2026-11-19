package com.silvally.service;

import com.silvally.dto.response.SchoolInterviewEvaluationView;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 한국어 학교 면접의 결정론적 게임용 루브릭. AI 의미 이해/사실 검증으로 표시하지 않습니다.
 * 완결된 서술에서 항목별 다섯 근거를 각5점으로 평가하며 단어 빈도·답변 길이는 가산하지 않습니다.
 */
@Component
public class SchoolInterviewEvaluator {
    private static final String VERSION = "school-evidence-v1";
    public SchoolInterviewEvaluationView evaluate(String answer) {
        List<String> sentences = Arrays.stream(answer.strip().split("[.!?。！？\\r\\n]+"))
                .map(String::strip).filter(s -> s.length() >= 12 && (s.endsWith("다") || s.endsWith("요")))
                .distinct().toList();
        Map<String, SchoolInterviewEvaluationView.Criterion> criteria = new LinkedHashMap<>();
        criteria.put("motivation", criterion(sentences, "지원|동기|관심", "지원 동기의 서술", List.of(
                signal("개인적인 지원 계기", "계기|불편|때문|필요|느꼈"),
                signal("만들거나 배우려는 구체적인 대상", "API|프로그램|서비스|앱|백엔드|자바|Java|데이터"),
                signal("경험과 목표를 연결한 이유", "바탕|그래서|위해|싶|때문|통해"),
                signal("학교 학습과 지원 목적의 연결", "학교|입학|배우|학습")), "지원 동기가 없"));
        criteria.put("experience", criterion(sentences, "활동|프로젝트|경험|동아리", "활동 경험의 서술", List.of(
                new Signal("본인이 맡은 역할과 행동", text -> contains(text, "제가|저는|직접|담당|맡아") && contains(text, "구현|개발|작성|만들|분석|설계|수정")),
                signal("구체적인 제작·작업 대상", "API|프로그램|서비스|앱|서버|테스트|센서|데이터"),
                signal("협업 또는 작업 과정", "팀원|동료|함께|역할|문서|비교|단계"),
                signal("활동 결과 또는 확인", "동작|완성|확인|성공|개선|검증|배포")), "경험이 없|활동한 적이 없|프로젝트를 하지 않"));
        criteria.put("problemSolving", criterion(sentences, "문제|오류|버그|장애|원인|해결", "문제 상황의 서술", List.of(
                signal("원인 분석·재현 근거", "로그|재현|비교|가설|분석|추적"),
                new Signal("원인에 따른 수정 행동", text -> contains(text, "원인") && contains(text, "수정|변경|분리|제거|해결")),
                signal("수정 후 검증 과정", "테스트|검증|다시 확인|재확인"),
                signal("해결 결과", "확인|성공|개선|정상|한 번만|줄었")), "해결한 적이 없|해결하지 못|문제 해결 경험이 없"));
        criteria.put("schoolPlan", criterion(sentences, "입학|계획|학습|학기|학교", "입학 후 계획의 서술", List.of(
                signal("실행 시기·주기", "학기|매주|주마다|방학|개월|[0-9]+주"),
                signal("구체적인 학습 대상", "Java|자바|Spring|스프링|API|자료구조|데이터베이스|파이썬|네트워크"),
                signal("직접 수행할 학습 행동", "학습|구현|만들|작성|분석|개발|연습"),
                signal("확인할 산출물·목표", "프로젝트|완성|배포|테스트|발표|공유|기록")), "계획이 없|계획은 없"));
        Map<String, Integer> components = new LinkedHashMap<>();
        criteria.forEach((key, value) -> components.put(key, value.score()));
        return new SchoolInterviewEvaluationView(components.values().stream().mapToInt(Integer::intValue).sum(),
                components, "RULE_BASED", VERSION, criteria);
    }
    private record Signal(String label, java.util.function.Predicate<String> matches) { }
    private Signal signal(String label, String terms) { return new Signal(label, text -> contains(text, terms)); }
    private boolean contains(String text, String alternatives) {
        return java.util.regex.Pattern.compile(alternatives, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    private SchoolInterviewEvaluationView.Criterion criterion(List<String> sentences, String relevant,
            String baseReason, List<Signal> signals, String explicitAbsence) {
        List<String> evidence = sentences.stream().filter(s -> contains(s, relevant)).toList();
        String context = String.join(" ", evidence);
        if (evidence.isEmpty() || contains(context, explicitAbsence)) {
            return new SchoolInterviewEvaluationView.Criterion(0, 25, evidence.stream().limit(3).toList(),
                    List.of("확인 가능한 " + baseReason + " 근거가 부족합니다."));
        }
        int score = 5;
        List<String> reasons = new ArrayList<>(); reasons.add(baseReason + " 확인: 5점");
        for (Signal signal : signals) {
            boolean found = signal.matches().test(context);
            if (found) score += 5;
            reasons.add(signal.label() + (found ? " 확인: 5점" : " 근거 부족: 0점"));
        }
        return new SchoolInterviewEvaluationView.Criterion(score, 25,
                evidence.stream().limit(3).map(s -> s.substring(0, Math.min(240, s.length()))).toList(), reasons);
    }
}
