package com.ecotrack.report.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "collection_points")
public class CollectionPoint {

    @Id
    private UUID id;
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String address;
    private String wasteTypes;
    private boolean active;
    private Instant createdAt;

    protected CollectionPoint() {
    }

    public CollectionPoint(String name, String description, double latitude, double longitude, String address,
                           Collection<WasteCategory> wasteTypes, Instant now) {
        this.id = UUID.randomUUID();
        this.active = true;
        this.createdAt = now;
        update(name, description, latitude, longitude, address, wasteTypes);
    }

    public void update(String name, String description, double latitude, double longitude, String address,
                       Collection<WasteCategory> wasteTypes) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.wasteTypes = wasteTypes.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    public void deactivate() {
        this.active = false;
    }

    public Set<WasteCategory> acceptedTypes() {
        Set<WasteCategory> types = EnumSet.noneOf(WasteCategory.class);
        Arrays.stream(wasteTypes.split(",")).filter(s -> !s.isBlank()).map(WasteCategory::valueOf).forEach(types::add);
        return types;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
