package com.ecotrack.report.web.dto;

import com.ecotrack.report.domain.CollectionPoint;
import com.ecotrack.report.domain.WasteCategory;

import java.util.Set;
import java.util.UUID;

public record CollectionPointResponse(UUID id, String name, String description, double latitude, double longitude,
                                      String address, Set<WasteCategory> wasteTypes, boolean active) {

    public static CollectionPointResponse from(CollectionPoint p) {
        return new CollectionPointResponse(p.getId(), p.getName(), p.getDescription(), p.getLatitude(),
                p.getLongitude(), p.getAddress(), p.acceptedTypes(), p.isActive());
    }
}
