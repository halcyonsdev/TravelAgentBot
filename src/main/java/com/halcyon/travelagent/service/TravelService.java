package com.halcyon.travelagent.service;

import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.repository.TravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelRepository travelRepository;

    public List<Travel> getUserTravels(long userId) {
        return travelRepository.findAllByCreatorId(userId);
    }
}
