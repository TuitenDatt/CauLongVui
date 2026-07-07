package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.BookingHoldRequest;
import com.example.CauLongVui.dto.BookingHoldResponse;
import com.example.CauLongVui.dto.SlotStateDTO;
import com.example.CauLongVui.service.BookingHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingHoldController {

    private final BookingHoldService bookingHoldService;

    /**
     * SSE endpoint to stream slot availability updates.
     */
    @GetMapping(value = "/availability/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SlotStateDTO>> streamUpdates() {
        return bookingHoldService.streamSlotUpdates()
                .map(state -> ServerSentEvent.<SlotStateDTO>builder()
                        .data(state)
                        .build())
                // Keep-alive every 20 seconds
                .mergeWith(Flux.interval(Duration.ofSeconds(20))
                        .map(i -> ServerSentEvent.<SlotStateDTO>builder().comment("keep-alive").build()));
    }

    /**
     * REST endpoint to hold a slot.
     */
    @PostMapping("/hold")
    public ApiResponse<BookingHoldResponse> holdSlot(@RequestBody BookingHoldRequest request) {
        BookingHoldResponse response = bookingHoldService.holdSlot(request);
        return response.isSuccess()
                ? ApiResponse.success(response.getMessage(), response)
                : ApiResponse.error(response.getMessage());
    }

    /**
     * REST endpoint to release a slot.
     */
    @PostMapping("/release/{holdId}")
    public ApiResponse<Void> releaseSlot(@PathVariable String holdId) {
        bookingHoldService.releaseSlot(holdId);
        return ApiResponse.success("Da bo giu cho.", null);
    }

    /**
     * REST endpoint to get active holds for the schedule grid.
     */
    @GetMapping("/holds")
    public ApiResponse<List<SlotStateDTO>> getActiveHolds(
            @RequestParam(name = "date") LocalDate date,
            @RequestParam(name = "courtId", required = false) Long courtId) {
        List<SlotStateDTO> holds = courtId == null
                ? bookingHoldService.getHeldSlots(date)
                : bookingHoldService.getHeldSlots(courtId, date);
        return ApiResponse.success(holds);
    }
}
