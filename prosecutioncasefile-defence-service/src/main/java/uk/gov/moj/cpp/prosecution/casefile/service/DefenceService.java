package uk.gov.moj.cpp.prosecution.casefile.service;

import java.util.UUID;

import jakarta.json.JsonObject;

public interface DefenceService {

    JsonObject getAssociatedOrganisation(final UUID defendantId);
}