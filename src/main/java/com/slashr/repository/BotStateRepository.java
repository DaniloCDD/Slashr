package com.slashr.repository;

import com.slashr.model.BotState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotStateRepository extends JpaRepository<BotState, String> {
}
