package com.tynysai.medicalrecordservice.client.dto;

import lombok.Data;

@Data
public class WrappedResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
