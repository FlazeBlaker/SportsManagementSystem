package com.managepickle.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private int id;
    private int courtId;
    private String customerName;
    private String customerPhone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalAmount;
    private double paidAmount;
    private boolean noShow;
    private String cafeItems; // JSON string: {"items":[{"name":"Coffee","price":3.50,"qty":2}]}
    private String status; // "CONFIRMED", "CANCELLED", "COMPLETED"
}
