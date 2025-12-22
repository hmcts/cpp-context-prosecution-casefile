package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.DefendantHearings.defendantHearings;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.HearingDay.hearingDay;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDefendantHearingsTest {

    private static final String JSON_RESPONSE_FILE = "progression-query-case-defendant-hearings.json";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    private final String caseId = UUID.randomUUID().toString();
    private final String defendantId = UUID.randomUUID().toString();
    private final String dtString = now(ZoneId.of("UTC")).format(ZONE_DATETIME_FORMATTER);
    private final ZonedDateTime sittingDay = parse(dtString, ZONE_DATETIME_FORMATTER);


    @Test
    public void shouldDeserializeJsonToCaseDefendantHearings() {
        final CaseDefendantHearings caseDefendantHearings = jsonObjectToObjectConverter.convert(readJsonResource(JSON_RESPONSE_FILE, caseId, defendantId, sittingDay.format(ZONE_DATETIME_FORMATTER)), CaseDefendantHearings.class);

        assertThat(caseDefendantHearings.getCaseId(), is(caseId));
        assertThat(caseDefendantHearings.getDefendantId(), is(defendantId));
        assertThat(caseDefendantHearings.getHearings().get(0).getHearingDays().get(0).getSittingDay(), is(sittingDay));
    }

    @Test
    public void shouldGetEarliestHearingDayAndEvaluateIfEarliestHearingDayInThePast() {
        final CaseDefendantHearings caseDefendantHearings = jsonObjectToObjectConverter.convert(readJsonResource(JSON_RESPONSE_FILE, caseId, defendantId, sittingDay.format(ZONE_DATETIME_FORMATTER)), CaseDefendantHearings.class);

        assertThat(caseDefendantHearings.getEarliestHearingDay(), is(sittingDay));
        assertThat(caseDefendantHearings.isEarliestHearingDayInThePast(), is(true));
    }

    @Test
    public void shouldEvaluateIfEarliestHearingDayInFuture() {
        final String sittingDayInFuture = now(ZoneId.of("UTC")).plusDays(1).format(ZONE_DATETIME_FORMATTER);
        final CaseDefendantHearings caseDefendantHearings = jsonObjectToObjectConverter.convert(readJsonResource(JSON_RESPONSE_FILE, caseId, defendantId,
                sittingDayInFuture), CaseDefendantHearings.class);

        assertThat(caseDefendantHearings.getEarliestHearingDay().format(ZONE_DATETIME_FORMATTER), is(sittingDayInFuture));
        assertThat(caseDefendantHearings.isEarliestHearingDayInFuture(), is(true));
    }

    @Test
    public void shouldSortToEarliestHearingDay() {
        final ZonedDateTime sittingDay1 = now(ZoneId.of("UTC")).minusMonths(2);
        final ZonedDateTime sittingDay2 = now(ZoneId.of("UTC")).plusHours(1);
        final ZonedDateTime sittingDay3 = now(ZoneId.of("UTC")).plusDays(1);

        final CaseDefendantHearings caseDefendantHearings = new CaseDefendantHearings(caseId, defendantId,
                asList(defendantHearings().withHearingDays(asList(hearingDay().withSittingDay(sittingDay1).build(), hearingDay().withSittingDay(sittingDay2).build()))
                                .build(),
                        defendantHearings().withHearingDays(asList(hearingDay().withSittingDay(sittingDay3).build(), hearingDay().withSittingDay(now(ZoneId.of("UTC")).plusMonths(1)).build()))
                                .build()));


        assertThat(caseDefendantHearings.getHearings().stream()
                .mapToLong(dh -> dh.getHearingDays().size()).sum(), is(4L));
        assertThat(caseDefendantHearings.getEarliestHearingDay(), is(sittingDay1));
    }


}