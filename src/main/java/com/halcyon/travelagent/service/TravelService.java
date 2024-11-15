package com.halcyon.travelagent.service;

import com.halcyon.travelagent.caching.CacheManager;
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
    private final CacheManager cacheManager;

    public List<Travel> getUserTravels(long userId) {
        return travelRepository.findAllByCreatorIdOrderByCreatedAt(userId);
    }

    public void create(CallbackQuery callbackQuery) {
        Travel travel = Travel.builder()
                .name("")
                .description("Описание отсутствует")
                .creatorId(callbackQuery.getFrom().getId())
                .build();

        travel = travelRepository.save(travel);

        saveStatus(travel.getId(), ChatStatusType.TRAVEL_NAME, callbackQuery);
    }

    public void saveStatus(long travelId, ChatStatusType type, CallbackQuery callbackQuery) {
        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(type)
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .data(String.valueOf(travelId))
                        .build()
        );
    }

    public void changeNameAndRemoveStatus(long travelId, String newName, long chatId) {
        changeName(travelId, newName);
        cacheManager.removeChatStatus(chatId);
    }

    public void changeName(long travelId, String newName) {
        Travel travel = findById(travelId);
        travel.setName(newName);

        travelRepository.save(travel);
    }

    public Travel findById(long travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new TravelNotFoundException("Travel with this id not found."));
    }

    public void changeDescriptionAndRemoveStatus(long travelId, String newDescription, long chatId) {
        changeDescription(travelId, newDescription);
        cacheManager.removeChatStatus(chatId);
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
