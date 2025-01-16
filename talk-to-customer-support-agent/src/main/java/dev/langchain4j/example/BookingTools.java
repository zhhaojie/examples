package dev.langchain4j.example;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.booking.Booking;
import dev.langchain4j.example.booking.BookingService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingTools {

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool
    public List<Booking> getBookings(String customerName) {
        return bookingService.getBookings(customerName);
    }

    @Tool
    public Booking getBookingDetails(String bookingNumber, String customerName) {
        return bookingService.getBookingDetails(bookingNumber, customerName);
    }

    @Tool
    public void cancelBooking(String bookingNumber, String customerName) {
        bookingService.cancelBooking(bookingNumber, customerName);
    }

}