package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Address;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.LegalEntityDefendant;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Offence;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.PersonalDetails;
import uk.gov.moj.cpp.prosecution.casefile.query.api.utils.OffenceHelper;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail.caseDetail;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.PersonalDetails.personalDetails;

public class CaseDetailConverter {

    @Inject
    private OffenceHelper offenceHelper;

    @Inject
    private ReferenceOffencesDataService referenceOffencesDataService;

    public List<CaseDetail> convert(final Envelope<?> originatingQuery, final Requester requester, final ProsecutionCase source, final List<Defendant> matchingDefendants) {

        final List<CaseDetail> caseDetailDefendants = new ArrayList<>();

        matchingDefendants.forEach(defendant -> {

            final CaseDetail.Builder caseDetailBuilder = caseDetail()
                    .withId(source.getId().toString())
                    .withUrn(ofNullable(source.getProsecutionCaseIdentifier()).map(ProsecutionCaseIdentifier::getCaseURN).orElse(null))
                    .withDefendant(toDefendant(originatingQuery, requester, defendant));
            ofNullable(source.getInitiationCode()).ifPresent(ic -> caseDetailBuilder.withInitiationCode(ic.name()));

            caseDetailDefendants.add(caseDetailBuilder.build());
        });

        return caseDetailDefendants;
    }

    private uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Defendant toDefendant(final Envelope<?> originatingQuery, final Requester requester, final Defendant defendant) {
        final PersonalDetails.Builder personalDetailsBuilder = personalDetails();

        ofNullable(defendant.getPersonDefendant()).map(PersonDefendant::getPersonDetails).ifPresent(pd -> {
            personalDetailsBuilder.withFirstname(pd.getFirstName());
            personalDetailsBuilder.withLastname(pd.getLastName());
            personalDetailsBuilder.withDateOfBirth(pd.getDateOfBirth());
            if (nonNull(pd.getContact())) {
                personalDetailsBuilder.withContactDetails(new ContactDetails(pd.getContact().getPrimaryEmail(),
                        pd.getContact().getHome(), pd.getContact().getWork(), pd.getContact().getMobile()));
            }
            ofNullable(pd.getAddress()).ifPresent(addr -> personalDetailsBuilder.withAddress(toAddress(addr)));
        });

        final uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Defendant.Builder defendantBuilder = defendant()
                .withId(defendant.getId().toString())
                .withPersonalDetails(personalDetailsBuilder.build())
                .withOffences(toOffences(originatingQuery, requester, defendant.getOffences()))
                .pcqId(defendant.getPcqId())
                .withInitiationCode( defendant.getInitiationCode() != null ? defendant.getInitiationCode().toString() : null);

        ofNullable(defendant.getLegalEntityDefendant())
                .map(uk.gov.justice.core.courts.LegalEntityDefendant::getOrganisation)
                .ifPresent(defOrg -> defendantBuilder.withLegalEntityDefendant(toLegalEntityDefendant(defOrg)));

        return defendantBuilder.build();
    }

    private LegalEntityDefendant toLegalEntityDefendant(final Organisation defendantOrg) {
        final LegalEntityDefendant.Builder legalEntityBuilder = legalEntityDefendant()
                .withName(defendantOrg.getName())
                .withAddress(toAddress(defendantOrg.getAddress()))
                .withIncorporationNumber(defendantOrg.getIncorporationNumber());

        ofNullable(defendantOrg.getContact())
                .ifPresent(c -> legalEntityBuilder.withContactDetails(new ContactDetails(c.getPrimaryEmail(), c.getHome(), c.getWork(), c.getMobile())));

        return legalEntityBuilder.build();
    }

    private List<Offence> toOffences(final Envelope<?> originatingQuery, final Requester requester, final List<uk.gov.justice.core.courts.Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return emptyList();
        }

        final List<Offence> offenceList = new ArrayList<>();
        offences.forEach(o -> {
            final Offence.Builder offenceBuilder = Offence.offence();
            offenceBuilder.withId(o.getId().toString());
            ofNullable(o.getPlea()).ifPresent(plea -> offenceBuilder.withPlea(plea.getPleaValue()));
            offenceBuilder.withTitle(o.getOffenceTitle());
            offenceBuilder.withTitleWelsh(o.getOffenceTitleWelsh());
            offenceBuilder.withLegislation(o.getOffenceLegislation());
            offenceBuilder.withLegislationWelsh(o.getOffenceLegislationWelsh());
            offenceBuilder.withWording(o.getWording());
            offenceBuilder.withWordingWelsh(o.getWordingWelsh());
            offenceBuilder.withEndorsable(o.getEndorsableFlag());
            offenceBuilder.withImprisonable(offenceHelper.isOffenceSummaryType(buildOffenceObject(originatingQuery, requester, o.getOffenceCode(), o.getStartDate())));
            offenceBuilder.withHasPlea(hasPleaAlready(o));
            offenceBuilder.withOnlinePleaReceived(o.getOnlinePleaReceived());

            offenceList.add(offenceBuilder.build());
        });

        return offenceList;
    }

    private JsonObject buildOffenceObject(final Envelope<?> originatingQuery, final Requester requester, final String offenceCode, final String startDate) {
        return referenceOffencesDataService.getOffenceReferenceData(originatingQuery, requester, offenceCode, startDate);
    }

    private Address toAddress(final uk.gov.justice.core.courts.Address sourceAddress) {
        return Address.address()
                .withAddress1(sourceAddress.getAddress1())
                .withAddress2(sourceAddress.getAddress2())
                .withAddress3(sourceAddress.getAddress3())
                .withAddress4(sourceAddress.getAddress4())
                .withAddress5(sourceAddress.getAddress5())
                .withPostcode(sourceAddress.getPostcode())
                .build();
    }

    private boolean hasPleaAlready(uk.gov.justice.core.courts.Offence offence) {
        return nonNull(offence.getNotifiedPlea())
                || (nonNull(offence.getOnlinePleaReceived()) && offence.getOnlinePleaReceived());
    }

}
