package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;


import javax.inject.Inject;

public class ProsecutionCaseFileProsecutorToCourtApplicationPartyConverter implements DualParameterisedConverter<Prosecutor, CourtApplicationParty, ParamsVO, Metadata> {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private ProsecutionCaseFileAddressToCourtsAddressConverter prosecutionCaseFileAddressToCourtsAddressConverter;

    @Override
    public CourtApplicationParty convert(final Prosecutor source, final ParamsVO param, final Metadata metadata) {
        final String ouCode = source.getProsecutingAuthority();
        ProsecutorsReferenceData prosecutorsReferenceData = source.getReferenceData();
        if (isNull(prosecutorsReferenceData)) {
            if (nonNull(source.getProsecutionAuthorityId())) {
                prosecutorsReferenceData = referenceDataQueryService.getProsecutorById(source.getProsecutionAuthorityId());
            } else {
                prosecutorsReferenceData = referenceDataQueryService.getProsecutorsByOuCode(metadata, ouCode);
            }
        }
        return courtApplicationParty()
                .withId(prosecutorsReferenceData.getId())
                .withProsecutingAuthority(buildProsecutingAuthority(prosecutorsReferenceData))
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
    }

    private ProsecutingAuthority buildProsecutingAuthority(final ProsecutorsReferenceData source) {
        Address address = null;
        if (nonNull(source.getAddress())) {
            address = prosecutionCaseFileAddressToCourtsAddressConverter.convert(source.getAddress());
        }

        return prosecutingAuthority()
                .withProsecutionAuthorityId(source.getId())
                .withProsecutionAuthorityCode(source.getShortName())
                .withName(source.getFullName())
                .withWelshName(source.getNameWelsh())
                .withAddress(address)
                .withContact(ofNullable(source.getContactEmailAddress()).map(email -> contactNumber().withPrimaryEmail(email).build()).orElse(null))
                .withMajorCreditorCode(source.getMajorCreditorCode())
                .withProsecutorCategory(source.getProsecutorCategory())
                .withProsecutionAuthorityOUCode(source.getOucode())
                .build();
    }
}
