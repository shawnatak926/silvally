package com.silvally.service;

import com.silvally.entity.Player;
import org.springframework.stereotype.Component;
import java.util.SplittableRandom;

@Component
public class AdmissionPolicy {
    public boolean isEligible(Player player, String track) { return player.eligible(track); }

    // 구 회차를 읽는 호출부를 위한 배경 검사. 신규 게임 자격은 Player에 저장한 추첨을 사용합니다.
    public boolean isEligible(String profile, String track) {
        return switch (track) {
            case "GENERAL" -> true;
            case "MEISTER" -> "MAKER".equals(profile);
            case "LOCAL" -> "REGION".equals(profile);
            case "SOCIAL" -> "ACCESS".equals(profile);
            default -> false;
        };
    }
    public double documentScore(String track, double essay, double grades, double record) {
        return switch (track) {
            case "GENERAL" -> essay * .40 + grades * .35 + record * .25;
            case "MEISTER" -> essay * .25 + grades * .25 + record * .50;
            case "LOCAL" -> essay * .30 + grades * .45 + record * .25;
            case "SOCIAL" -> essay * .45 + grades * .30 + record * .25;
            default -> throw new IllegalArgumentException("Unknown track");
        };
    }
    public record Decision(boolean passed, boolean shieldActivated, int rank, int limit,
                           double score, double generalScore, int generalRank) { }
    public Decision decide(String seed, int stage, String track, double score, double generalScore) {
        int limit = stage == 1 ? 144 : 72;
        // 같은 단계에서는 특별전형/일반전형 모두 동일한 경쟁자와 제출 점수를 재사용합니다.
        int rank = rank(seed, stage, score, limit * 2 - 1);
        int generalRank = rank(seed, stage, generalScore, limit * 2 - 1);
        boolean shield = !"GENERAL".equals(track) && rank > limit;
        return new Decision(rank <= limit || shield && generalRank <= limit,
                shield, rank, limit, score, generalScore, generalRank);
    }
    private int rank(String seed, int stage, double score, int competitors) {
        var random = new SplittableRandom(seed.hashCode() * 31L + stage);
        int rank = 1;
        for (int n = 0; n < competitors; n++) if (35 + random.nextDouble() * 60 >= score) rank++;
        return rank;
    }
}
