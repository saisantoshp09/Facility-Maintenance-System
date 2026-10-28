package com.facility.property.application;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class SortGuard {
    private SortGuard() {
    }

    /** Rejects unknown sort fields (400) and falls back to a default order when none is given. */
    static Pageable restrict(Pageable pageable, Set<String> allowed, Sort defaultSort) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException("Unsupported sort field: " + order.getProperty());
            }
        });
        return pageable;
    }
}
