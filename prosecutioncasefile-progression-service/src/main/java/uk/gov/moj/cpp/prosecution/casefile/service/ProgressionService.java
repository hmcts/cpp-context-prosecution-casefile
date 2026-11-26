package uk.gov.moj.cpp.prosecution.casefile.service;

import uk.gov.justice.core.courts.CourtApplication;

import java.util.UUID;

import javax.json.JsonObject;

public interface ProgressionService {

    CourtApplication getApplicationOnly(final UUID applicationId);

    JsonObject getProsecutionCase(final UUID caseId);
}