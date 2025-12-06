package com.zant.backend.repository;

import com.zant.backend.model.AssistantState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantStateRepository extends JpaRepository<AssistantState, String> {
}
