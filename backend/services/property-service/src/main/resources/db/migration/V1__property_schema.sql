CREATE TABLE properties (
    id BINARY(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_properties PRIMARY KEY (id)
);

CREATE INDEX idx_properties_name ON properties (name);

CREATE TABLE buildings (
    id BINARY(16) NOT NULL,
    property_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_buildings PRIMARY KEY (id),
    CONSTRAINT uk_buildings_name UNIQUE (property_id, name),
    CONSTRAINT fk_buildings_property FOREIGN KEY (property_id) REFERENCES properties (id)
);

CREATE TABLE floors (
    id BINARY(16) NOT NULL,
    property_id BINARY(16) NOT NULL,
    building_id BINARY(16) NOT NULL,
    level_number INT NOT NULL,
    label VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_floors PRIMARY KEY (id),
    CONSTRAINT uk_floors_level UNIQUE (building_id, level_number),
    CONSTRAINT fk_floors_building FOREIGN KEY (building_id) REFERENCES buildings (id),
    CONSTRAINT fk_floors_property FOREIGN KEY (property_id) REFERENCES properties (id)
);

CREATE TABLE units (
    id BINARY(16) NOT NULL,
    property_id BINARY(16) NOT NULL,
    building_id BINARY(16) NULL,
    floor_id BINARY(16) NULL,
    unit_number VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_units PRIMARY KEY (id),
    CONSTRAINT uk_units_number UNIQUE (property_id, unit_number),
    CONSTRAINT fk_units_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_units_building FOREIGN KEY (building_id) REFERENCES buildings (id),
    CONSTRAINT fk_units_floor FOREIGN KEY (floor_id) REFERENCES floors (id)
);

-- user_id is a reference to the identity service; there is intentionally no foreign key.
CREATE TABLE property_memberships (
    id BINARY(16) NOT NULL,
    property_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role VARCHAR(30) NOT NULL,
    unit_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_property_memberships PRIMARY KEY (id),
    CONSTRAINT uk_memberships UNIQUE (property_id, user_id, role, unit_id),
    CONSTRAINT fk_memberships_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_memberships_unit FOREIGN KEY (unit_id) REFERENCES units (id)
);

CREATE INDEX idx_memberships_user ON property_memberships (user_id);
