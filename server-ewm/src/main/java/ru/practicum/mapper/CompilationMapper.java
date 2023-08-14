package ru.practicum.mapper;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.model.Compilation;

import java.util.List;

public class CompilationMapper {
    public static Compilation toCompilation(NewCompilationDto compilationDto) {
        return Compilation.builder()
            .title(compilationDto.getTitle())
            .pinned(compilationDto.getPinned())
            .build();
    }

    public static CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> shortDtos) {
        return CompilationDto.builder()
            .id(compilation.getId())
            .pinned(compilation.getPinned())
            .title(compilation.getTitle())
            .events(shortDtos)
            .build();
    }
}
