package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtCentre;

import org.junit.jupiter.api.Test;

public class CourtCentreMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToBoxHearingRequestCourtCentre() {
        final BoxHearingRequest boxHearingRequest = sourceSubmitApplication.getBoxHearingRequest();
        final CourtCentre sourceCourtCentre = boxHearingRequest.getCourtCentre();

        final uk.gov.justice.core.courts.CourtCentre targetCourtCentre = CodeCentreMapper.convertCourtCentre.apply(sourceCourtCentre);
        assertThat(targetCourtCentre.getId(), is(sourceCourtCentre.getId()));
        assertThat(targetCourtCentre.getCode(), is(sourceCourtCentre.getCode()));
        assertThat(targetCourtCentre.getName(), is(sourceCourtCentre.getName()));
    }

    @Test
    public void shouldConvertEventPayloadToBoxHearingRequestCourtCentreIsNull() {
        final uk.gov.justice.core.courts.CourtCentre targetCourtCentre = CodeCentreMapper.convertCourtCentre.apply(null);
        assertTrue(isNull(targetCourtCentre));
    }
}