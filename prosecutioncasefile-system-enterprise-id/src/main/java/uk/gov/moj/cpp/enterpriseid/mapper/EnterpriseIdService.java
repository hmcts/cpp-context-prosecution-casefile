package uk.gov.moj.cpp.enterpriseid.mapper;

import java.util.UUID;

/**
 * Interface for a Service to generate and store Enterprise IDs for a case.
 */
public interface EnterpriseIdService {

    /**
     * Service to create and persist an Enterprise ID.
     *
     * @param caseId - the ID of the case the enterprise ID is being created for.
     * @return the created enterprise ID.
     */
    String enterpriseIdForCase(final UUID caseId);
}
