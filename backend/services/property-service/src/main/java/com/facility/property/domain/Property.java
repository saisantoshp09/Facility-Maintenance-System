package com.facility.property.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "properties")
public class Property {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    private String name;
    @Column(name = "address_line")
    private String addressLine;
    private String city;
    @Column(name = "postal_code")
    private String postalCode;
    private String country;
    private boolean active;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private long version;

    protected Property() {
    }

    public Property(String name, String addressLine, String city, String postalCode, String country, Instant now) {
        this.id = UUID.randomUUID();
        this.active = true;
        this.createdAt = now;
        update(name, addressLine, city, postalCode, country, now);
    }

    public void update(String name, String addressLine, String city, String postalCode, String country, Instant now) {
        this.name = name;
        this.addressLine = addressLine;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
