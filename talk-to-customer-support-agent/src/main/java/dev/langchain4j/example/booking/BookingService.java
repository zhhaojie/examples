package dev.langchain4j.example.booking;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BookingService {

    private static final Booking BOOKING1 = new Booking(
            "MS-777",
            LocalDate.of(2025, 12, 13),
            LocalDate.of(2025, 12, 31),
            "pangzi"
    );

    private static final Booking BOOKING2 = new Booking(
            "MS-888",
            LocalDate.of(2025, 12, 13),
            LocalDate.of(2025, 12, 31),
            "xiaofeng"
    );

    private static final Map<String, Booking> BOOKINGS = new HashMap<>() {{
        put(BOOKING1.getBookingNumber(), BOOKING1);
        put(BOOKING2.getBookingNumber(), BOOKING2);
    }};

    public List<Booking> getBookings(String customerName) {
        List<Booking> bookings = new ArrayList<>();
        for (Map.Entry<String, Booking> stringBookingEntry : BOOKINGS.entrySet()) {
            if (stringBookingEntry.getValue().getCustomerName().equals(customerName)) {
                bookings.add(stringBookingEntry.getValue());
            }
        }
        return bookings;
    }

    public Booking getBookingDetails(String bookingNumber, String customerName) {
        ensureExists(bookingNumber, customerName);
        return BOOKINGS.get(bookingNumber);
    }

    public void cancelBooking(String bookingNumber, String customerName) {
        ensureExists(bookingNumber, customerName);
        BOOKINGS.remove(bookingNumber);
    }

    private void ensureExists(String bookingNumber, String customerName) {
        Booking booking = BOOKINGS.get(bookingNumber);
        if (booking == null) {
            throw new BookingNotFoundException(bookingNumber);
        }

        if (!booking.getCustomerName().equalsIgnoreCase(customerName)) {
            throw new BookingNotFoundException(bookingNumber);
        }
    }
}
