package ru.practicum.model;

import org.springframework.stereotype.Component;
import ru.practicum.HitDto;
import ru.practicum.NewHitDto;
import ru.practicum.StatsDto;

import java.time.LocalDateTime;

@Component
public class HitMapper {
    public Hit toHit(NewHitDto hitDto) {
        return Hit.builder()
                .ip(hitDto.getIp())
                .app("ewm-service")
                .uri(hitDto.getUri())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public HitDto toHitDto(Hit hit) {
        return HitDto.builder()
                .app(hit.getApp())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .uri(hit.getUri())
                .build();
    }

    public StatsDto toStatsDto(Stats hit) {
        return StatsDto.builder()
                .uri(hit.getUri())
                .app(hit.getApp())
                .hits(hit.getHits())
                .build();
    }
}
