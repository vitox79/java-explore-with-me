package ru.practicum.service;

import ru.practicum.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    List<UserDto> getAllDtoById(List<Long> ids, int from, int size);

    void delete(Long id);

}
