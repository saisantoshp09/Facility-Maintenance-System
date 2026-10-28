package com.facility.property.application;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.facility.property.api.dto.PropertyDtos.BuildingRequest;
import com.facility.property.api.dto.PropertyDtos.FloorRequest;
import com.facility.property.api.dto.PropertyDtos.MembershipRequest;
import com.facility.property.api.dto.PropertyDtos.PropertyRequest;
import com.facility.property.api.dto.PropertyDtos.UnitRequest;
import com.facility.property.application.PropertyAccessService.PropertyAccess;
import com.facility.property.domain.Building;
import com.facility.property.domain.BuildingRepository;
import com.facility.property.domain.Floor;
import com.facility.property.domain.FloorRepository;
import com.facility.property.domain.MembershipRole;
import com.facility.property.domain.Property;
import com.facility.property.domain.PropertyMembership;
import com.facility.property.domain.PropertyMembershipRepository;
import com.facility.property.domain.PropertyRepository;
import com.facility.property.domain.Unit;
import com.facility.property.domain.UnitRepository;

@Service
@Transactional
public class PropertyService {
    private static final Set<String> PROPERTY_SORTS = Set.of("name", "city", "createdAt");
    private static final Set<String> BUILDING_SORTS = Set.of("name", "createdAt");
    private static final Set<String> UNIT_SORTS = Set.of("unitNumber", "createdAt");
    private static final Set<String> MEMBERSHIP_SORTS = Set.of("createdAt", "role");

    private final PropertyRepository properties;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final UnitRepository units;
    private final PropertyMembershipRepository memberships;
    private final PropertyAccessService access;
    private final Clock clock;

    public PropertyService(PropertyRepository properties, BuildingRepository buildings, FloorRepository floors,
            UnitRepository units, PropertyMembershipRepository memberships, PropertyAccessService access,
            Clock clock) {
        this.properties = properties;
        this.buildings = buildings;
        this.floors = floors;
        this.units = units;
        this.memberships = memberships;
        this.access = access;
        this.clock = clock;
    }

