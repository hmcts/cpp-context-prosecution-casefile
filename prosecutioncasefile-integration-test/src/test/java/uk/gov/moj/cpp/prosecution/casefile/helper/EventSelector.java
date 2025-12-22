package uk.gov.moj.cpp.prosecution.casefile.helper;

import javax.jms.MessageConsumer;


public class EventSelector {

    public static final String EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED = "prosecutioncasefile.events.sjp-prosecution-received";
    public static final String EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS = "prosecutioncasefile.events.sjp-prosecution-received-with-warnings";
    public static final String EVENT_SELECTOR_SJP_PROSECUTION_INITIATED = "prosecutioncasefile.events.sjp-prosecution-initiated";
    public static final String EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS = "prosecutioncasefile.events.sjp-prosecution-initiated-with-warnings";
    public static final String PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED = "public.prosecutioncasefile.prosecution-rejected";
    public static final String EVENT_SELECTOR_SJP_PROSECUTION_REJECTED = "prosecutioncasefile.events.sjp-prosecution-rejected";
    public static final String EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED = "prosecutioncasefile.events.prosecution-case-unsupported";
    public static final String EVENT_SELECTOR_CC_PROSECUTION_REJECTED = "prosecutioncasefile.events.cc-prosecution-rejected";
    public static final String EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL = "prosecutioncasefile.events.defendants-parked-for-summons-application-approval";
    public static final String EVENT_GROUP_CASES_RECEIVED = "prosecutioncasefile.events.group-cases-received";
    public static final String EVENT_GROUP_CASES_PARKED_FOR_APPROVAL = "prosecutioncasefile.events.group-cases-parked-for-approval";
    public static final String EVENT_GROUP_ID_RECORDED_FOR_SUMMONS_APPLICATION = "prosecutioncasefile.events.group-id-recorded-for-summons-application";
    public static final String EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY = "prosecutioncasefile.events.sjp-case-created-successfully";
    public static final String EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS = "prosecutioncasefile.events.sjp-case-created-successfully-with-warnings";
    public static final String EVENT_SELECTOR_MATERIAL_ADDED = "prosecutioncasefile.events.material-added";
    public static final String EVENT_SELECTOR_MATERIAL_ADDED_V2 = "prosecutioncasefile.events.material-added-v2";
    public static final String EVENT_SELECTOR_MATERIAL_ADDED_WITH_WARNINGS = "prosecutioncasefile.events.material-added-with-warnings";
    public static final String EVENT_SELECTOR_MATERIAL_PENDING = "prosecutioncasefile.events.material-pending";
    public static final String EVENT_SELECTOR_MATERIAL_PENDING_V2 = "prosecutioncasefile.events.material-pending-v2";
    public static final String EVENT_SELECTOR_MATERIAL_REJECTED = "prosecutioncasefile.events.material-rejected";
    public static final String EVENT_SELECTOR_MATERIAL_REJECTED_V2 = "prosecutioncasefile.events.material-rejected-v2";
    public static final String EVENT_SELECTOR_MATERIAL_REJECTED_WITH_WARNINGS = "prosecutioncasefile.events.material-rejected-with-warnings";
    public static final String PUBLIC_MATERIAL_REJECTED = "public.prosecutioncasefile.material-rejected";
    public static final String PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE = "public.prosecutioncasefile.document-bundle-arrived-for-unbundling";
    public static final String PUBLIC_DOCUMENT_REVIEW = "public.prosecutioncasefile.document-review-required";
    public static final String PUBLIC_MATERIAL_ADDED = "public.prosecutioncasefile.material-added";
    public static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.prosecution-submission-succeeded";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED = "public.prosecutioncasefile.cps-serve-material-status-updated";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED = "public.prosecutioncasefile.cps-serve-pet-submitted";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED = "public.prosecutioncasefile.cps-serve-bcm-submitted";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED = "public.prosecutioncasefile.cps-serve-cotr-submitted";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED = "public.prosecutioncasefile.cps-update-cotr-submitted";


    public static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PTPH_SUBMITTED = "public.prosecutioncasefile.cps-serve-ptph-submitted";
    public static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";
    public static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_CASE_UNSUPPORTED = "public.prosecutioncasefile.prosecution-case-unsupported";
    public static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED = "public.progression.court-application-summons-rejected";
    public static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED = "public.progression.court-application-summons-approved";
    public static final String PUBLIC_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    public static final String EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS = "prosecutioncasefile.events.case-received-with-duplicate-defendants";

    public static final String EXTERNAL_COMMAND_SJP_CREATE_CASE = "sjp.create-sjp-case";
    public static final String EXTERNAL_EVENT_SJP_CASE_CREATED = "public.sjp.sjp-case-created";
    public static final String EXTERNAL_EVENT_SJP_CASE_DOCUMENT_UPLOADED = "public.sjp.case-document-uploaded";
    public static final String EVENT_SELECTOR_UPLOAD_CASE_DOCUMENT_RECORDED = "prosecutioncasefile.events.upload-case-document-recorded";
    public static final String PUBLIC_CASE_REFERRED_TO_COURT_EVENT = "public.resulting.decision-to-refer-case-for-court-hearing-saved";
    public static final String PUBLIC_SJP_CASE_ASSIGNED_EVENT = "public.sjp.case-assigned";
    public static final String PUBLIC_SJP_CASE_UNASSIGNED_EVENT = "public.sjp.case-unassigned";

