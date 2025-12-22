package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HearingDateValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final Optional<InitialHearing> initialHearing = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .map(Defendant::getInitialHearing)
                .filter(Objects::nonNull)
                .findFirst();

        if (initialHearing.isPresent()) {
            final String dateTimeString = initialHearing.get().getDateOfHearing() + initialHearing.get().getTimeOfHearing();
            DateTimeFormatter dateTimeFormatterWithMilliSeconds = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss.SSS");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss");

            final DateTimeFormatterBuilder dateTimeFormatterBuilder = new DateTimeFormatterBuilder();
            if (dateTimeString.length() == 18) {
                dateTimeFormatterBuilder.append(dateTimeFormatter);
            } else {
                dateTimeFormatterBuilder.append(dateTimeFormatterWithMilliSeconds);
            }

            final ZonedDateTime hearingDate = ZonedDateTime.parse(dateTimeString, dateTimeFormatterBuilder.toFormatter().withZone(ZoneId.of("UTC")));

            if (hearingDate.isBefore(now())) {
                return newValidationResult(of(newProblem(ProblemCode.DATE_OF_HEARING_IN_THE_PAST, problemValue()
                        .withKey("hearing.date.in.the.past")
                        .withValue(hearingDate.toString())
                        .build())));
            } else {
                return VALID;
            }

        } else {

            return newValidationResult(of(newProblem(ProblemCode.DATE_OF_HEARING_NOT_AVAILABLE, problemValue()
                    .withValue("date of hearing is not available")
                    .build())));

        }

    }

}
