package ru.practicum.service;

import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.model.Event;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto compilationDto);

    List<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto getById(Long id);

    void delete(Long id);

    CompilationDto update(Long id, NewCompilationDto compilationDto);

    public List<Event> getAllEvents(List<Long> ids);
}
