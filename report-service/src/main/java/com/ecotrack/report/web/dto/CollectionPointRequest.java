package com.ecotrack.report.web.dto;

import com.ecotrack.report.domain.WasteCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CollectionPointRequest(
        @NotBlank @Size(max = 120) String name,
        String description,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Size(max = 200) String address,
        @NotEmpty Set<WasteCategory> wasteTypes) {
}
