package com.ecotrack.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "REPORT_SERVICE_URL=http://report-service:8081")
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routes;

    @Test
    void reportServiceRouteIsDefinedAndTargetsTheConfiguredUri() {
        var all = routes.getRoutes().collectList().block();
        assertTrue(all.stream().anyMatch(r -> r.getId().equals("report-service")));
        Route route = all.stream().filter(r -> r.getId().equals("report-service")).findFirst().orElseThrow();
        assertEquals("http://report-service:8081", route.getUri().toString());
    }

    @Test
    void sseRouteIsDefinedWithoutResponseTimeout() {
        var all = routes.getRoutes().collectList().block();
        Route route = all.stream().filter(r -> r.getId().equals("report-events")).findFirst().orElseThrow();
        assertEquals("-1", String.valueOf(route.getMetadata().get("response-timeout")));
    }
}
