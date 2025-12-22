package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CourtApplicationCaseMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicationCases() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();

        List<CourtApplicationCase> targetCourtApplication = CourtApplicationCaseMapper.convertCourtApplicationCase.apply(sourceCourtApplication.getCourtApplicationCases());
        assertThat(targetCourtApplication.size(), is(sourceCourtApplication.getCourtApplicationCases().size()));
        assertThat(targetCourtApplication.get(0).getProsecutionCaseIdentifier().getCaseURN(), is(sourceCourtApplication.getCourtApplicationCases().get(0).getCaseURN()));
        assertThat(targetCourtApplication.get(1).getProsecutionCaseIdentifier().getCaseURN(), is(sourceCourtApplication.getCourtApplicationCases().get(1).getCaseURN()));
        assertThat(targetCourtApplication.get(2).getProsecutionCaseIdentifier().getCaseURN(), is(sourceCourtApplication.getCourtApplicationCases().get(2).getCaseURN()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationWhenCourtApplicationCasesEmptyOrNull() {
        final List<CourtApplicationCase> targetCourtApplicationEmpty = CourtApplicationCaseMapper.convertCourtApplicationCase.apply(emptyList());
        assertThat(targetCourtApplicationEmpty.size(), is(0));

        final List<CourtApplicationCase> targetCourtApplicationNull = CourtApplicationCaseMapper.convertCourtApplicationCase.apply(null);
        assertThat(targetCourtApplicationNull, nullValue());
    }
}