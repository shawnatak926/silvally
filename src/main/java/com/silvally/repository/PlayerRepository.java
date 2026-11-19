package com.silvally.repository;

import com.silvally.entity.Player;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Optional<Player> findByCreateKey(String createKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.id = :id")
    Optional<Player> findByIdForUpdate(@Param("id") UUID id);

    /**
     * 생성 응답 캐시 저장. 생성 직후의 버전(0)을 유지하도록 벌크 업데이트로 @Version을 거치지 않는다.
     */
    /**
     * 최초 직무 추천 저장. 조회만으로 회차 버전이 오르지 않도록 벌크 업데이트를 쓰고, 이미 저장돼 있으면 건드리지 않는다.
     */
    @Modifying
    @Query("update Player p set p.recommendationJson = :json where p.id = :id and p.recommendationJson is null")
    int saveRecommendationIfAbsent(@Param("id") UUID id, @Param("json") String json);

    @Modifying
    @Query("update Player p set p.createResponseJson = :json where p.id = :id")
    int saveCreateResponse(@Param("id") UUID id, @Param("json") String json);
}
