package com.veefin.chat_session.repository;

import com.veefin.chat_session.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.userId = :userId ORDER BY cs.updatedAt DESC")
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    void deleteBySessionId(String sessionId);
}
