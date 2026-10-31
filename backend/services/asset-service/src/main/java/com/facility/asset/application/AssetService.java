package com.facility.asset.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.facility.asset.api.dto.AssetDtos.AssetRequest;
import com.facility.asset.api.dto.AssetDtos.AssetUpdateRequest;
import com.facility.asset.api.dto.AssetDtos.ScheduleRequest;
import com.facility.asset.api.dto.AssetDtos.ServiceRecordRequest;
import com.facility.asset.domain.Asset;
import com.facility.asset.domain.AssetRepository;
import com.facility.asset.domain.AssetStatus;
import com.facility.asset.domain.MaintenanceSchedule;
import com.facility.asset.domain.MaintenanceScheduleRepository;
import com.facility.asset.domain.MaintenanceTask;
import com.facility.asset.domain.MaintenanceTaskRepository;
import com.facility.asset.domain.ServiceRecord;
import com.facility.asset.domain.ServiceRecordRepository;
import com.facility.asset.domain.TaskStatus;
import jakarta.persistence.criteria.Predicate;

@Service @Transactional
public class AssetService {
    private static final Set<String> ASSET_SORTS=Set.of("name","category","status","createdAt","updatedAt");
    private static final Set<String> DATE_SORTS=Set.of("dueDate","performedOn","createdAt");
    private final AssetRepository assets; private final MaintenanceScheduleRepository schedules; private final MaintenanceTaskRepository tasks;
    private final ServiceRecordRepository records; private final PropertyDirectory properties; private final Clock clock;
    public AssetService(AssetRepository assets,MaintenanceScheduleRepository schedules,MaintenanceTaskRepository tasks,
            ServiceRecordRepository records,PropertyDirectory properties,Clock clock){this.assets=assets;this.schedules=schedules;this.tasks=tasks;this.records=records;this.properties=properties;this.clock=clock;}

