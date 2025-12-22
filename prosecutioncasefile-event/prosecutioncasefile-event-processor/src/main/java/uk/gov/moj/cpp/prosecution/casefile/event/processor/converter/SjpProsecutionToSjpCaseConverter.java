package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase.createSjpCase;

import uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated;

import javax.inject.Inject;

public class SjpProsecutionToSjpCaseConverter implements Converter<SjpProsecutionInitiated, CreateSjpCase> {

    private final ProsecutionCaseFileDefendantToSjpDefendantConverter prosecutionDefendantToSjpDefendantConverter;

    @Inject
    public SjpProsecutionToSjpCaseConverter(ProsecutionCaseFileDefendantToSjpDefendantConverter prosecutionDefendantToSjpDefendantConverter) {
        this.prosecutionDefendantToSjpDefendantConverter = prosecutionDefendantToSjpDefendantConverter;
    }

    @Override
    public CreateSjpCase convert(final SjpProsecutionInitiated source) {
        final Prosecution prosecution = source.getProsecution();
        final ProsecutorsReferenceData prosecutor = prosecution.getCaseDetails()
                .getProsecutor().getReferenceData();
        final Defendant defendant = prosecution.getDefendants().get(0);

        return createSjpCase()
                .withId(prosecution.getCaseDetails().getCaseId())
                .withEnterpriseId(source.getEnterpriseId())
                .withUrn(prosecution.getCaseDetails().getProsecutorCaseReference())
                .withPostingDate(defendant.getPostingDate())
                .withProsecutingAuthority(prosecutor.getShortName())
                .withDefendant(prosecutionDefendantToSjpDefendantConverter.convert(defendant))
                .withCosts(defendant.getAppliedProsecutorCosts())
                .build();
    }

}
