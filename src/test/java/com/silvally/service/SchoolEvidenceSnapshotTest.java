package com.silvally.service;

import com.silvally.dto.request.ChooseEventRequest;
import com.silvally.entity.LifeNarrative;
import com.silvally.repository.LifeNarrativeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SchoolFixture.class)
class SchoolEvidenceSnapshotTest {

    @Autowired SchoolFixture fixture;
    @Autowired ActivityService events;
    @Autowired LifeNarrativeRepository narratives;
    @Autowired ObjectMapper json;

    @Test
    void eachSceneKeepsTheSchoolEvidenceAndStudentStateOfItsOwnMoment() {
        UUID id = fixture.enrolledStudent();
        NarrativeContext focus = context(id, "FOCUS_Y1_S1");

        fixture.modify(id, p -> {
            ReflectionTestUtils.setField(p, "year", 3);
            ReflectionTestUtils.setField(p, "turn", 3);
            p.chooseSemesterFocus("P");
        });
        events.chooseEvent(id, "J01", fixture.version(id), UUID.randomUUID().toString(),
                new ChooseEventRequest("SILICON_VALLEY"));
        NarrativeContext field = context(id, "J01");

        assertThat(focus.year()).isEqualTo(1);
        assertThat(focus.schoolEvidence()).extracting(NarrativeContext.SchoolEvidence::code).containsExactly("CURRICULUM");
        assertThat(field.year()).isEqualTo(3);
        assertThat(field.schoolEvidence()).extracting(NarrativeContext.SchoolEvidence::code)
                .containsExactly("GLOBAL_SELECTION", "GLOBAL_PERIOD_NOTE");
        assertThat(context(id, "FOCUS_Y1_S1")).isEqualTo(focus);
    }

    private NarrativeContext context(UUID playerId, String sceneId) {
        LifeNarrative narrative = narratives.findAll().stream()
                .filter(n -> n.getPlayerId().equals(playerId) && sceneId.equals(n.getSceneId()))
                .findFirst().orElseThrow();
        return json.readValue(narrative.getContextJson(), NarrativeContext.class);
    }
}
