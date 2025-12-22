package uk.gov.moj.cpp.prosecution.casefile.domain;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;

public class DomainConstants {

    private static final List<String> LIST_OF_ALLOWED_DOCUMENTTYPE = asList("APPLICATION", "CORRESPONDENCE", "PLEA");
    public static final String PROBLEM_CODE_DOCUMENT_NOT_MATCHED = "DOCUMENT_NOT_MATCHED_TO_CASE";
    public static final String SOURCE_CPS_FOR_PUBLIC_EVENTS = "CPS";

    private DomainConstants() {
    }

    public static List<String> getListOfAllowedDocumentTypes(){
        return Collections.unmodifiableList(LIST_OF_ALLOWED_DOCUMENTTYPE);
    }
}
