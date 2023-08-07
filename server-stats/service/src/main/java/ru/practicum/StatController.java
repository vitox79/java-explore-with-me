package ru.practicum;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.StatService;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatController {
    private final StatService service;

    @GetMapping("/statistics")
    public List<StatsDto> getStatistics(
        @RequestParam("start") String startStr,
        @RequestParam("end") String endStr,
        @RequestParam(required = false) List<String> uris,
        @RequestParam(defaultValue = "false", required = false) Boolean unique) {
        log.info("Fetching statistics with parameters: start {}, end {}, uris {}, unique {}",
            startStr, endStr, uris, unique);
        return service.getStatus(startStr, endStr, uris, unique);
    }

    @PostMapping("/record")
    public HitDto createStatRecord(@RequestBody @Valid HitDto dto) {
        log.info("Updating statistics: saving {}", dto);
        return service.recordHit(dto);
    }
}
