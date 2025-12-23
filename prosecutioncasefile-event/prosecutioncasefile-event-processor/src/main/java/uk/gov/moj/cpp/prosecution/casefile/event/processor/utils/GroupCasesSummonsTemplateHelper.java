package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class GroupCasesSummonsTemplateHelper {

    private GroupCasesSummonsTemplateHelper(){
    }

    public static JsonObject createTemplatePayload(final GroupProsecutionList groupProsecutionList, final GroupProsecutionWithReferenceData masterCase){

        final JsonArrayBuilder defendantsBuilder = createArrayBuilder();
        groupProsecutionList.getGroupProsecutionWithReferenceDataList().forEach(
                groupProsecutionWithReferenceData -> defendantsBuilder.add(buildDefendant(groupProsecutionWithReferenceData))
        );

        final GroupProsecution masterGroupProsecution = masterCase.getGroupProsecution();
        final Offence offence = masterGroupProsecution.getDefendants().get(0).getOffences().get(0);

        return createObjectBuilder()
                .add("dateReceived", masterGroupProsecution.getCaseDetails().getDateReceived().toString())
                .add("feeStatus", "OUTSTANDING")
                .add("feeReference", masterGroupProsecution.getPaymentReference())
                .add("prosecutor", masterGroupProsecution.getCaseDetails().getProsecutor().getProsecutingAuthority())
                .add("offenceTitle", offence.getReferenceData().getTitle())
                .add("offenceCode", offence.getOffenceCode())
                .add("legislation", offence.getReferenceData().getLegislation())
                .add("totalDefendants", groupProsecutionList.getGroupProsecutionWithReferenceDataList().size())
                .add("defendants", defendantsBuilder)
                .build();
    }

    private static JsonObject buildDefendant(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData) {
        final Defendant defendant = groupProsecutionWithReferenceData.getGroupProsecution().getDefendants().get(0);
        return createObjectBuilder()
                .add("name", buildDefendantName(defendant))
                .add("address", buildDefendantAddress(defendant))
                .add("offenceWording", defendant.getOffences().get(0).getOffenceWording())
                .build();
    }

    private static String buildDefendantName(final Defendant defendant){
        if(nonNull(defendant.getOrganisationName())){
            return defendant.getOrganisationName();
        }

        final PersonalInformation personalInformation = defendant.getIndividual().getPersonalInformation();
        return personalInformation.getFirstName() + " " + personalInformation.getLastName();
    }

    private static String buildDefendantAddress(final Defendant defendant){
        final Address address = getDefendantAddress(defendant);

        if(isNull(address)){
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        if(isNoneEmpty(address.getAddress1())) {
            sb.append(address.getAddress1()).append(", ");
        }

        if(isNoneEmpty(address.getAddress2())) {
            sb.append(address.getAddress2()).append(", ");
        }

        if(isNoneEmpty(address.getAddress3())) {
            sb.append(address.getAddress3()).append(", ");
        }

        if(isNoneEmpty(address.getAddress4())) {
            sb.append(address.getAddress4()).append(", ");
        }

        if(isNoneEmpty(address.getAddress5())) {
            sb.append(address.getAddress5()).append(", ");
        }

        if(isNoneEmpty(address.getPostcode())) {
            sb.append(address.getPostcode()).append(", ");
        }

        return sb.toString();
    }

    private static Address getDefendantAddress(final Defendant defendant){
        if (nonNull(defendant.getAddress())){
            return defendant.getAddress();
        } else if (nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getPersonalInformation().getAddress())){
            return defendant.getIndividual().getPersonalInformation().getAddress();
        }
        return null;
    }
}
