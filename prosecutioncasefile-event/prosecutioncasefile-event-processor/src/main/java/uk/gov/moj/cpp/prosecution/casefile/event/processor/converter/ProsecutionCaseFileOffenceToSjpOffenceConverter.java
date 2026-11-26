package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.json.schemas.domains.sjp.commands.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Details;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Document;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Welsh;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ProsecutionCaseFileOffenceToSjpOffenceConverter implements Converter<List<Offence>, List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence>> {

    private final OffenceIdGenerator offenceIdGenerator;

    @Inject
    public ProsecutionCaseFileOffenceToSjpOffenceConverter(OffenceIdGenerator offenceIdGenerator) {
        this.offenceIdGenerator = offenceIdGenerator;
    }


    @Override
    @SuppressWarnings({"squid:S1135", "squid:S1188"})
    public List<uk.gov.justice.json.schemas.domains.sjp.commands.Offence> convert(final List<Offence> source) {
        return source.stream()
                .map(offence ->
                        {
                            final boolean availableReportRestrictResultCode = !StringUtils.isEmpty(ofNullable(offence.getReferenceData())
                                    .map(OffenceReferenceData::getReportRestrictResultCode).orElse(""));
                            return offence()
                                    //Offence in SJP has id field as mandatory, there will be future story to delete the id from sjp offence.
                                    // TODO ATCM-3870: GENERATE ID FOR HISTORICAL EVENTS
                                    .withId(ofNullable(offence.getOffenceId()).map(UUID::toString).orElseGet(() -> offenceIdGenerator.generateId()))
                                    .withProsecutionFacts(offence.getStatementOfFacts())
                                    .withOffenceTitle(ofNullable(offence.getReferenceData())
                                            .map(OffenceReferenceData::getTitle)
                                            .orElse(null))
                                    .withOffenceTitleWelsh(ofNullable(offence.getReferenceData())
                                            .map(OffenceReferenceData::getDetails)
                                            .map(Details::getDocument)
                                            .map(Document::getWelsh)
                                            .map(Welsh::getWelshoffencetitle)
                                            .orElse(null))
                                    .withOffenceWording(offence.getOffenceWording())
                                    .withOffenceWordingWelsh(offence.getOffenceWordingWelsh())
                                    .withOffenceSequenceNo(offence.getOffenceSequenceNumber())
                                    .withOffenceCommittedDate(ofNullable(offence.getOffenceCommittedDate()).map(LocalDates::to).orElse(null))
                                    .withLibraOffenceCode(offence.getOffenceCode())
                                    .withLibraOffenceDateCode(offence.getOffenceDateCode())
                                    .withCompensation(offence.getAppliedCompensation())
                                    .withChargeDate(offence.getChargeDate())
                                    .withBackDuty(offence.getBackDuty())
                                    .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                                    .withBackDutyDateTo(offence.getBackDutyDateTo())
                                    .withVehicleMake(offence.getVehicleMake())
                                    .withVehicleRegistrationMark(offence.getVehicleRegistrationMark())
                                    .withEndorsable(ofNullable(offence.getReferenceData()).map(OffenceReferenceData::getEndorsableFlag).orElse(null))
                                    .withPressRestrictable(availableReportRestrictResultCode)
                                    .withProsecutorOfferAOCP(offence.getProsecutorOfferAOCP())
                                    .build();
                        }
                )
                .collect(toList());
    }

    // for testing - allows to control ids
    public static class OffenceIdGenerator {

        public String generateId() {
            return UUID.randomUUID().toString();
        }
    }
}
