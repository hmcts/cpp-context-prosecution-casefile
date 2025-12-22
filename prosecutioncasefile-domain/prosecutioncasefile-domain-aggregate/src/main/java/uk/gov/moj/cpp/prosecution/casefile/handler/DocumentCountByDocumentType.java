package uk.gov.moj.cpp.prosecution.casefile.handler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentCountByDocumentType implements Serializable {

    private static final long serialVersionUID = 6085387207075980216L;

    private Map<String, Integer> documentCount = new HashMap<>();

    public Integer getCount(String documentType) {
        if (documentType == null) {
            return 0;
        }
        final Integer numberOfDocuments = documentCount.get(normalise(documentType));
        return numberOfDocuments != null ? numberOfDocuments : 0;
    }

    private String normalise(String documentType) {
        return documentType.replaceAll("\\s", "").toLowerCase();
    }
}
