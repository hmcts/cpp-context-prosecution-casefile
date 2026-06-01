package uk.gov.moj.cpp.enterpriseid.mapper;

import static java.lang.String.format;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.enterpriseid.generator.EnterpriseIdGenerator;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
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
        final String enterpriseId = idGenerator.enterpriseId();

        final AdditionResponse response = idMapperClient.add(new SystemIdMap(
                enterpriseId,
                ENTERPRISE_ID_SOURCE_TYPE,
                caseId,
                CASE_ID_TARGET_TYPE), userId);

        switch (response.code()) {
            case CONFLICT -> {
                logger.info(format("Conflict as result for enterprise id: %s generated for case id: %s",
                        enterpriseId, caseId));
                return enterpriseIdForCase(caseId);
            }
            default -> { }
        }

        return enterpriseId;
    }
}
