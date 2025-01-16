package dev.langchain4j.example.booking;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class Booking {
    private String bookingNumber;
    private LocalDate bookingBeginDate;
    private LocalDate bookingEndDate;
    private String customerName;
}
