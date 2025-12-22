package uk.gov.moj.cpp.prosecution.casefile.command.handler.util;


import static java.util.UUID.randomUUID;

import java.util.UUID;

public class DefaultTestData {

    public static final UUID CASE_ID = randomUUID();
    public static final String CASE_ID_STR = CASE_ID.toString();
    public static final UUID CASE_DOCUMENT_ID = randomUUID();
    public static final String CASE_DOCUMENT_ID_STR = CASE_DOCUMENT_ID.toString();
    public static final UUID CASE_DOCUMENT_MATERIAL_ID = randomUUID();
    public static final String CASE_DOCUMENT_MATERIAL_ID_STR = CASE_DOCUMENT_MATERIAL_ID.toString();
    public static final String CASE_DOCUMENT_TYPE_IDPC = "IDPC";
    public static final UUID DEFENDANT_ID = randomUUID();
}
