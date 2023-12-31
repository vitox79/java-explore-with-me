package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventDto;
import ru.practicum.enums.Sorts;
import ru.practicum.enums.State;
import ru.practicum.enums.StateAction;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.SearchEventParams;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository repository;

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final EventMapper mapper;
    private final CategoryMapper categoryMapper;

    private final StatsClient client;

    @Override
    @Transactional
    public EventDto create(NewEventDto newEventDto, Long userId) {
        Event event = mapper.toEvent(newEventDto);
        event.setInitiator(userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(String.format("Категории с id %d не найдено", userId))));
        event.setCategory(categoryRepository.findById(newEventDto.getCategory())
            .orElseThrow(
                () -> new NotFoundException(String.format("Категории с id %d не найдено", newEventDto.getCategory()))));
        event = repository.save(event);

        return mapper.toEventDto(event, UserMapper.toUserShortDto(event.getInitiator()),
            categoryMapper.toCategoryDto(event.getCategory()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAll(List<Long> usersId, List<String> statesStr, List<Long> catsId, String startStr,
                                 String endStr, int from, int size) {
        int pageNumber = (int) Math.ceil((double) from / size);
        List<Event> events;
        List<User> users = null;
        List<Category> categories = null;
        LocalDateTime start = null;
        LocalDateTime end = null;
        List<State> states = new ArrayList<>();
        if (usersId == null && statesStr == null && catsId == null && startStr == null && endStr == null) {
            events = repository.findAll(PageRequest.of(pageNumber, size)).toList();
        } else {
            if (statesStr != null) {
                for (String state : statesStr) {
                    states.add(State.fromString(state));
                }
            }
            if (usersId != null) {
                users = userRepository.findAllById(usersId);
            }
            if (catsId != null) {
                categories = categoryRepository.findAllById(catsId);
            }
            if (startStr != null) {
                start = fromString(startStr);
            }
            if (endStr != null) {
                end = fromString(endStr);
            }
            events = repository.findAllEventsForAdminBy(users, states, categories,
                start, end, PageRequest.of(pageNumber, size));
        }
        return toEventDtoList(events);
    }

    @Override
    public EventDto published(Long id, UpdateEventDto updateEventDto) {
        Event event = getEventById(id);
        if (event.getState() != State.PENDING) {
            throw new ConflictException("Вы не можете опубликовать уже опубликованное или отклонёное событие.");
        }
        return update(event, updateEventDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllByUser(Long userId, int from, int size) {
        int pageNumber = (int) Math.ceil((double) from / size);
        List<Event> events = repository.findByInitiatorId(userId,
            PageRequest.of(pageNumber, size, Sort.by("id").ascending())).toList();
        return toEventDtoList(events);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllPublic(SearchEventParams params, int from, int size,
                                       HttpServletRequest request) {
        List<Event> events = List.of();
        int pageNumber = (int) Math.ceil((double) from / size);
        if (params.getText() == null ||
            params.getText().isBlank() && params.getCatsId() == null && params.getPaid() != null &&
                params.getStartStr() != null && params.getEndStr() != null) {
            if (params.getSortStr() == null) {
                events = repository.findAll(PageRequest.of(pageNumber, size)).toList();
            } else {
                switch (Sorts.fromString(params.getSortStr())) {
                    case VIEWS:
                        events =
                            repository.findAll(PageRequest.of(pageNumber, size, Sort.by("views").ascending())).toList();
                        break;
                    case EVENT_DATE:
                        events = repository.findAll(PageRequest.of(pageNumber, size, Sort.by("eventDate").ascending()))
                            .toList();
                        break;
                }
            }
        } else {
            List<Category> categories = null;
            LocalDateTime start = null;
            LocalDateTime end = null;
            Sorts sort = null;
            if (params.getCatsId() != null) {
                categories = categoryRepository.findAllById(params.getCatsId());
            }
            if (params.getStartStr() != null) {
                start = fromString(params.getStartStr());
            }
            if (params.getEndStr() != null) {
                end = fromString(params.getEndStr());
            }
            if (end != null && start != null && end.isBefore(start)) {
                throw new ValidationException("Окончание диапозона не может быть раньше начала диапозона");
            }
            if (params.getSortStr() != null) {
                sort = Sorts.fromString(params.getSortStr());
            }
            events = repository.findAllEventsForUserBy(params.getText(), params.getPaid(), categories, start, end,
                params.getOnlyAvailable(),
                sort, PageRequest.of(pageNumber, size));
        }
        client.createHit(request);
        events = events.stream()
            .peek(event -> event.setViews(client.getStatsUnique(request.getRequestURI()).getBody()))
            .collect(Collectors.toList());
        repository.saveAll(events);
        return toEventDtoList(events);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getPublicById(Long id, HttpServletRequest request) {
        Event event = repository.findByIdAndStateIn(id, List.of(State.PUBLISHED))
            .orElseThrow(() -> new NotFoundException(String.format("Категории с id %d не найдено", id)));
        client.createHit(request);
        event.setViews(client.getStatsUnique(request.getRequestURI()).getBody());
        saveEvent(event);
        return mapper.toEventDto(event, UserMapper.toUserShortDto(event.getInitiator()),
            categoryMapper.toCategoryDto(event.getCategory()));
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getForUserById(Long userId, Long eventId) {
        Event event = getEventById(eventId);
        if (!userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(String.format("Категории с id %d не найдено", userId))).getId()
            .equals(event.getInitiator().getId())) {
            throw new ValidationException("Вы не являетесь инициатором события.");
        } else {
            return mapper.toEventDto(event, UserMapper.toUserShortDto(event.getInitiator()),
                categoryMapper.toCategoryDto(event.getCategory()));
        }
    }

    @Override
    @Transactional
    public EventDto update(Long userId, Long eventId, UpdateEventDto eventDto) {
        Event event = getEventById(eventId);
        if (!userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(String.format("Категории с id %d не найдено", userId)))
            .getId().equals(event.getInitiator().getId())) {
            throw new ValidationException("Вы не являетесь инициатором события.");
        }
        if (event.getState() == State.PUBLISHED) {
            throw new ConflictException("Невозможно изменить уже опубликованное событие");
        }
        return update(event, eventDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException(String.format("Категории с id %d не найдено", id)));
    }

    @Override
    @Transactional
    public Event saveEvent(Event event) {
        return repository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents(List<Long> ids) {
        if (ids != null) {
            return repository.findAllById(ids);
        } else {
            return List.of();
        }
    }

    @Override
    public List<EventShortDto> getShortEvent(List<Event> events) {
        return events.stream()
            .map(event -> mapper.toEventShortDto(event,
                UserMapper.toUserShortDto(event.getInitiator()),
                categoryMapper.toCategoryDto(event.getCategory())))
            .collect(Collectors.toList());
    }

    private LocalDateTime fromString(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateStr, formatter);
    }

    private List<EventDto> toEventDtoList(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        } else {
            return events.stream()
                .map(event -> mapper.toEventDto(event,
                    UserMapper.toUserShortDto(event.getInitiator()),
                    categoryMapper.toCategoryDto(event.getCategory())))
                .collect(Collectors.toList());
        }
    }

    private EventDto update(Event event, UpdateEventDto eventDto) {
        if (eventDto.getPaid() != null) {
            event.setPaid(eventDto.getPaid());
        }
        if (eventDto.getEventDate() != null) {
            event.setEventDate(eventDto.getEventDate());
        }
        if (eventDto.getAnnotation() != null && !eventDto.getAnnotation().isBlank()) {
            event.setAnnotation(eventDto.getAnnotation());
        }
        if (eventDto.getDescription() != null && !eventDto.getDescription().isBlank()) {
            event.setDescription(eventDto.getDescription());
        }
        if (eventDto.getLocation() != null) {
            event.setLat(eventDto.getLocation().getLat());
            event.setLon(eventDto.getLocation().getLon());
        }
        if (eventDto.getTitle() != null && !eventDto.getTitle().isBlank()) {
            event.setTitle(eventDto.getTitle());
        }
        if (eventDto.getCategory() != null) {
            event.setCategory(categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                    String.format("Категории с id %d не найдено", eventDto.getCategory()))));
        }
        if (eventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(eventDto.getParticipantLimit());
        }
        if (eventDto.getStateAction() != null) {
            if (eventDto.getStateAction() == StateAction.PUBLISH_EVENT) {
                event.setState(State.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (eventDto.getStateAction() == StateAction.REJECT_EVENT ||
                eventDto.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(State.CANCELED);
            } else if (eventDto.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(State.PENDING);
            }
        }
        if (eventDto.getRequestModeration() != null) {
            event.setRequestModeration(eventDto.getRequestModeration());
        }

        return mapper.toEventDto(saveEvent(event),
            UserMapper.toUserShortDto(event.getInitiator()),
            categoryMapper.toCategoryDto(event.getCategory()));
    }
}
