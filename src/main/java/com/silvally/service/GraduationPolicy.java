package com.silvally.service;

import com.silvally.common.exception.BusinessException;
import com.silvally.common.exception.ErrorCode;
import com.silvally.entity.JobApplication;
import com.silvally.entity.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 졸업 진로와 엔딩 코드를 정하는 순수 판정 규칙. 상태를 바꾸지 않는다.
 *
 * <ul>
 *     <li>유효한 명시 진로가 AUTO보다 우선한다.</li>
 *     <li>AUTO는 수락 가능한 오퍼가 하나라도 있으면 진로를 정할 수 없어 FINAL_PATH_REQUIRED로 거부한다.</li>
 *     <li>AUTO 우선순위: 포트폴리오 작품이 있으면 개인 개발, 학업(A)이 기준 이상이면 진학, 그 외는 미취업.</li>
 *     <li>수락한 오퍼의 직무가 현재 희망 직무와 다르면 적성 불일치 취업 엔딩이다.</li>
 * </ul>
 */
@Component
public class GraduationPolicy {

    public static final int UNIVERSITY_MIN_ACADEMIC = 60;

    public static final String EMPLOYED = "EMPLOYED";
    public static final String EMPLOYED_MISMATCH = "EMPLOYED_MISMATCH";
    public static final String UNEMPLOYED = "UNEMPLOYED";
    public static final String UNIVERSITY = "UNIVERSITY";
    public static final String INDIE_DEV = "INDIE_DEV";

    /**
     * @param path             확정된 진로(AUTO는 풀려서 들어온다)
     * @param accepted         취업일 때 수락할 오퍼, 그 외에는 null
     * @param endingCode       엔딩 코드
     * @param aptitudeMismatch 수락한 오퍼의 직무가 희망 직무와 다른지 여부
     */
    public record Decision(GraduationPath path, JobApplication accepted, String endingCode,
                           boolean aptitudeMismatch) {
    }

    public Decision decide(GraduationPath requested, UUID applicationId, Player player,
                           List<JobApplication> applications, int portfolioCount) {
        List<JobApplication> offers = applications.stream().filter(JobApplication::isOffer).toList();

        if (requested == GraduationPath.AUTO) {
            return decideAuto(applicationId, player, offers, portfolioCount);
        }
        if (requested != GraduationPath.EMPLOYMENT && applicationId != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "취업이 아닌 진로에는 applicationId를 보낼 수 없습니다.");
        }
        return switch (requested) {
            case EMPLOYMENT -> employ(player, pickOffer(applicationId, applications, offers));
            case INDIE_DEV -> {
                requirePortfolio(portfolioCount);
                yield new Decision(requested, null, INDIE_DEV, false);
            }
            case UNIVERSITY -> new Decision(requested, null, UNIVERSITY, false);
            case UNEMPLOYED -> new Decision(requested, null, UNEMPLOYED, false);
            case AUTO -> throw new IllegalStateException("AUTO는 위에서 처리된다.");
        };
    }

    private Decision decideAuto(UUID applicationId, Player player, List<JobApplication> offers,
                                int portfolioCount) {
        if (applicationId != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "AUTO에는 applicationId를 보낼 수 없습니다. 오퍼를 수락하려면 EMPLOYMENT를 선택하세요.");
        }
        if (!offers.isEmpty()) {
            throw new BusinessException(ErrorCode.FINAL_PATH_REQUIRED, Map.of(
                    "offers", offers.stream().map(offer -> Map.of(
                            "applicationId", offer.getId(),
                            "companyCode", offer.getCompanyCode(),
                            "role", offer.getRole())).toList()));
        }
        if (portfolioCount > 0) {
            return new Decision(GraduationPath.INDIE_DEV, null, INDIE_DEV, false);
        }
        if (player.getStats().getA() >= UNIVERSITY_MIN_ACADEMIC) {
            return new Decision(GraduationPath.UNIVERSITY, null, UNIVERSITY, false);
        }
        return new Decision(GraduationPath.UNEMPLOYED, null, UNEMPLOYED, false);
    }

    private JobApplication pickOffer(UUID applicationId, List<JobApplication> applications,
                                     List<JobApplication> offers) {
        if (applicationId != null) {
            JobApplication target = applications.stream()
                    .filter(application -> application.getId().equals(applicationId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "지원 내역을 찾을 수 없습니다."));
            if (!target.isOffer()) {
                throw new BusinessException(ErrorCode.LOCKED, "수락할 수 없는 지원 상태입니다.",
                        Map.of("applicationId", applicationId, "status", target.getStatus().name()));
            }
            return target;
        }
        if (offers.isEmpty()) {
            throw new BusinessException(ErrorCode.LOCKED, "수락할 오퍼가 없습니다.");
        }
        if (offers.size() > 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "오퍼가 여러 개라 수락할 applicationId를 지정해야 합니다.",
                    Map.of("offerIds", offers.stream().map(JobApplication::getId).toList()));
        }
        return offers.get(0);
    }

    private Decision employ(Player player, JobApplication offer) {
        String hoped = player.getCurrentCareerCode();
        boolean mismatch = hoped != null && !hoped.equals(offer.getRole());
        return new Decision(GraduationPath.EMPLOYMENT, offer, mismatch ? EMPLOYED_MISMATCH : EMPLOYED, mismatch);
    }

    private void requirePortfolio(int portfolioCount) {
        if (portfolioCount < 1) {
            throw new BusinessException(ErrorCode.LOCKED, "포트폴리오에 등록한 완료 작품이 있어야 개인 개발을 선택할 수 있습니다.");
        }
    }
}
