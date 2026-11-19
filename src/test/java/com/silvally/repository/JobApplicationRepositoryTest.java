package com.silvally.repository;

import com.silvally.entity.ApplicationStatus;
import com.silvally.entity.JobApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class JobApplicationRepositoryTest {

    @Autowired
    private JobApplicationRepository repository;

    @Test
    @DisplayName("지원 내역은 회차별로 기업 코드 순서로 조회하고 다른 회차 것은 섞이지 않는다")
    void findByPlayer_orderedByCompanyAndIsolatedPerPlayer() {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        repository.save(application(player, "PIXEL_LAB", true));
        repository.save(application(player, "BYTE_WORKS", true));
        JobApplication foreign = repository.save(application(other, "BYTE_WORKS", true));

        assertThat(repository.findByPlayerIdOrderByCompanyCodeAsc(player))
                .extracting(JobApplication::getCompanyCode)
                .containsExactly("BYTE_WORKS", "PIXEL_LAB");
        assertThat(repository.findByIdAndPlayerId(foreign.getId(), player)).isEmpty();
        assertThat(repository.existsByPlayerIdAndCompanyCode(player, "PIXEL_LAB")).isTrue();
        assertThat(repository.existsByPlayerIdAndCompanyCode(player, "DEEP_MIND_KR")).isFalse();
    }

    @Test
    @DisplayName("진행 중 상태만 골라 조회하고 같은 기업에 두 번 지원하면 제약 위반이다")
    void findByStatus_andUniqueCompanyPerPlayer() {
        UUID player = UUID.randomUUID();
        repository.save(application(player, "BYTE_WORKS", true));
        repository.save(application(player, "PIXEL_LAB", false));

        assertThat(repository.findByPlayerIdAndStatusIn(player, List.of(ApplicationStatus.DOC_PASSED)))
                .extracting(JobApplication::getCompanyCode)
                .containsExactly("BYTE_WORKS");

        assertThatThrownBy(() -> repository.saveAndFlush(application(player, "BYTE_WORKS", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static JobApplication application(UUID playerId, String company, boolean passed) {
        return JobApplication.apply(playerId, company, "BACKEND", 21, 45, passed ? 32 : 10, passed);
    }
}
