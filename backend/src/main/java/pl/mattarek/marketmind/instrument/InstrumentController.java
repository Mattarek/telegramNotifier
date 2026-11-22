package pl.mattarek.marketmind.instrument;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(final InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public List<InstrumentDto> findAll() {
        return instrumentService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentDto create(@Valid @RequestBody final InstrumentRequest request) {
        return instrumentService.create(request);
    }

    @PutMapping("/{id}")
    public InstrumentDto update(@PathVariable final long id, @Valid @RequestBody final InstrumentRequest request) {
        return instrumentService.update(id, request);
    }
}
