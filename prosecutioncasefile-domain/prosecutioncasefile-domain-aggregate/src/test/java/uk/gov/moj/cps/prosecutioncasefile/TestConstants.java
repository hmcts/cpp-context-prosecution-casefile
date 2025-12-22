package uk.gov.moj.cps.prosecutioncasefile;

import static java.time.LocalDate.of;
import static java.util.UUID.randomUUID;

import java.time.LocalDate;
import java.util.UUID;

public class TestConstants {
    public static final UUID APPLICATION_ID = randomUUID();
    public static final UUID APPLICATION_ID_2 = randomUUID();
    public static final UUID CASE_ID = randomUUID();
    public static final UUID EXTERNAL_ID = randomUUID();
    public static final UUID EXTERNAL_ID_2 = randomUUID();
    public static final UUID EXTERNAL_ID_3 = randomUUID();
    public static final String OFFENCE_CODE = "A00PCD7073";
    public static final LocalDate ARREST_DATE = LocalDate.now().minusMonths(4);
    public static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.now();
    public static final LocalDate OFFENCE_CHARGE_DATE = LocalDate.now().minusMonths(4);
    public static final String SURNAME = "Bloggs";
    public static final String SECOND_SURNAME = "Floggs";
    public static final String THIRD_SURNAME = "Hackney";
    public static final String FOURTH_SURNAME = "Becks";
    public static final String FITH_SURNAME = "Halland";
    public static final LocalDate BIRTH_DATE = of(1991, 5, 5);
    public static final String DEFENDANT_ID = "4ca8e2eb-8253-4d2c-9c57-d4fc4b91e6fe";
    public static final String SECOND_DEFENDANT_ID = "60fc76f7-af1b-4c7d-94de-1db1d1eb49ce";
    public static final String THIRD_DEFENDANT_ID = randomUUID().toString();
    public static final String FOURTH_DEFENDANT_ID = randomUUID().toString();
    public static final String FIFTH_DEFENDANT_ID = randomUUID().toString();
    public static final String COURT_HEARING_LOCATION = "B016771";
    public static final String DATE_OF_HEARING = "2050-10-03";
    public static final String DATE_OF_HEARING_IN_PAST = "1950-10-03";
    public static final String CUSTODY_STATUS = "B";
    public static final String OFFENCE_START_DATE = "2017-10-10";
    public static final String PROSECUTOR_CASE_REFERENCE = "Prosecutor Case Reference";
    public static final String POLICE_SYSTEM_ID = "00101PoliceCaseSystem";
    public static final String ORIGINATING_ORGANISATION = "Originating Organisation";
    public static final String CPS_ORGANISATION = "A30AB00";
    public static final String URN = "88GD6251318";
    public static final String FORENAME = "Joe";
    public static final String SECOND_FORENAME = "James";
    public static final String THIRD_FORENAME = "Bob";
    public static final LocalDate SECOND_BIRTH_DATE = of(1992, 3, 2);
    public static final LocalDate THIRD_BIRTH_DATE = of(1995, 3, 2);
    public static final String PROSECUTOR_DEFENDANT_REFERENCE_ONE = "DEFENDANT_REFERENCE_ONE";
    public static final String PROSECUTOR_DEFENDANT_REFERENCE_TWO = "DEFENDANT_REFERENCE_TWO";
    public static final String PROSECUTOR_DEFENDANT_REFERENCE_THREE = "DEFENDANT_REFERENCE_THREE";
    public static final String OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE = "OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE";
    public static final String SUMMONS_INITIATION_CODE = "S";
    public static final String PROSECUTOR_COST = "£300";
    public static final String ARREST_SUMMON_NUMBER = "ARREST_SUMMON_NUMBER";
    public static final UUID OFFENCE_ID = UUID.randomUUID();
}
