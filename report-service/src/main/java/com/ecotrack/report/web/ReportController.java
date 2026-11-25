package com.ecotrack.report.web;

import com.ecotrack.report.domain.Report;
import com.ecotrack.report.domain.ReportStatus;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.service.ReportService;
import com.ecotrack.report.web.dto.CreateReportRequest;
import com.ecotrack.report.web.dto.HistoryResponse;
import com.ecotrack.report.web.dto.PageResponse;
import com.ecotrack.report.web.dto.ReportResponse;
import com.ecotrack.report.web.dto.CollectionResponse;
import com.ecotrack.report.web.dto.RejectRequest;
import com.ecotrack.report.web.dto.ValidateBody;
import com.ecotrack.report.collection.CollectionClient;
import com.ecotrack.report.common.NotFoundException;
import com.ecotrack.report.service.PlanningWorkflow;
import com.ecotrack.report.storage.PhotoStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReportService service;
    private final PlanningWorkflow planning;
    private final CollectionClient collections;
    private final PhotoStorage photos;

    public ReportController(ReportService service, PlanningWorkflow planning, CollectionClient collections,
                            PhotoStorage photos) {
        this.service = service;
        this.planning = planning;
        this.collections = collections;
        this.photos = photos;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest body) {
        Report report = service.create(body.reporterName(), body.description(), body.wasteCategory(),
                body.latitude(), body.longitude(), body.address());
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(report.getId()).toUri();
        return ResponseEntity.created(location).body(ReportResponse.from(report));
    }

    @GetMapping
    public PageResponse<ReportResponse> list(@RequestParam(required = false) ReportStatus status,
                                             @RequestParam(required = false) WasteCategory category,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(service.list(status, category, pageable), ReportResponse::from);
    }

    @GetMapping("/{id}")
    public ReportResponse get(@PathVariable UUID id) {
        return ReportResponse.from(service.get(id));
    }

    /** Agent validation: triggers the planning of a collection over gRPC. 202 when planning had to be postponed. */
    @PostMapping("/{id}/validate")
    public ResponseEntity<ReportResponse> validate(@PathVariable UUID id,
                                                   @Valid @RequestBody(required = false) ValidateBody body,
                                                   @RequestHeader(value = "X-Actor", defaultValue = "agent") String actor) {
        Report report = planning.validate(id, body == null ? null : body.scheduledAt(),
                body == null ? null : body.priority(), actor);
        HttpStatus status = report.getStatus() == ReportStatus.PENDING_PLANNING ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ReportResponse.from(report));
    }

    @PostMapping("/{id}/reject")
    public ReportResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest body,
                                 @RequestHeader(value = "X-Actor", defaultValue = "agent") String actor) {
        return ReportResponse.from(service.reject(id, body.comment(), actor));
    }

    /** Live status of the collection, read from collection-service over gRPC. */
    @GetMapping("/{id}/collection")
    public CollectionResponse collection(@PathVariable UUID id) {
        Report report = service.get(id);
        if (report.getCollectionId() == null) {
            throw new NotFoundException("report " + id + " has no collection yet");
        }
        return CollectionResponse.from(collections.get(report.getCollectionId().toString()));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportResponse uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        service.get(id);
        return ReportResponse.from(service.attachPhoto(id, photos.store(id, file)));
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable UUID id) {
        Report report = service.get(id);
        if (report.getPhotoFilename() == null) {
            throw new NotFoundException("report " + id + " has no photo");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(PhotoStorage.contentType(report.getPhotoFilename())))
                .body(photos.load(report.getPhotoFilename()));
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable UUID id) {
        return service.history(id).stream().map(HistoryResponse::from).toList();
    }
}
