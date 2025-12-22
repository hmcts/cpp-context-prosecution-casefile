package uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.List;

public class RuleConstants {

     static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
     static final String GROUP_LISTING_OFFICERS = "Listing Officers";
     static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
     static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
     static final String GROUP_SYSTEM_USERS = "System Users";
     static final String GROUP_COURT_ASSOCIATE = "Court Associate";
     static final String GROUP_COURT_CLERKS = "Court Clerks";
     static final String GROUP_ONLINE_PLEA_SYSTEM_USERS = "Online Plea System Users";


    private RuleConstants() {
    }

    public static List<String> getQueryCaseActionGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryCaseByProsecutionReferenceActionGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryCaseErrorsActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCountsCasesErrorsActionGroups() {
        return asList(GROUP_CROWN_COURT_ADMIN, GROUP_LEGAL_ADVISERS, GROUP_COURT_ASSOCIATE, GROUP_COURT_CLERKS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCaseForCitizenActionGroups() {
        return singletonList(GROUP_ONLINE_PLEA_SYSTEM_USERS);
    }
}
