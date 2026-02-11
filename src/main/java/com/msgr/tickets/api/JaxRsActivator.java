package com.msgr.tickets.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/api")
public class JaxRsActivator extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                AuthResource.class,
                AuthFilter.class,
                TicketResource.class,
                EventResource.class,
                PersonResource.class,
                VenueResource.class,
                TicketSpecialResource.class
        );
    }
}
