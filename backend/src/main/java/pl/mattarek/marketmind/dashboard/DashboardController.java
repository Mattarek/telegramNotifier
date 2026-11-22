package pl.mattarek.marketmind.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.mattarek.marketmind.prediction.Mode;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardQueryService queryService;

    public DashboardController(final DashboardQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/dashboard")
    public Views.DashboardView dashboard() {
        return queryService.dashboard();
    }

    @GetMapping("/instruments/{id}/detail")
    public Views.InstrumentDetailView detail(@PathVariable final long id,
                                             @RequestParam(defaultValue = "INVEST") final Mode mode) {
        return queryService.detail(id, mode);
    }

    @GetMapping("/trading")
    public Views.TradingView trading() {
        return queryService.trading();
    }

    @GetMapping("/learning")
    public Views.LearningView learning() {
        return queryService.learning();
    }
}
