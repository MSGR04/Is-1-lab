package com.msgr.tickets.network.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageDto<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
}
