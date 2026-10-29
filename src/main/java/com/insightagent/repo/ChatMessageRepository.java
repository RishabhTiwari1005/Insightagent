package com.insightagent.repo;

import com.insightagent.domain.ChatMessage;
import com.insightagent.domain.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByDatasetOrderByCreatedAtAsc(Dataset dataset);

    void deleteByDataset(Dataset dataset);
}
