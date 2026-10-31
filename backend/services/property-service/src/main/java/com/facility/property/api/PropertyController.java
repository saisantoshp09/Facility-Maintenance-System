package com.facility.property.api;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.facility.property.api.dto.PageResponse;
import com.facility.property.api.dto.PropertyDtos.AccessResponse;
import com.facility.property.api.dto.PropertyDtos.BuildingRequest;
import com.facility.property.api.dto.PropertyDtos.BuildingResponse;
import com.facility.property.api.dto.PropertyDtos.FloorRequest;
import com.facility.property.api.dto.PropertyDtos.FloorResponse;
import com.facility.property.api.dto.PropertyDtos.MembershipRequest;
import com.facility.property.api.dto.PropertyDtos.MembershipResponse;
import com.facility.property.api.dto.PropertyDtos.PropertyRequest;
import com.facility.property.api.dto.PropertyDtos.PropertyResponse;
import com.facility.property.api.dto.PropertyDtos.UnitRequest;
import com.facility.property.api.dto.PropertyDtos.UnitResponse;
import com.facility.property.application.Caller;
import com.facility.property.application.PropertyAccessService.PropertyAccess;
import com.facility.property.application.PropertyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {
    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<PropertyResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PropertyResponse.from(service.create(Caller.from(jwt), request)));
    }

    @GetMapping
    PageResponse<PropertyResponse> list(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return PageResponse.from(service.list(Caller.from(jwt), pageable), PropertyResponse::from);
    }

    @GetMapping("/{propertyId}")
    PropertyResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId) {
        return PropertyResponse.from(service.get(Caller.from(jwt), propertyId));
    }

    @PutMapping("/{propertyId}")
    PropertyResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @Valid @RequestBody PropertyRequest request) {
        return PropertyResponse.from(service.update(Caller.from(jwt), propertyId, request));
    }

    /** Authoritative property-scoped access answer for other services. */
    @GetMapping("/{propertyId}/access")
    AccessResponse access(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId) {
        PropertyAccess granted = service.accessOf(Caller.from(jwt), propertyId);
        return new AccessResponse(granted.propertyId(), granted.systemAdmin(), granted.roles(), granted.unitIds());
    }

    /** Lets a property manager/admin check which roles another user holds (used to validate assignees). */
    @GetMapping("/{propertyId}/members/{userId}/access")
    AccessResponse memberAccess(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @PathVariable UUID userId) {
        PropertyAccess granted = service.memberAccessOf(Caller.from(jwt), propertyId, userId);
        return new AccessResponse(granted.propertyId(), false, granted.roles(), granted.unitIds());
    }

    @PostMapping("/{propertyId}/buildings")
    ResponseEntity<BuildingResponse> createBuilding(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID propertyId, @Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BuildingResponse.from(service.createBuilding(Caller.from(jwt), propertyId, request)));
    }

    @GetMapping("/{propertyId}/buildings")
    PageResponse<BuildingResponse> listBuildings(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID propertyId, Pageable pageable) {
        return PageResponse.from(service.listBuildings(Caller.from(jwt), propertyId, pageable),
                BuildingResponse::from);
    }

    @PostMapping("/{propertyId}/buildings/{buildingId}/floors")
    ResponseEntity<FloorResponse> createFloor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @PathVariable UUID buildingId, @Valid @RequestBody FloorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FloorResponse.from(service.createFloor(Caller.from(jwt), propertyId, buildingId, request)));
    }

    @GetMapping("/{propertyId}/buildings/{buildingId}/floors")
    java.util.List<FloorResponse> listFloors(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @PathVariable UUID buildingId) {
        return service.listFloors(Caller.from(jwt), propertyId, buildingId).stream()
                .map(FloorResponse::from).toList();
    }

    @PostMapping("/{propertyId}/units")
    ResponseEntity<UnitResponse> createUnit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @Valid @RequestBody UnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UnitResponse.from(service.createUnit(Caller.from(jwt), propertyId, request)));
    }

    @GetMapping("/{propertyId}/units")
    PageResponse<UnitResponse> listUnits(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            Pageable pageable) {
        return PageResponse.from(service.listUnits(Caller.from(jwt), propertyId, pageable), UnitResponse::from);
    }

    @GetMapping("/{propertyId}/units/{unitId}")
    UnitResponse getUnit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @PathVariable UUID unitId) {
        return UnitResponse.from(service.getUnit(Caller.from(jwt), propertyId, unitId));
    }

    @PostMapping("/{propertyId}/memberships")
    ResponseEntity<MembershipResponse> addMembership(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID propertyId, @Valid @RequestBody MembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MembershipResponse.from(service.addMembership(Caller.from(jwt), propertyId, request)));
    }

    @GetMapping("/{propertyId}/memberships")
    PageResponse<MembershipResponse> listMemberships(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID propertyId, Pageable pageable) {
        return PageResponse.from(service.listMemberships(Caller.from(jwt), propertyId, pageable),
                MembershipResponse::from);
    }

    @DeleteMapping("/{propertyId}/memberships/{membershipId}")
    ResponseEntity<Void> removeMembership(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID propertyId,
            @PathVariable UUID membershipId) {
        service.removeMembership(Caller.from(jwt), propertyId, membershipId);
        return ResponseEntity.noContent().build();
    }
}
