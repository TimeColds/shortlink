package com.timecold.shortlink.admin.remote.dto.resp;

import lombok.Data;

import java.util.List;

@Data
public class PageResponseDTO<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;
}
