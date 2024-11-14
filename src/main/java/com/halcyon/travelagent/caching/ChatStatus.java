package com.halcyon.travelagent.caching;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatStatus {
    private ChatStatusType type;
    private Long messageId;
    private String data;
}
