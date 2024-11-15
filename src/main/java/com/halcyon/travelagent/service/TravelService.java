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
                .description("Название отсутствует")
                .creatorId(callbackQuery.getFrom().getId())
                .build();

        travel = travelRepository.save(travel);

        cacheManager.saveChatStatus(
                callbackQuery.getMessage().getChatId(),
                ChatStatus.builder()
                        .type(ChatStatusType.TRAVEL_NAME)
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .data(String.valueOf(travel.getId()))
                        .build()
        );
    }

    public void changeName(long travelId, String name, long chatId) {
        Travel travel = findById(travelId);
        travel.setName(name);

        travelRepository.save(travel);
        cacheManager.removeChatStatus(chatId);
    }

    private Travel findById(long travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new TravelNotFoundException("Travel with this id not found."));
    }
}
