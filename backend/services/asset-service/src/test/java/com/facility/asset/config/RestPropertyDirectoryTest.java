package com.facility.asset.config;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.facility.asset.application.Caller;
import com.facility.asset.application.DependencyUnavailableException;
import com.facility.asset.application.NotFoundException;
import com.facility.asset.application.PropertyAccessView;
import com.sun.net.httpserver.HttpServer;
class RestPropertyDirectoryTest {
    HttpServer server; final AtomicInteger calls=new AtomicInteger(); final UUID property=UUID.randomUUID();
    final Caller caller=new Caller(UUID.randomUUID(),"sample-token");
    @AfterEach void stop(){if(server!=null)server.stop(0);}
    RestPropertyDirectory start(int status,String body)throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",e->{calls.incrementAndGet();byte[] b=body.getBytes();e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(status,b.length);e.getResponseBody().write(b);e.close();});server.start();return new RestPropertyDirectory("http://127.0.0.1:"+server.getAddress().getPort(),Duration.ofSeconds(1),Duration.ofSeconds(1),2);}
    @Test void parsesResponse()throws Exception{PropertyPropertyDirectoryCheck(start(200,"{\"propertyId\":\""+property+"\",\"systemAdmin\":false,\"roles\":[\"TECHNICIAN\"],\"unitIds\":[]}"));}
    private void PropertyPropertyDirectoryCheck(RestPropertyDirectory d){PropertyAccessView v=d.accessOf(caller,property);assertTrue(v.isTechnician());assertEquals(1,calls.get());}
    @Test void maps404WithoutRetry()throws Exception{RestPropertyDirectory d=start(404,"{}");assertThrows(NotFoundException.class,()->d.accessOf(caller,property));assertEquals(1,calls.get());}
    @Test void maps403()throws Exception{RestPropertyDirectory d=start(403,"{}");assertThrows(AccessDeniedException.class,()->d.accessOf(caller,property));}
    @Test void retries500Boundedly()throws Exception{RestPropertyDirectory d=start(500,"{}");assertThrows(DependencyUnavailableException.class,()->d.accessOf(caller,property));assertEquals(2,calls.get());}
    @Test void failsClosedWhenUnreachable(){RestPropertyDirectory d=new RestPropertyDirectory("http://127.0.0.1:1",Duration.ofMillis(100),Duration.ofMillis(100),2);assertThrows(DependencyUnavailableException.class,()->d.accessOf(caller,property));}
}
