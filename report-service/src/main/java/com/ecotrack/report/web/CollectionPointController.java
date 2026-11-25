package com.ecotrack.report.web;

import com.ecotrack.report.domain.CollectionPoint;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.service.CollectionPointService;
import com.ecotrack.report.web.dto.CollectionPointRequest;
import com.ecotrack.report.web.dto.CollectionPointResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collection-points")
public class CollectionPointController {

    private final CollectionPointService service;

    public CollectionPointController(CollectionPointService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CollectionPointResponse> create(@Valid @RequestBody CollectionPointRequest body) {
        CollectionPoint point = service.create(body.name(), body.description(), body.latitude(), body.longitude(),
                body.address(), body.wasteTypes());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(point.getId()).toUri();
        return ResponseEntity.created(location).body(CollectionPointResponse.from(point));
    }

    @GetMapping
    public List<CollectionPointResponse> list(@RequestParam(required = false) WasteCategory wasteType) {
        return service.listActive(wasteType).stream().map(CollectionPointResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CollectionPointResponse get(@PathVariable UUID id) {
        return CollectionPointResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    public CollectionPointResponse update(@PathVariable UUID id, @Valid @RequestBody CollectionPointRequest body) {
        return CollectionPointResponse.from(service.update(id, body.name(), body.description(), body.latitude(),
                body.longitude(), body.address(), body.wasteTypes()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }
}