    public static final String PUBLIC_PROGRESSION_CASE_CREATED_EVENT = "public.progression.prosecution-case-created";

    public static final String PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED = "public.stagingprosecutors.cps-serve-pet-received";
    public static final String PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED = "public.stagingprosecutors.cps-serve-bcm-received";
    public static final String PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED = "public.stagingprosecutors.cps-serve-cotr-received";
    public static final String PUBLIC_STAGING_PROSECUTORS_CPS_UPDATE_COTR_RECEIVED = "public.stagingprosecutors.cps-update-cotr-received";

    public static final String EVENT_SELECTOR_CC_PROSECUTION_RECEIVED = "prosecutioncasefile.events.cc-case-received";

    public static final String EVENT_SELECTOR_RESOLVED_CASE = "prosecutioncasefile.event.resolved-case";

    public static final String EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS = "prosecutioncasefile.events.cc-case-received-with-warnings";
    public static final String EVENT_SELECTOR_CASE_VALIDATION_FAILED = "prosecutioncasefile.events.case-validation-failed";
    public static final String EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED = "prosecutioncasefile.events.defendant-validation-failed";
    public static final String EVENT_SELECTOR_SJP_VALIDATION_FAILED = "prosecutioncasefile.events.sjp-validation-failed";
    public static final String EVENT_SELECTOR_DEFENDANT_VALIDATION_PASSED = "prosecutioncasefile.events.defendant-validation-passed";

    public static final String EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY = "prosecutioncasefile.events.case-created-successfully";
    public static final String EVENT_SELECTOR_INITIATE_APPLICATION_ACCEPTED = "prosecutioncasefile.events.submit-application-accepted";
    public static final String PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_VALIDATION_FAILED = "prosecutioncasefile.events.submit-application-validation-failed";

    public static final String EVENT_SELECTOR_CPS_SERVE_PET_RECEIVED = "prosecutioncasefile.events.received-cps-serve-pet-processed";
    public static final String EVENT_SELECTOR_CPS_SERVE_BCM_RECEIVED = "prosecutioncasefile.events.received-cps-serve-bcm-processed";
    public static final String EVENT_SELECTOR_CPS_SERVE_COTR_RECEIVED = "prosecutioncasefile.events.received-cps-serve-cotr-processed";
    public static final String EVENT_SELECTOR_CPS_UPDATE_COTR_RECEIVED = "prosecutioncasefile.events.received-cps-update-cotr-processed";


    public static final String EVENT_SELECTOR_CPS_SERVE_PTPH_RECEIVED = "prosecutioncasefile.events.received-cps-serve-ptph-processed";

    public static final String PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_SUBMITTED = "prosecutioncasefile.events.online-plea-submitted";
    public static final String PROSECUTIONCASEFILE_EVENTS_ONLINE_PLEA_PCQ_VISITED_SUBMITTED = "prosecutioncasefile.events.online-plea-pcq-visited-submitted";
    public static final String EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED = "prosecutioncasefile.events.idpc-material-received";
    public static final String EXTERNAL_EVENT_MATERIAL_ADDED = "material.material-added";
    public static final String EVENT_SELECTOR_CASE_DOCUMENT_ADDED = "prosecutioncasefile.events.defendant-idpc-added";
    public static final String EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING = "prosecutioncasefile.events.idpc-defendant-match-pending";
    public static final String EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED = "prosecutioncasefile.events.idpc-defendant-matched";
    public static final String EVENT_SELECTOR_DEFENDANT_IDPC_ALREADY_EXITS = "prosecutioncasefile.events.defendant-idpc-already-exists";
    public static final String EVENT_SELECTOR_DEFENDANT_ADDED = "prosecutioncasefile.events.prosecution-defendants-added";

    public static final String PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH = "prosecutioncasefile.handler.case-updated-initiate-idpc-match";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED_WITH_WARNINGS = "public.prosecutioncasefile.cc-case-received-with-warnings";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED = "public.prosecutioncasefile.cc-case-received";
    public static final String PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED = "public.prosecutioncasefile.manual-case-received";
    public static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED = "public.prosecutioncasefile.prosecution-defendants-added";
    public static final String PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_IDPC_ADDED = "public.prosecutioncasefile.defendant-idpc-added";

    public static final String PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED = "public.prosecutioncasefile.events.sjp-validation-failed";
    public static final String PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED = "public.prosecutioncasefile.events.case-validation-failed";
    public static final String PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED = "public.prosecutioncasefile.events.defendant-validation-failed";

    public static final String EVENT_CASE_VALIDATION_COMPLETED = "prosecutioncasefile.events.validation-completed";

    public static final String PUBLIC_BULK_SCAN_MATERIAL_REJECTED_EVENT = "public.prosecutioncasefile.bulkscan-material-followup";

    public static final String PUBLIC_CASE_IS_EJECTED = "public.progression.events.case-or-application-ejected";

    public static final String PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PTPH_RECEIVED = "public.stagingprosecutors.cps-serve-ptph-received";
    public static final String PUBLIC_GROUP_PROSECUTION_REJECTED_EVENT = "public.prosecutioncasefile.group-prosecution-rejected";


}
