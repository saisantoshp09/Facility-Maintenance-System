package com.facility.asset.domain;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord,UUID>,JpaSpecificationExecutor<ServiceRecord> {}
