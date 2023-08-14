package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository repository;

    private final EventRepository eventRepository;


    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);
        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }
        compilation.setEvents(getAllEvents(newCompilationDto.getEvents()));
        compilation = repository.save(compilation);
        return CompilationMapper.toCompilationDto(compilation,
            getShortEvent(compilation.getEvents()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        int pageNumber = (int) Math.ceil((double) from / size);
        if (pinned == null) {
            return repository.findAll(PageRequest.of(pageNumber, size)).stream()
                .map(compilation -> CompilationMapper.toCompilationDto(compilation,
                    getShortEvent(compilation.getEvents())))
                .collect(Collectors.toList());
        } else {
            return repository.findByPinned(pinned, PageRequest.of(pageNumber, size)).stream()
                .map(compilation -> CompilationMapper.toCompilationDto(compilation,
                    getShortEvent(compilation.getEvents())))
                .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getById(Long id) {
        Compilation compilation = repository.findById(id).orElseThrow(() ->
            new NotFoundException("Данной подборки не существует"));
        return CompilationMapper.toCompilationDto(compilation, getShortEvent(compilation.getEvents()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public CompilationDto update(Long id, NewCompilationDto compilationDto) {
        Compilation compilation = repository.findById(id).orElseThrow(() ->
            new NotFoundException("Данной подборки не существует"));
        if (compilationDto.getTitle() != null) {
            if (compilationDto.getTitle().isBlank() || compilationDto.getTitle().length() > 50) {
                throw new ValidationException("Название не может быть пустым или больше 50");
            } else {
                compilation.setTitle(compilationDto.getTitle());
            }
        }
        if (compilationDto.getPinned() != null) {
            compilation.setPinned(compilationDto.getPinned());
        }
        if (compilationDto.getEvents() != null) {
            compilation.setEvents(getAllEvents(compilationDto.getEvents()));
        }
        compilation = repository.save(compilation);
        return CompilationMapper.toCompilationDto(compilation, getShortEvent(compilation.getEvents()));
    }

    public List<EventShortDto> getShortEvent(List<Event> events) {
        return events.stream().map(
            event -> EventMapper.toEventShortDto(event, UserMapper.toUserShortDto(event.getInitiator()),
                CategoryMapper.toCategoryDto(event.getCategory()))).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents(List<Long> ids) {
        if (ids != null) {
            return eventRepository.findAllById(ids);
        } else {
            return List.of();
        }
    }
}
