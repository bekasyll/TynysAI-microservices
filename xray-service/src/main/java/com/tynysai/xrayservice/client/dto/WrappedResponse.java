package com.tynysai.xrayservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrappedResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
