package com.bytebites.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomApiResponse<T> {
    private boolean success;
    private String message;
    private int statusCode;
    private T data;
}