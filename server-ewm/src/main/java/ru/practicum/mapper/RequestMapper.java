package ru.practicum.mapper;


import ru.practicum.dto.RequestDto;
import ru.practicum.model.Request;

public class RequestMapper {
    public static RequestDto toRequestDto(Request request) {
        return RequestDto.builder()
            .requester(request.getRequester().getId())
            .created(request.getCreated())
            .id(request.getId())
            .event(request.getEvent().getId())
            .status(request.getStatus())
            .build();
    }
}
