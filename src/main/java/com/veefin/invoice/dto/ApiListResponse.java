package com.veefin.invoice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic API Response for all listing endpoints with pagination
 * @param <T> The type of content in the list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiListResponse<T> {
    
    private boolean success;
    private DataWrapper<T> data;
    private String message;
    
    /**
     * Inner class to wrap content and pagination
     * @param <T> The type of content in the list
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataWrapper<T> {
        private List<T> content;
        private PaginationDTO pagination;
    }
    
    /**
     * Helper method to create a successful response
     */
    public static <T> ApiListResponse<T> success(List<T> content, PaginationDTO pagination, String message) {
        return ApiListResponse.<T>builder()
                .success(true)
                .data(DataWrapper.<T>builder()
                        .content(content)
                        .pagination(pagination)
                        .build())
                .message(message)
                .build();
    }
    
    /**
     * Helper method to create an error response
     */
    public static <T> ApiListResponse<T> error(String message) {
        return ApiListResponse.<T>builder()
                .success(false)
                .data(null)
                .message(message)
                .build();
    }
}

