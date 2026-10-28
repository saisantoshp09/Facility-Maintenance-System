package com.facility.property.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.facility.property.domain.Building;
import com.facility.property.domain.Floor;
import com.facility.property.domain.MembershipRole;
import com.facility.property.domain.Property;
import com.facility.property.domain.PropertyMembership;
import com.facility.property.domain.Unit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PropertyDtos {
    private PropertyDtos() {
    }

    public record PropertyRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 255) String addressLine,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 20) String postalCode,
            @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 code") String country) {
    }

    public record PropertyResponse(UUID id, String name, String addressLine, String city, String postalCode,
            String country, boolean active, Instant createdAt, Instant updatedAt) {
        public static PropertyResponse from(Property p) {
            return new PropertyResponse(p.getId(), p.getName(), p.getAddressLine(), p.getCity(), p.getPostalCode(),
                    p.getCountry(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record BuildingRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record BuildingResponse(UUID id, UUID propertyId, String name) {
        public static BuildingResponse from(Building b) {
            return new BuildingResponse(b.getId(), b.getPropertyId(), b.getName());
        }
    }

    public record FloorRequest(@Min(-10) @Max(200) int levelNumber, @Size(max = 50) String label) {
    }

    public record FloorResponse(UUID id, UUID propertyId, UUID buildingId, int levelNumber, String label) {
        public static FloorResponse from(Floor f) {
            return new FloorResponse(f.getId(), f.getPropertyId(), f.getBuildingId(), f.getLevelNumber(),
                    f.getLabel());
        }
    }

    public record UnitRequest(UUID buildingId, UUID floorId, @NotBlank @Size(max = 30) String unitNumber) {
    }

    public record UnitResponse(UUID id, UUID propertyId, UUID buildingId, UUID floorId, String unitNumber) {
        public static UnitResponse from(Unit u) {
            return new UnitResponse(u.getId(), u.getPropertyId(), u.getBuildingId(), u.getFloorId(),
                    u.getUnitNumber());
        }
    }

    public record MembershipRequest(@NotNull UUID userId, @NotNull MembershipRole role, UUID unitId) {
    }

    public record MembershipResponse(UUID id, UUID propertyId, UUID userId, MembershipRole role, UUID unitId,
            Instant createdAt) {
        public static MembershipResponse from(PropertyMembership m) {
            return new MembershipResponse(m.getId(), m.getPropertyId(), m.getUserId(), m.getRole(), m.getUnitId(),
                    m.getCreatedAt());
        }
    }

    public record AccessResponse(UUID propertyId, boolean systemAdmin, java.util.Set<MembershipRole> roles,
            java.util.Set<UUID> unitIds) {
    }
}
