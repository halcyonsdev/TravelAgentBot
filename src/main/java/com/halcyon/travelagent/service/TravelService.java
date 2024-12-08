package com.halcyon.travelagent.service;

import com.halcyon.travelagent.caching.ChatStatus;
import com.halcyon.travelagent.caching.ChatStatusType;
import com.halcyon.travelagent.entity.Travel;
import com.halcyon.travelagent.exception.TravelNotFoundException;
import com.halcyon.travelagent.repository.TravelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelRepository travelRepository;

    public List<Travel> getUserTravels(long userId) {
        return travelRepository.findAllByCreatorIdOrderByCreatedAt(userId);
    }

    public Travel createTravel(CallbackQuery callbackQuery) {
        Travel travel = Travel.builder()
                .name("")
                .description("отсутствует")
                .creatorId(callbackQuery.getFrom().getId())
                .build();

        return travelRepository.save(travel);
    }

    public void changeName(long travelId, String newName) {
        Travel travel = findById(travelId);
        travel.setName(newName);

        travelRepository.save(travel);
    }

    public Travel findById(long travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new TravelNotFoundException("Travel with id=" + travelId + " not found."));
    }

    public void changeDescription(long travelId, String newDescription) {
        Travel travel = findById(travelId);
        travel.setDescription(newDescription);

        travelRepository.save(travel);
    }

    public void deleteTravel(long travelId) {
        travelRepository.deleteById(travelId);
    }
}
