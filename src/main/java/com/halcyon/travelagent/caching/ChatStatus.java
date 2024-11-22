package com.halcyon.travelagent.caching;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatStatus {
    private ChatStatusType type;
    private List<String> data;
}