    public Property create(Caller caller, PropertyRequest request) {
        if (!caller.systemAdmin()) {
            throw new AccessDeniedException("Only system administrators can create properties");
        }
        return properties.save(new Property(request.name().trim(), request.addressLine().trim(),
                request.city().trim(), request.postalCode().trim(), request.country(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public Page<Property> list(Caller caller, Pageable pageable) {
        Pageable safe = SortGuard.restrict(pageable, PROPERTY_SORTS, Sort.by("name"));
        return caller.systemAdmin() ? properties.findAll(safe) : properties.findAccessibleTo(caller.userId(), safe);
    }

    @Transactional(readOnly = true)
    public Property get(Caller caller, UUID propertyId) {
        access.requireAccess(caller, propertyId);
        return properties.findById(propertyId).orElseThrow(() -> new NotFoundException("Property not found"));
    }

    public Property update(Caller caller, UUID propertyId, PropertyRequest request) {
        access.requireManager(caller, propertyId);
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));
        property.update(request.name().trim(), request.addressLine().trim(), request.city().trim(),
                request.postalCode().trim(), request.country(), clock.instant());
        return property;
    }

    @Transactional(readOnly = true)
    public PropertyAccess accessOf(Caller caller, UUID propertyId) {
        return access.requireAccess(caller, propertyId);
    }

    @Transactional(readOnly = true)
    public PropertyAccess memberAccessOf(Caller caller, UUID propertyId, UUID userId) {
        return access.requireMemberAccess(caller, propertyId, userId);
    }

    public Building createBuilding(Caller caller, UUID propertyId, BuildingRequest request) {
        access.requireManager(caller, propertyId);
        String name = request.name().trim();
        if (buildings.existsByPropertyIdAndName(propertyId, name)) {
            throw new ConflictException("A building with this name already exists");
        }
        return saveOrConflict(() -> buildings.saveAndFlush(new Building(propertyId, name, clock.instant())),
                "A building with this name already exists");
    }

    @Transactional(readOnly = true)
    public Page<Building> listBuildings(Caller caller, UUID propertyId, Pageable pageable) {
        access.requireAccess(caller, propertyId);
        return buildings.findByPropertyId(propertyId,
                SortGuard.restrict(pageable, BUILDING_SORTS, Sort.by("name")));
    }

    public Floor createFloor(Caller caller, UUID propertyId, UUID buildingId, FloorRequest request) {
        access.requireManager(caller, propertyId);
        requireBuilding(propertyId, buildingId);
        if (floors.existsByBuildingIdAndLevelNumber(buildingId, request.levelNumber())) {
            throw new ConflictException("This floor level already exists in the building");
        }
        return saveOrConflict(() -> floors.saveAndFlush(new Floor(propertyId, buildingId, request.levelNumber(),
                request.label(), clock.instant())), "This floor level already exists in the building");
    }

    @Transactional(readOnly = true)
    public List<Floor> listFloors(Caller caller, UUID propertyId, UUID buildingId) {
        access.requireAccess(caller, propertyId);
        requireBuilding(propertyId, buildingId);
        return floors.findByBuildingIdAndPropertyIdOrderByLevelNumber(buildingId, propertyId);
    }

    public Unit createUnit(Caller caller, UUID propertyId, UnitRequest request) {
        access.requireManager(caller, propertyId);
        if (request.buildingId() != null) {
            requireBuilding(propertyId, request.buildingId());
        }
        if (request.floorId() != null) {
            Floor floor = floors.findByIdAndPropertyId(request.floorId(), propertyId)
                    .orElseThrow(() -> new InvalidRequestException("Floor does not belong to this property"));
            if (request.buildingId() != null && !floor.getBuildingId().equals(request.buildingId())) {
                throw new InvalidRequestException("Floor does not belong to the given building");
            }
        }
        String number = request.unitNumber().trim();
        if (units.existsByPropertyIdAndUnitNumber(propertyId, number)) {
            throw new ConflictException("A unit with this number already exists");
        }
        return saveOrConflict(() -> units.saveAndFlush(new Unit(propertyId, request.buildingId(),
                request.floorId(), number, clock.instant())), "A unit with this number already exists");
    }

    @Transactional(readOnly = true)
    public Page<Unit> listUnits(Caller caller, UUID propertyId, Pageable pageable) {
        PropertyAccess granted = access.requireAccess(caller, propertyId);
        Pageable safe = SortGuard.restrict(pageable, UNIT_SORTS, Sort.by("unitNumber"));
        if (granted.restrictedToUnits()) {
            if (granted.unitIds().isEmpty()) {
                return Page.empty(safe);
            }
            return units.findByPropertyIdAndIdIn(propertyId, granted.unitIds(), safe);
        }
        return units.findByPropertyId(propertyId, safe);
    }

    @Transactional(readOnly = true)
    public Unit getUnit(Caller caller, UUID propertyId, UUID unitId) {
        PropertyAccess granted = access.requireAccess(caller, propertyId);
        Unit unit = units.findByIdAndPropertyId(unitId, propertyId)
                .orElseThrow(() -> new NotFoundException("Unit not found"));
        if (granted.restrictedToUnits() && !granted.unitIds().contains(unitId)) {
            throw new NotFoundException("Unit not found");
        }
        return unit;
    }

    public PropertyMembership addMembership(Caller caller, UUID propertyId, MembershipRequest request) {
        access.requireManager(caller, propertyId);
        if (request.role() == MembershipRole.TENANT) {
            if (request.unitId() == null) {
                throw new InvalidRequestException("A tenant membership requires a unitId");
            }
            units.findByIdAndPropertyId(request.unitId(), propertyId)
                    .orElseThrow(() -> new InvalidRequestException("Unit does not belong to this property"));
        } else if (request.unitId() != null) {
            throw new InvalidRequestException("Only tenant memberships can reference a unit");
        }
        boolean exists = request.unitId() == null
                ? memberships.existsByPropertyIdAndUserIdAndRoleAndUnitIdIsNull(propertyId, request.userId(),
                        request.role())
                : memberships.existsByPropertyIdAndUserIdAndRoleAndUnitId(propertyId, request.userId(),
                        request.role(), request.unitId());
        if (exists) {
            throw new ConflictException("This membership already exists");
        }
        return saveOrConflict(() -> memberships.saveAndFlush(new PropertyMembership(propertyId, request.userId(),
                request.role(), request.unitId(), clock.instant())), "This membership already exists");
    }

    @Transactional(readOnly = true)
    public Page<PropertyMembership> listMemberships(Caller caller, UUID propertyId, Pageable pageable) {
        access.requireManager(caller, propertyId);
        return memberships.findByPropertyId(propertyId,
                SortGuard.restrict(pageable, MEMBERSHIP_SORTS, Sort.by("createdAt")));
    }

    public void removeMembership(Caller caller, UUID propertyId, UUID membershipId) {
        access.requireManager(caller, propertyId);
        PropertyMembership membership = memberships.findByIdAndPropertyId(membershipId, propertyId)
                .orElseThrow(() -> new NotFoundException("Membership not found"));
        memberships.delete(membership);
    }

    private void requireBuilding(UUID propertyId, UUID buildingId) {
        buildings.findByIdAndPropertyId(buildingId, propertyId)
                .orElseThrow(() -> new NotFoundException("Building not found"));
    }

    private <T> T saveOrConflict(java.util.function.Supplier<T> save, String message) {
        try {
            return save.get();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(message);
        }
    }
}