    public Asset create(Caller caller,AssetRequest req){PropertyAccessView access=properties.accessOf(caller,req.propertyId()); requireManager(access);
        Instant now=clock.instant(); return assets.save(new Asset(req.propertyId(),req.unitId(),req.name().trim(),req.category().trim(),clean(req.serialNumber()),req.location().trim(),req.installedOn(),now));}
    @Transactional(readOnly=true) public Asset get(Caller caller,UUID id){return load(caller,id,false);}
    @Transactional(readOnly=true) public Page<Asset> list(Caller caller,UUID propertyId,AssetStatus status,String category,Pageable pageable){
        PropertyAccessView access=properties.accessOf(caller,propertyId); requireReader(access);
        Pageable safe=restrict(pageable,ASSET_SORTS,Sort.by(Sort.Direction.DESC,"createdAt"));
        Specification<Asset> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();p.add(cb.equal(root.get("propertyId"),propertyId));if(status!=null)p.add(cb.equal(root.get("status"),status));if(category!=null&&!category.isBlank())p.add(cb.equal(cb.lower(root.get("category")),category.toLowerCase()));return cb.and(p.toArray(new Predicate[0]));};
        return assets.findAll(spec,safe);
    }
    public Asset update(Caller caller,UUID id,AssetUpdateRequest req){Asset a=load(caller,id,true);try{a.update(req.unitId(),req.name().trim(),req.category().trim(),clean(req.serialNumber()),req.location().trim(),req.installedOn(),clock.instant());}catch(IllegalStateException ex){throw new ConflictException(ex.getMessage());}return a;}
    public Asset retire(Caller caller,UUID id){Asset a=load(caller,id,true);a.retire(clock.instant());return a;}

    public MaintenanceSchedule createSchedule(Caller caller,UUID assetId,ScheduleRequest req){Asset asset=load(caller,assetId,true);if(asset.getStatus()==AssetStatus.RETIRED)throw new ConflictException("Retired assets cannot have schedules");
        MaintenanceSchedule schedule=schedules.save(new MaintenanceSchedule(assetId,req.title().trim(),req.description().trim(),req.intervalDays(),req.startDate()));
        tasks.save(new MaintenanceTask(schedule.getId(),assetId,asset.getPropertyId(),req.startDate()));return schedule;}
    @Transactional(readOnly=true) public List<MaintenanceSchedule> listSchedules(Caller caller,UUID assetId){load(caller,assetId,false);return schedules.findByAssetId(assetId);}
    public MaintenanceSchedule deactivateSchedule(Caller caller,UUID scheduleId){MaintenanceSchedule initial=schedules.findById(scheduleId).orElseThrow(()->new NotFoundException("Schedule not found"));load(caller,initial.getAssetId(),true);MaintenanceSchedule s=schedules.findLockedById(scheduleId).orElseThrow(()->new NotFoundException("Schedule not found"));s.deactivate();tasks.findByScheduleIdAndStatus(s.getId(),TaskStatus.PENDING).forEach(t->t.cancel());return s;}

    public ServiceRecord recordService(Caller caller,UUID assetId,ServiceRecordRequest req){Asset asset=load(caller,assetId,false);PropertyAccessView access=properties.accessOf(caller,asset.getPropertyId());if(!access.canManage()&&!access.isTechnician())throw new AccessDeniedException("Only managers and technicians may record service");
        if(asset.getStatus()==AssetStatus.RETIRED)throw new ConflictException("Retired assets cannot receive service records");
        if(req.performedOn().isAfter(LocalDate.now(clock)))throw new InvalidRequestException("performedOn cannot be in the future");
        if(req.cost()!=null&&req.cost().signum()<0)throw new InvalidRequestException("cost must be non-negative");
        if(req.taskId()!=null) completeTask(asset,req.taskId(),req.performedOn());
        return records.save(new ServiceRecord(assetId,req.taskId(),caller.userId(),req.performedOn(),req.summary().trim(),req.cost(),clock.instant()));}
    @Transactional(readOnly=true) public Page<ServiceRecord> listRecords(Caller caller,UUID assetId,Pageable pageable){load(caller,assetId,false);return records.findAll((root,q,cb)->cb.equal(root.get("assetId"),assetId),restrict(pageable,DATE_SORTS,Sort.by(Sort.Direction.DESC,"performedOn")));}

    @Transactional(readOnly=true) public Page<MaintenanceTask> listTasks(Caller caller,UUID propertyId,TaskStatus status,Boolean overdue,LocalDate dueBefore,Pageable pageable){PropertyAccessView access=properties.accessOf(caller,propertyId);requireReader(access);LocalDate today=LocalDate.now(clock);
        Specification<MaintenanceTask> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();p.add(cb.equal(root.get("propertyId"),propertyId));if(status!=null)p.add(cb.equal(root.get("status"),status));if(dueBefore!=null)p.add(cb.lessThan(root.get("dueDate"),dueBefore));if(overdue!=null){Predicate late=cb.and(cb.equal(root.get("status"),TaskStatus.PENDING),cb.lessThan(root.get("dueDate"),today));p.add(overdue?late:cb.not(late));}return cb.and(p.toArray(new Predicate[0]));};
        return tasks.findAll(spec,restrict(pageable,Set.of("dueDate"),Sort.by("dueDate")));}
    @Transactional(readOnly=true) public List<MaintenanceTask> dueSoon(Caller caller,UUID propertyId,int withinDays){if(withinDays<0||withinDays>365)throw new InvalidRequestException("withinDays must be between 0 and 365");requireReader(properties.accessOf(caller,propertyId));LocalDate today=LocalDate.now(clock);return tasks.findByPropertyIdAndStatusAndDueDateBetweenOrderByDueDateAsc(propertyId,TaskStatus.PENDING,today,today.plusDays(withinDays));}

    private void completeTask(Asset asset,UUID taskId,LocalDate completedOn){MaintenanceTask task=tasks.findById(taskId).orElseThrow(()->new NotFoundException("Task not found"));if(!task.getAssetId().equals(asset.getId()))throw new ConflictException("Task does not belong to this asset");if(task.getStatus()!=TaskStatus.PENDING)throw new ConflictException("Task is not pending");
        MaintenanceSchedule schedule=schedules.findLockedById(task.getScheduleId()).orElseThrow(()->new NotFoundException("Schedule not found"));task=tasks.findLockedById(taskId).orElseThrow(()->new NotFoundException("Task not found"));if(!task.getAssetId().equals(asset.getId()))throw new ConflictException("Task does not belong to this asset");if(task.getStatus()!=TaskStatus.PENDING)throw new ConflictException("Task is not pending");if(!schedule.isActive())throw new ConflictException("Schedule is inactive");task.complete(clock.instant());LocalDate next=schedule.completeOn(completedOn);
        if(tasks.findFirstByScheduleIdAndStatus(schedule.getId(),TaskStatus.PENDING).isEmpty())tasks.save(new MaintenanceTask(schedule.getId(),asset.getId(),asset.getPropertyId(),next));}
    private Asset load(Caller caller,UUID id,boolean manager){Asset a=assets.findById(id).orElseThrow(()->new NotFoundException("Asset not found"));PropertyAccessView access=properties.accessOf(caller,a.getPropertyId());if(manager)requireManager(access);else requireReader(access);return a;}
    private static void requireManager(PropertyAccessView a){if(!a.canManage())throw new AccessDeniedException("Property manager access is required");}
    private static void requireReader(PropertyAccessView a){if(!a.canManage()&&!a.isTechnician())throw new AccessDeniedException("Asset access is denied");}
    private static String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private static Pageable restrict(Pageable p,Set<String> allowed,Sort fallback){if(p.getSort().isUnsorted())return PageRequest.of(p.getPageNumber(),p.getPageSize(),fallback);p.getSort().forEach(o->{if(!allowed.contains(o.getProperty()))throw new InvalidRequestException("Unsupported sort field: "+o.getProperty());});return p;}
}
