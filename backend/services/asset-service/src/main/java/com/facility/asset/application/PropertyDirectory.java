package com.facility.asset.application;

import java.util.UUID;

public interface PropertyDirectory {
    PropertyAccessView accessOf(Caller caller, UUID propertyId);
}
