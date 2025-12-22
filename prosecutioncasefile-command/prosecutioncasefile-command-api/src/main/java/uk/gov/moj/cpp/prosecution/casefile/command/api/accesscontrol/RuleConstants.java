package uk.gov.moj.cpp.prosecution.casefile.command.api.accesscontrol;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.util.ActionTypes.CREATE;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.util.ObjectTypes.CASE;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuleConstants {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private static final String GROUP_CMS = "CMS";
    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_CLERKS = "Court Clerks";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_ONLINE_PLEA_SYSTEM_USERS = "Online Plea System Users";
    private static final String NCES = "NCES";


    private RuleConstants() {
    }

    public static List<String> getStartProceedingsActionGroups() {
        return singletonList(GROUP_CMS);
    }

    public static List<String> getAssignHearingActionGroups() {
        return singletonList(GROUP_CMS);
    }

    public static List<String> getInitiateSjpProsecutionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getInitiateCCProsecutionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_LISTING_OFFICERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_CROWN_COURT_ADMIN, GROUP_CLERKS, NCES);
    }

    public static List<String> getInitiateGroupProsecutionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_LISTING_OFFICERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_CROWN_COURT_ADMIN, GROUP_CLERKS);
    }

    public static List<String> getInitiateCCApplicationGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_LISTING_OFFICERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_CROWN_COURT_ADMIN, GROUP_CLERKS);
    }


    public static List<String> getAddMaterialActionGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getAddMaterialIdpcActionGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getUpdateErrorsActionGroups() {
        return asList(GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS,
                GROUP_CLERKS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getPleadOnlineActionGroups() {
        return asList(GROUP_ONLINE_PLEA_SYSTEM_USERS);
    }

    public static List<String> getPleadOnlinePcqVisitedActionGroups() {
        return asList(GROUP_ONLINE_PLEA_SYSTEM_USERS);
    }

    public static String[] expectedPermissionsForCase() throws JsonProcessingException {
        final ExpectedPermission expectedPermissionsForCase = ExpectedPermission.builder()
                .withAction(CREATE.toString())
                .withObject(CASE.toString())
                .build();
        return new String[]{objectMapper.writeValueAsString(expectedPermissionsForCase)};
    }
}
