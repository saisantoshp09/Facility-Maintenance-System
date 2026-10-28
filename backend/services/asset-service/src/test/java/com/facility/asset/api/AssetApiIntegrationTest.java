package com.facility.asset.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.facility.asset.application.Caller;
import com.facility.asset.application.DependencyUnavailableException;
import com.facility.asset.application.NotFoundException;
import com.facility.asset.application.PropertyAccessView;
import com.facility.asset.application.PropertyDirectory;
import com.facility.asset.domain.AssetRepository;
import com.facility.asset.domain.MaintenanceScheduleRepository;
import com.facility.asset.domain.MaintenanceTaskRepository;
import com.facility.asset.domain.ServiceRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(AssetApiIntegrationTest.TestConfig.class)
class AssetApiIntegrationTest {
    static class FakeDirectory implements PropertyDirectory {
        final Map<String,PropertyAccessView> grants=new HashMap<>(); volatile boolean down;
        void grant(UUID property,UUID user,String role){grants.put(property+":"+user,new PropertyAccessView(property,false,Set.of(role),Set.of()));}
        public PropertyAccessView accessOf(Caller caller,UUID property){if(down)throw new DependencyUnavailableException("unavailable",null);PropertyAccessView a=grants.get(property+":"+caller.userId());if(a==null)throw new NotFoundException("property not found");return a;}
    }
    @TestConfiguration static class TestConfig {
        @Bean @Primary FakeDirectory fakeDirectory(){return new FakeDirectory();}
        @Bean @Primary Clock fixedClock(){return Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"),ZoneOffset.UTC);}
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired FakeDirectory directory;
    @Autowired AssetRepository assets; @Autowired MaintenanceScheduleRepository schedules; @Autowired MaintenanceTaskRepository tasks; @Autowired ServiceRecordRepository records;
    final UUID property=UUID.randomUUID(), otherProperty=UUID.randomUUID(), manager=UUID.randomUUID(), otherManager=UUID.randomUUID(), tenant=UUID.randomUUID(), tech=UUID.randomUUID();
    final LocalDate today=LocalDate.parse("2026-03-15");
    @BeforeEach void setup(){records.deleteAll();tasks.deleteAll();schedules.deleteAll();assets.deleteAll();directory.grants.clear();directory.down=false;directory.grant(property,manager,"PROPERTY_MANAGER");directory.grant(otherProperty,otherManager,"PROPERTY_MANAGER");directory.grant(property,tenant,"TENANT");directory.grant(property,tech,"TECHNICIAN");}
    RequestPostProcessor as(UUID user){return jwt().jwt(t->t.subject(user.toString()));}
    JsonNode send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,UUID user,int status)throws Exception{String body=mvc.perform(request.with(as(user))).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();return body.isBlank()?null:json.readTree(body);}
    JsonNode createAsset(UUID user,int expected)throws Exception{return send(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content("{\"propertyId\":\""+property+"\",\"name\":\"Boiler\",\"category\":\"HVAC\",\"location\":\"Roof\"}"),user,expected);}
    JsonNode createSchedule(String assetId,String start,int interval,int expected)throws Exception{return send(post("/api/v1/assets/"+assetId+"/schedules").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Inspection\",\"description\":\"Inspect unit\",\"intervalDays\":"+interval+",\"startDate\":\""+start+"\"}"),manager,expected);}
    @Test void managerCreatesAssetScheduleAndServiceRecordCompletesAndReschedulesTask()throws Exception{
        JsonNode asset=createAsset(manager,201);String aid=asset.get("id").asText();JsonNode schedule=createSchedule(aid,today.toString(),30,201);String sid=schedule.get("id").asText();
        JsonNode taskPage=send(get("/api/v1/tasks?propertyId="+property+"&status=PENDING"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(1,taskPage.get("totalElements").asInt());JsonNode task=taskPage.get("content").get(0);org.junit.jupiter.api.Assertions.assertEquals(today.toString(),task.get("dueDate").asText());
        JsonNode record=send(post("/api/v1/assets/"+aid+"/service-records").contentType(MediaType.APPLICATION_JSON).content("{\"taskId\":\""+task.get("id").asText()+"\",\"performedOn\":\""+today+"\",\"summary\":\"Completed inspection\"}"),tech,201);
        org.junit.jupiter.api.Assertions.assertEquals(tech.toString(),record.get("performedBy").asText());
        send(get("/api/v1/tasks?propertyId="+property+"&status=COMPLETED"),manager,200);
        JsonNode updated=send(get("/api/v1/assets/"+aid+"/schedules"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(today.plusDays(30).toString(),updated.get(0).get("nextDueDate").asText());
        JsonNode pending=send(get("/api/v1/tasks?propertyId="+property+"&status=PENDING"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(1,pending.get("totalElements").asInt());org.junit.jupiter.api.Assertions.assertEquals(today.plusDays(30).toString(),pending.get("content").get(0).get("dueDate").asText());
    }
    @Test void overdueAndDueSoonFiltersUseTheInjectedUtcClock()throws Exception{
        JsonNode a=createAsset(manager,201);createSchedule(a.get("id").asText(),today.minusDays(1).toString(),30,201);createSchedule(a.get("id").asText(),today.plusDays(5).toString(),30,201);
        JsonNode overdue=send(get("/api/v1/tasks?propertyId="+property+"&overdue=true"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(1,overdue.get("totalElements").asInt());
        JsonNode notOverdue=send(get("/api/v1/tasks?propertyId="+property+"&overdue=false"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(1,notOverdue.get("totalElements").asInt());
        JsonNode due=send(get("/api/v1/tasks/due?propertyId="+property+"&withinDays=7"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(1,due.size());
    }
    @Test void hidesOtherPropertyAssetsAndDeniesTenantAccess()throws Exception{JsonNode a=createAsset(manager,201);send(get("/api/v1/assets/"+a.get("id").asText()),otherManager,404);send(get("/api/v1/assets/"+a.get("id").asText()),tenant,403);}
    @Test void technicianCanRecordServiceButCannotCreateAssets()throws Exception{createAsset(tech,403);JsonNode a=createAsset(manager,201);send(post("/api/v1/assets/"+a.get("id").asText()+"/service-records").contentType(MediaType.APPLICATION_JSON).content("{\"performedOn\":\""+today+"\",\"summary\":\"Repair\"}"),tech,201);}
    @Test void validatesRequestsAndDateRules()throws Exception{send(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content("{\"propertyId\":\""+property+"\",\"name\":\"\",\"category\":\"HVAC\",\"location\":\"Roof\"}"),manager,400);JsonNode a=createAsset(manager,201);createSchedule(a.get("id").asText(),today.toString(),0,400);send(post("/api/v1/assets/"+a.get("id").asText()+"/service-records").contentType(MediaType.APPLICATION_JSON).content("{\"performedOn\":\""+today.plusDays(1)+"\",\"summary\":\"Future\"}"),tech,400);}
    @Test void retiredAssetsAndCompletedTasksReturnConflicts()throws Exception{JsonNode a=createAsset(manager,201);JsonNode s=createSchedule(a.get("id").asText(),today.toString(),2,201);JsonNode t=send(get("/api/v1/tasks?propertyId="+property),manager,200).get("content").get(0);String url="/api/v1/assets/"+a.get("id").asText()+"/service-records";String body="{\"taskId\":\""+t.get("id").asText()+"\",\"performedOn\":\""+today+"\",\"summary\":\"done\"}";send(post(url).contentType(MediaType.APPLICATION_JSON).content(body),tech,201);send(post(url).contentType(MediaType.APPLICATION_JSON).content(body),tech,409);send(post("/api/v1/assets/"+a.get("id").asText()+"/retire"),manager,200);send(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"performedOn\":\""+today+"\",\"summary\":\"no\"}"),manager,409);send(post("/api/v1/assets/"+a.get("id").asText()+"/schedules").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"description\":\"x\",\"intervalDays\":1,\"startDate\":\""+today+"\"}"),manager,409);}
    @Test void failsClosedWhenPropertyDirectoryIsDown()throws Exception{directory.down=true;send(get("/api/v1/assets?propertyId="+property),manager,503);}
    @Test void paginationAndSortWhitelistAreEnforced()throws Exception{createAsset(manager,201);createAsset(manager,201);createAsset(manager,201);JsonNode page=send(get("/api/v1/assets?propertyId="+property+"&page=0&size=1&sort=name,asc"),manager,200);org.junit.jupiter.api.Assertions.assertEquals(1,page.get("content").size());org.junit.jupiter.api.Assertions.assertEquals(3,page.get("totalElements").asInt());send(get("/api/v1/assets?propertyId="+property+"&sort=propertyId,asc"),manager,400);}
}
