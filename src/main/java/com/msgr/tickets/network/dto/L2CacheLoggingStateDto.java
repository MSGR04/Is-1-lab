package com.msgr.tickets.network.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class L2CacheLoggingStateDto {
    private boolean enabled;
    private long hitCount;
    private long missCount;
    private long putCount;
}
