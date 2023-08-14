package ru.practicum.service;

import ru.practicum.dto.RequestDto;
import ru.practicum.dto.UpdateRequestDtoRequest;
import ru.practicum.dto.UpdateRequestDtoResult;
import ru.practicum.model.Event;
import ru.practicum.model.User;

import java.util.List;

public interface RequestService {
    RequestDto createRequest(Long eventId, Long userId);

    RequestDto cancel(Long requestId, Long userId);

    UpdateRequestDtoResult update(Long eventId, Long userId, UpdateRequestDtoRequest requestDto);

    List<RequestDto> getByUser(Long userId);

    List<RequestDto> getByEvent(Long userId, Long eventId);

    public User getUser(Long id);

    public Event saveEvent(Event event);

    public Event getEventById(Long id);


}
