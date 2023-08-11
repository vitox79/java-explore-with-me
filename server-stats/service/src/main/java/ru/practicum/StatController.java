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

    @PostMapping("/hit")
    public HitDto addStatistics(@RequestBody @Valid HitDto dto) {
        log.info("Refresh data: saving {}", dto);
        return service.create(dto);
    }

    @GetMapping("/stats")
    public List<StatsDto> getStatistics(@RequestParam("start") String startStr,
                                        @RequestParam("end") String endStr,
                                        @RequestParam(required = false) List<String> uris,
                                        @RequestParam(defaultValue = "false", required = false) Boolean unique) {
        log.info("Get statistics with: start {}, end {}, uris {}, unique {}",
            startStr, endStr, uris, unique);
        return service.getStatus(startStr, endStr, uris, unique);
    }
}
