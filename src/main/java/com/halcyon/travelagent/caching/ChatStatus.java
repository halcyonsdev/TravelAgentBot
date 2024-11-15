package com.halcyon.travelagent.caching;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatStatus {
    private ChatStatusType type;
    private Integer messageId;
    private String data;
}
