package uk.gov.moj.cpp.enterpriseid.mapper;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.enterpriseid.generator.EnterpriseIdGenerator;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class EnterpriseIdServiceImpl implements EnterpriseIdService {

    private static final String ENTERPRISE_ID_SOURCE_TYPE = "enterpriseId";
    private static final String CASE_ID_TARGET_TYPE = "caseId";
    private static final int MAX_ALLOCATION_ATTEMPTS = 10;

    @Inject
    private Logger logger;

    @Inject
    private SystemIdMapperClient idMapperClient;

    @Inject
    private EnterpriseIdGenerator idGenerator;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Override
    public String enterpriseIdForCase(final UUID caseId) {

        final UUID userId = systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot register enterprise ID for case '" + caseId + "': context system user ID is not available"));

        for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
            final String enterpriseId = idGenerator.enterpriseId();

            if (idMapperClient.add(new SystemIdMap(enterpriseId, ENTERPRISE_ID_SOURCE_TYPE, caseId, CASE_ID_TARGET_TYPE), userId).code() != ResultCode.CONFLICT) {
                return enterpriseId;
            }

            logger.info("Conflict registering enterprise id: " + enterpriseId + " for case id: " + caseId);
        }

        throw new EnterpriseIdAllocationException(
                "Failed to allocate a unique enterprise ID for case '" + caseId + "' after " + MAX_ALLOCATION_ATTEMPTS + " attempts");
    }
}
