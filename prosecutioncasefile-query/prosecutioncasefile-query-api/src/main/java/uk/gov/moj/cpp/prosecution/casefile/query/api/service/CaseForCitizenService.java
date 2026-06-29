package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.LEGAL_ENTITY;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.PERSON;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.INITIATION_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail.caseDetail;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail.fromCaseDetail;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.ALREADY_PLEADED;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.NO_MATCH_FOUND;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.OUT_OF_TIME;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.TOO_MANY_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.fromCaseDetail;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDefendantHearings;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Offence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseForCitizenService {

    public static final String DOB_DATE_PATTERN = "yyyy-MM-dd";
    public static final String ID = "id";
    public static final String PCQ_ID = "pcqId";
    public static final String PERSONAL_DETAILS = "personalDetails";
    public static final String LEGAL_ENTITY_DETAILS = "legalEntityDetails";
    public static final String LEGAL_ENTITY_DEFENDANT = "legalEntityDefendant";
    public static final String OFFENCES = "offences";
    public static final String SJP_TYPE = "SJP";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private SjpService sjpService;

    @Inject
    private ProgressionService progressionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseForCitizenService.class.getName());

    public JsonObject getCaseWithDefendant(final JsonEnvelope jsonEnvelope, final Requester requester, final DefendantType defendantType,
                                           final String caseUrn, final String postcode, final LocalDate dob) {

        if (defendantType == PERSON) {
            return objectToJsonObjectConverter.convert(getCaseWithDefendant(jsonEnvelope, requester, caseUrn, postcode, dob));
        } else {
            return objectToJsonObjectConverter.convert(getCaseWithDefendant(jsonEnvelope, requester, caseUrn, postcode));
        }
    }

    private CaseDetailWithReason getCaseWithDefendant(final JsonEnvelope jsonEnvelope, final Requester requester, final String caseUrn,
                                                      final String postcode, final LocalDate dob) {

        final Optional<CaseDetail> caseDetailFromSjp = getSjpCaseDetail(jsonEnvelope, requester, caseUrn, postcode);

        //find case defendant in progression
        final Optional<String> caseId = progressionService.getCaseId(jsonEnvelope, requester, caseUrn);
        if (caseId.isPresent()) {

            final List<CaseDetail> caseDetailWithDefendants = progressionService.getCaseDetailWithDefendantForPerson(jsonEnvelope, requester, caseId.get(), postcode);
            caseDetailFromSjp.ifPresent(cd -> {
                LOGGER.info("Found case with defendant in SJP for the caseUrn={}", caseUrn);
                caseDetailWithDefendants.add(cd);
            });

            final List<CaseDetail> matchingDefendantList = getCaseDetailDefendantMatch(caseDetailWithDefendants, dob);

            final Optional<CaseDetailWithReason> matchFromDefendantAndOffences = getMatchFromDefendantAndOffences(matchingDefendantList, caseUrn, caseId.get());
            if (matchFromDefendantAndOffences.isPresent()) {
                return matchFromDefendantAndOffences.get();
            } else {
                final CaseDefendantHearings defHearings = progressionService.getCaseDefendantHearings(jsonEnvelope, requester, caseId.get(), matchingDefendantList.get(0).getDefendant().getId());
                final Optional<CaseDetailWithReason> caseDetailMatch = getMatchFromDefendantHearingDays(defHearings, matchingDefendantList);
                if (caseDetailMatch.isPresent()) {
                    return caseDetailMatch.get();
                }
            }
        } else {
            //case not in progression; return if present in SJP
            if (caseDetailFromSjp.isPresent()) {
                LOGGER.info("Match!! Found matching defendant in SJP for the caseUrn={}", caseUrn);
                return fromCaseDetail(caseDetailFromSjp.get(), TRUE);
            }
        }

        LOGGER.info("No Match!! No case found for the caseUrn={} & defendantType={}", caseUrn, PERSON);
        return fromCaseDetail(caseDetail().withUrn(caseUrn).build(), FALSE, NO_MATCH_FOUND);
    }

    private CaseDetailWithReason getCaseWithDefendant(final JsonEnvelope jsonEnvelope, final Requester requester,
                                                      final String caseUrn, final String postcode) {
        final Optional<CaseDetail> caseDetailFromSjp = getSjpCaseDetail(jsonEnvelope, requester, caseUrn, postcode);

        final Optional<String> caseId = progressionService.getCaseId(jsonEnvelope, requester, caseUrn);
        if (caseId.isPresent()) {
            final List<CaseDetail> caseDetailWithDefendants = progressionService.getCaseDetailWithDefendantForLegalEntity(jsonEnvelope, requester, caseId.get(), postcode);

            final Optional<CaseDetailWithReason> matchFromDefendantAndOffences = getMatchFromDefendantAndOffences(caseDetailWithDefendants, caseUrn, caseId.get());
            if (matchFromDefendantAndOffences.isPresent()) {
                return matchFromDefendantAndOffences.get();
            } else {
                final CaseDefendantHearings defHearings = progressionService.getCaseDefendantHearings(jsonEnvelope, requester, caseId.get(), caseDetailWithDefendants.get(0).getDefendant().getId());
                final Optional<CaseDetailWithReason> caseDetailMatch = getMatchFromDefendantHearingDays(defHearings, caseDetailWithDefendants);
                if (caseDetailMatch.isPresent()) {
                    return caseDetailMatch.get();
                }
            }
        } else {
            //case not in progression; return if present in SJP
            if (caseDetailFromSjp.isPresent()) {
                LOGGER.info("Match!! Found matching defendant in SJP for the caseUrn={}", caseUrn);
                return fromCaseDetail(caseDetailFromSjp.get(), TRUE);
            }
        }

        LOGGER.info("No Match!! No case found for the caseUrn={} & defendantType={}", caseUrn, LEGAL_ENTITY);
        return fromCaseDetail(caseDetail().withUrn(caseUrn).build(), FALSE, NO_MATCH_FOUND);
    }

    private Optional<CaseDetailWithReason> getMatchFromDefendantAndOffences(final List<CaseDetail> matchingDefendant,
                                                                            final String caseUrn, final String caseId) {

        if (matchingDefendant.isEmpty() || isNull(matchingDefendant.get(0).getDefendant())) {
            LOGGER.info("No Match!! No case with defendant details in Progression or SJP for the caseUrn={}", caseUrn);
            return of(fromCaseDetail(caseDetail().withUrn(caseUrn).withId(caseId).build(),
                    FALSE, NO_MATCH_FOUND));
        } else if (matchingDefendant.size() > 1) {
            LOGGER.info("No Match!! Found case with more than one matching defendant for the caseUrn={}", caseUrn);
            return of(fromCaseDetail(caseDetail().withUrn(caseUrn).withId(caseId).build(),
                    FALSE, TOO_MANY_DEFENDANTS));
        } else if (nonNull(matchingDefendant.get(0).getDefendant())
                && anyOffenceHasOnlinePlea(matchingDefendant.get(0).getDefendant().getOffences())) {
            LOGGER.info("No Match!! Found defendant with id={} already with online plea for the caseUrn={}", matchingDefendant.get(0).getDefendant().getId(), caseUrn);
            return of(fromCaseDetail(matchingDefendant.get(0), FALSE, ALREADY_PLEADED));
        }
        return empty();
    }

    private Optional<CaseDetailWithReason> getMatchFromDefendantHearingDays(final CaseDefendantHearings defHearings, final List<CaseDetail> matchingDefendant) {
        final CaseDetail caseDetailMatch = matchingDefendant.get(0);
        if (defHearings.isEarliestHearingDayInThePast()) {
            LOGGER.info("No Match!! Found defendant with id={} with earliestHearingDay={} in the past for the caseUrn={}",
                    caseDetailMatch.getDefendant().getId(), defHearings.getEarliestHearingDay(), caseDetailMatch.getUrn());

            return of(fromCaseDetail(caseDetailMatch, FALSE, OUT_OF_TIME));
        }

        if (defHearings.isEarliestHearingDayInFuture()
                && !anyOffenceHasOnlinePlea(caseDetailMatch.getDefendant().getOffences())) {

            LOGGER.info("Match!! Found matching defendant with id={} for the caseUrn={}", caseDetailMatch.getDefendant().getId(), caseDetailMatch.getUrn());
            return of(fromCaseDetail(caseDetailMatch, TRUE));
        }
        return empty();
    }

    private Optional<CaseDetail> getSjpCaseDetail(final JsonEnvelope jsonEnvelope, final Requester requester, final String caseUrn, final String postcode) {
        final JsonObject sjpJsonResponse = sjpService.findCase(jsonEnvelope, requester, caseUrn, postcode);
        final JsonObject newSJPResponse = getNewSJPResponse(sjpJsonResponse);
        return ofNullable(newSJPResponse)
                .map(sjpResp -> fromCaseDetail(jsonObjectToObjectConverter.convert(sjpResp, CaseDetail.class), INITIATION_CODE));


    }

    public JsonObject getNewSJPResponse(final JsonObject sjpJsonResponse) {
        if (nonNull(sjpJsonResponse)) {
            final JsonObjectBuilder builder = createObjectBuilder();
            builder.add("urn", sjpJsonResponse.getString("urn"));
            builder.add(ID, sjpJsonResponse.getString(ID));
            builder.add("type", SJP_TYPE);
            builder.add("defendant", buildDefendant(sjpJsonResponse.getJsonObject("defendant")));

            ofNullable(sjpJsonResponse.getString("initiationCode", null)).ifPresent(initiationCode -> builder.add("initiationCode", initiationCode));
            builder.add("assigned", sjpJsonResponse.getBoolean("assigned", FALSE));
            builder.add("completed", sjpJsonResponse.getBoolean("completed", FALSE));

            ofNullable(sjpJsonResponse.getString("status", null)).ifPresent(status -> builder.add("status", status));
            builder.add("policeFlag", sjpJsonResponse.getBoolean("policeFlag", FALSE));

            ofNullable(sjpJsonResponse.getJsonNumber("costs")).ifPresent(costs -> builder.add("costs", costs));
            ofNullable(sjpJsonResponse.getJsonNumber("aocpVictimSurcharge")).ifPresent(aocpVictimSurcharge -> builder.add("aocpVictimSurcharge", aocpVictimSurcharge));
            ofNullable(sjpJsonResponse.getJsonNumber("aocpTotalCost")).ifPresent(aocpTotalCost -> builder.add("aocpTotalCost", aocpTotalCost));
            ofNullable(sjpJsonResponse.getBoolean("aocpEligible", FALSE)).ifPresent(aocpEligible -> builder.add("aocpEligible", aocpEligible));

            builder.add("readyForDecision", sjpJsonResponse.getBoolean("readyForDecision", FALSE));

            return builder.build();
        }
        return sjpJsonResponse;
    }

    private static JsonObject buildDefendant(final JsonObject defendant) {
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add(ID, defendant.getString(ID));
        ofNullable(defendant.getJsonString(PCQ_ID)).ifPresent(pcqId -> builder.add(PCQ_ID, pcqId));
        ofNullable(defendant.getJsonObject(PERSONAL_DETAILS)).ifPresent(personalDetails -> builder.add(PERSONAL_DETAILS, personalDetails));
        ofNullable(defendant.getJsonObject(LEGAL_ENTITY_DETAILS)).ifPresent(legalEntityDetails -> builder.add(LEGAL_ENTITY_DEFENDANT, buildLegalEntity(defendant.getJsonObject(LEGAL_ENTITY_DETAILS))));
        ofNullable(defendant.getJsonArray(OFFENCES)).ifPresent(offences -> builder.add(OFFENCES, offences));
        return builder.build();
    }

    private static JsonObject buildLegalEntity(final JsonObject legalEntityDetails) {

        final JsonObjectBuilder builder = createObjectBuilder();
        ofNullable(legalEntityDetails.getJsonObject("address")).ifPresent(address -> builder.add("address", address));
        ofNullable(legalEntityDetails.getJsonObject("contactDetails")).ifPresent(contactDetails -> builder.add("contactDetails", contactDetails));
        builder.add("name", legalEntityDetails.getString("legalEntityName"));
        return builder.build();
    }

    private boolean anyOffenceHasOnlinePlea(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return false;
        }
        return offences.stream().anyMatch(o -> nonNull(o.getOnlinePleaReceived()) && o.getOnlinePleaReceived());
    }

    private List<CaseDetail> getCaseDetailDefendantMatch(final List<CaseDetail> caseDetailWithDefendants, final LocalDate dob) {
        if (hasSingleDefendant(caseDetailWithDefendants)) {
            return caseDetailWithDefendants;
        } else {
            return caseDetailWithDefendants.stream()
                    .filter(cd -> isDefendantMatch(cd.getDefendant(), dob))
                    .collect(Collectors.toList());
        }
    }

    private boolean hasSingleDefendant(List<CaseDetail> caseDetailWithDefendants) {
        return caseDetailWithDefendants.stream().map(CaseDetail::getDefendant).filter(Objects::nonNull).count() == 1;
    }

    private boolean isDefendantMatch(final Defendant defendant, final LocalDate dob) {
        return nonNull(defendant.getPersonalDetails())
                && nonNull(defendant.getPersonalDetails().getDateOfBirth())
                && parse(defendant.getPersonalDetails().getDateOfBirth(), ofPattern(DOB_DATE_PATTERN)).equals(dob);
    }

}
