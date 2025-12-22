package uk.gov.moj.cpp.prosecution.casefile.command.api.service;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.text.RandomStringGenerator;


public class IdGenerationService {

    public static final String SOURCE_TYPE = "OU_URN";
    public static final String TARGET_TYPE_CPI_MCC = "CASE_FILE_ID";
    public static final String TARGET_TYPE_SPI = "CASE-ID";
    public static final String TARGET_TYPE_SJP = "CASE_ID";

    private static final String CC_PREFIX = "C";
    private static final int CASE_REF_LENGTH = 9;
    private static final String INVALID_CONTEXT_SYSTEM_USER_ID = "Context system user Id is invalid";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public UUID generateCaseId(final String caseReference) {
        final UUID newCaseId = randomUUID();
        final Optional<SystemIdMapping> systemIdMapping = fetchSystemIdMappingFor(caseReference);
        if (systemIdMapping.isPresent()) {
            return systemIdMapping.map(SystemIdMapping::getTargetId).orElseThrow(()
                    -> new IllegalStateException(format("Invalid mapping found against case reference %s", caseReference)));
        } else if(addMappingForProsecutorCaseReference(caseReference, newCaseId).isSuccess()) {
            return newCaseId;
        } else {
            throw new IllegalStateException(format("Unable to generate case id for reference %s", caseReference));
        }
    }

    private AdditionResponse addMappingForProsecutorCaseReference(final String caseReference, final UUID caseId) {
        final SystemIdMap systemIdMap = new SystemIdMap(caseReference, SOURCE_TYPE, caseId, TARGET_TYPE_CPI_MCC);
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();

        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.add(systemIdMap, contextSystemUserId.get());
        }
        return new AdditionResponse(caseId, ResultCode.CONFLICT, Optional.of("Failed to add system id mapping"));
    }

    public Optional<SystemIdMapping> fetchSystemIdMappingFor(final String caseReference) {
        return systemIdMapperClient.findBy(systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException(INVALID_CONTEXT_SYSTEM_USER_ID)),
                caseReference, TARGET_TYPE_CPI_MCC,TARGET_TYPE_SPI,TARGET_TYPE_SJP);
    }

    public String generateCaseReference() {
        final RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('0', 'z')
                .filteredBy(LETTERS, DIGITS)
                .build();

        return CC_PREFIX + generator.generate(CASE_REF_LENGTH).toUpperCase();

    }
}

