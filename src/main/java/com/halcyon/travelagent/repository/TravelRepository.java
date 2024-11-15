package com.halcyon.travelagent.repository;

import com.halcyon.travelagent.entity.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelRepository extends JpaRepository<Travel, Long> {
    List<Travel> findAllByCreatorIdOrderByCreatedAt(long creatorId);
}
