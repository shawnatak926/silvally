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
    @Modifying
    @Query("update Player p set p.createResponseJson = :json where p.id = :id")
    int saveCreateResponse(@Param("id") UUID id, @Param("json") String json);
}
