package com.ecotrack.report.service;

import com.ecotrack.report.common.NotFoundException;
import com.ecotrack.report.domain.CollectionPoint;
import com.ecotrack.report.domain.WasteCategory;
import com.ecotrack.report.repo.CollectionPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class CollectionPointService {

    private final CollectionPointRepository points;
    private final Clock clock;

    public CollectionPointService(CollectionPointRepository points, Clock clock) {
        this.points = points;
        this.clock = clock;
    }

    @Transactional
    public CollectionPoint create(String name, String description, double lat, double lon, String address,
                                  Collection<WasteCategory> types) {
        return points.save(new CollectionPoint(name, description, lat, lon, address, types, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<CollectionPoint> listActive(WasteCategory filter) {
        return points.findByActiveTrueOrderByName().stream()
                .filter(p -> filter == null || p.acceptedTypes().contains(filter)).toList();
    }

    @Transactional(readOnly = true)
    public CollectionPoint get(UUID id) {
        return points.findById(id).orElseThrow(() -> new NotFoundException("collection point " + id + " not found"));
    }

    @Transactional
    public CollectionPoint update(UUID id, String name, String description, double lat, double lon, String address,
                                  Collection<WasteCategory> types) {
        CollectionPoint point = get(id);
        point.update(name, description, lat, lon, address, types);
        return points.save(point);
    }

    /** Deactivation instead of deletion keeps historical references valid. */
    @Transactional
    public void deactivate(UUID id) {
        CollectionPoint point = get(id);
        point.deactivate();
        points.save(point);
    }
}
