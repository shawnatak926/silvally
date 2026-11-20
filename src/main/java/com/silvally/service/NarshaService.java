package com.silvally.service;

import com.silvally.dto.request.NarshaActionRequest;
import com.silvally.dto.request.NarshaInput;
import com.silvally.dto.response.MutationResult;
import com.silvally.dto.response.NarshaView;
import com.silvally.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 학년별 나르샤 프로젝트의 주제·작업·완성도·순위·전시 기록. */
public interface NarshaService {

    NarshaView getNarsha(UUID playerId);

    /** 현재 학년 프로젝트에 작업 또는 멘토링 한 번(AP 1). */
    MutationResult act(UUID playerId, NarshaActionRequest request, long expectedVersion, String idempotencyKey);

    /** 저장된 두 학년 기록. 진행 현황·졸업 요약이 같은 값을 쓴다. */
    List<Map<String, Object>> projects(UUID playerId);

    // 아래는 회차 잠금·명령 트랜잭션 안에서 이벤트 선택 처리기가 호출한다.

    /** 현재 학년 프로젝트를 새로 시작한다. */
    void start(Player player, NarshaInput input, Map<String, Object> details);

    /** 1학년 프로젝트를 마무리하고 완성도를 확정한다. */
    void wrap(Player player, Map<String, Object> details);

    /** 2학년 프로젝트의 완성도로 순위를 확정한다. */
    void rank(Player player, Map<String, Object> details);

    /** 소프트웨이브 전시에 참가했다고 기록한다. */
    void exhibit(Player player, Map<String, Object> details);

    /** 학교 사건 선택이 나르샤 작업·멘토링으로 이어지면 현재 프로젝트에 센다. */
    void recordSchoolChoice(Player player, String choiceCode);
}
