package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import javax.inject.Inject;

public class ProsecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter implements ParameterisedConverter<CaseDetails, ProsecutionCaseIdentifier, Metadata> {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private ProsecutionCaseFileAddressToCourtsAddressConverter prosecutionCaseFileAddressToCourtsAddressConverter;

    @Override
    public ProsecutionCaseIdentifier convert(final CaseDetails source, final Metadata metadata) {
        final Prosecutor prosecutor = source.getProsecutor();
        final String ouCode = prosecutor.getProsecutingAuthority();
        ProsecutorsReferenceData prosecutorsReferenceData = prosecutor.getReferenceData();
        if (isNull(prosecutorsReferenceData)) {
            if (nonNull(prosecutor.getProsecutionAuthorityId())) {
                prosecutorsReferenceData = referenceDataQueryService.getProsecutorById(prosecutor.getProsecutionAuthorityId());
            } else {
                prosecutorsReferenceData = referenceDataQueryService.getProsecutorsByOuCode(metadata, ouCode);
            }
        }
        Address address = null;
        if (nonNull(prosecutorsReferenceData.getAddress())) {
            address = prosecutionCaseFileAddressToCourtsAddressConverter.convert(prosecutorsReferenceData.getAddress());
        }

        return prosecutionCaseIdentifier()
                .withCaseURN(source.getProsecutorCaseReference())
                .withProsecutionAuthorityId(prosecutorsReferenceData.getId())
                .withProsecutionAuthorityCode(prosecutorsReferenceData.getShortName())
                .withAddress(address)
                .withProsecutionAuthorityName(prosecutorsReferenceData.getFullName())
                .withMajorCreditorCode(prosecutorsReferenceData.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutorsReferenceData.getOucode())
                .withContact(ofNullable(prosecutorsReferenceData.getContactEmailAddress()).map(email -> contactNumber().withPrimaryEmail(email).build()).orElse(null))
                .withProsecutorCategory(prosecutorsReferenceData.getProsecutorCategory())
                .build();
    }

}
