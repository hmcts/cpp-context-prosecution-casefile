package uk.gov.moj.cpp.prosecution.casefile.event.listener.model;

import java.io.Serializable;
import java.util.UUID;

public class ErrorOffenceDetails implements Serializable {

    private UUID id;

    private String description;

    public ErrorOffenceDetails(UUID id, String description) {
        this.id = id;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}