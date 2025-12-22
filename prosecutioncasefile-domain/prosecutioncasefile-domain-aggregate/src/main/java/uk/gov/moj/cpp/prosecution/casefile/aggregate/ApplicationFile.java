package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Objects.nonNull;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.prosecution.casefile.CaseType.UNKNOWN;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentAdded;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.prosecution.casefile.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AddMaterialSubmissionV2;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.common.AddMaterialCommonV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedWithWarnings;

public class ApplicationFile implements Aggregate {

    private static final long serialVersionUID = -4962958071633403582L;

    private final List<MaterialAddedV2> pendingMaterialsForCourtDocumentUpload = new ArrayList<>();
    private final List<MaterialAddedWithWarnings> pendingMaterialsWithWarningsForCourtDocumentUpload = new ArrayList<>();

    private static final CaseType caseType = UNKNOWN;

    public Stream<Object> addMaterialV2(final AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService, final ProgressionService progressionService) {
        final Object event = validateMaterialV2(addMaterialCommonV2, referenceDataQueryService, progressionService);

        return apply(of(event));
    }

    @Override
    @SuppressWarnings("squid:S2250")
    public Object apply(final Object event) {
        return match(event).with(
                when(MaterialAddedV2.class).apply(pendingMaterialsForCourtDocumentUpload::add),
                when(MaterialAddedWithWarnings.class).apply(pendingMaterialsWithWarningsForCourtDocumentUpload::add),
                when(CourtDocumentAdded.class).apply(e -> {
                    pendingMaterialsForCourtDocumentUpload
                            .removeIf(materialAddedV2 -> materialAddedV2.getMaterial().toString().equals(e.getFileStoreId()));
                    pendingMaterialsWithWarningsForCourtDocumentUpload
                            .removeIf(materialAddedWithWarnings -> materialAddedWithWarnings.getMaterial().toString().equals(e.getFileStoreId()));
                }),
                otherwiseDoNothing());
    }

    private Object validateMaterialV2(AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService, final ProgressionService progressionService) {
        return validateMaterialWithDocumentDetailsV2(addMaterialCommonV2, referenceDataQueryService, progressionService);
    }


    private Object validateMaterialWithDocumentDetailsV2(AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService, final ProgressionService progressionService) {

        CaseDocumentWithReferenceData caseDocumentWithReferenceData;

       caseDocumentWithReferenceData = new CaseDocumentWithReferenceData(null, false, null, addMaterialCommonV2.getMaterialType(), false, false, addMaterialCommonV2.getCourtApplicationSubject(), addMaterialCommonV2.getProsecutionCaseSubject(), addMaterialCommonV2.getMaterialType(), addMaterialCommonV2.getMaterialContentType(), null);
        if(nonNull(addMaterialCommonV2.getCourtApplicationSubject())) {
            final CourtApplication courtApplication = progressionService.getApplicationOnly(addMaterialCommonV2.getCourtApplicationSubject().getCourtApplicationId());
            caseDocumentWithReferenceData.setHasApplication(courtApplication != null && courtApplication.getType() != null);
        }

        return ProsecutionCaseFileHelper.validateMaterialWithDocumentDetailsV2(addMaterialCommonV2, referenceDataQueryService, caseDocumentWithReferenceData, caseType, null);
    }

    public Stream<Object> addCourtDocument(final CourtDocument courtDocument, final UUID materialId, final String fileStoreId) {
        final Builder<Object> builder = builder();
        pendingMaterialsForCourtDocumentUpload.stream()
                .filter(materialAddedV2 -> materialAddedV2.getMaterial().toString().equals(fileStoreId))
                .findFirst()
                .ifPresent(materialAddedV2 -> builder.add(getCourtDocuments(courtDocument, materialId, fileStoreId, materialAddedV2)));
        pendingMaterialsWithWarningsForCourtDocumentUpload.stream()
                .filter(materialAddedWithWarnings -> materialAddedWithWarnings.getMaterial().toString().equals(fileStoreId))
                .findFirst()
                .ifPresent(materialAddedWithWarnings -> builder.add(getCourtDocuments(courtDocument, materialId, fileStoreId, materialAddedWithWarnings)));
        return apply(builder.build());
    }

    public CourtDocumentAdded getCourtDocuments(final CourtDocument courtDocument, final UUID materialId, final String fileStoreId, MaterialAddedV2 materialAddedV2) {
        final AddMaterialSubmissionV2 addMaterialSubmissionV2 = AddMaterialSubmissionV2.addMaterialSubmissionV2()
                .withCaseId(materialAddedV2.getCaseId())
                .withCaseType(materialAddedV2.getCaseType())
                .withIsCpsCase(materialAddedV2.getIsCpsCase())
                .withExhibit(materialAddedV2.getExhibit())
                .withMaterial(materialId)
                .withWitnessStatement(materialAddedV2.getWitnessStatement())
                .withCourtApplicationSubject(materialAddedV2.getCourtApplicationSubject())
                .withProsecutionCaseSubject(materialAddedV2.getProsecutionCaseSubject())
                .withCaseSubFolderName(materialAddedV2.getCaseSubFolderName())
                .withFileName(materialAddedV2.getFileName())
                .withMaterialName(materialAddedV2.getMaterialName())
                .withMaterialType(materialAddedV2.getMaterialType())
                .withMaterialContentType(materialAddedV2.getMaterialContentType())
                .withSectionOrderSequence(materialAddedV2.getSectionOrderSequence())
                .withTag(materialAddedV2.getTag())
                .withDefendantId(materialAddedV2.getDefendantId())
                .build();

        return CourtDocumentAdded.courtDocumentAdded()
                .withCourtDocument(courtDocument)
                .withFileStoreId(fileStoreId)
                .withIsUnbundledDocument(false)
                .withMaterialId(materialId)
                .withMaterialSubmittedV2(addMaterialSubmissionV2)
                .build();
    }

    public CourtDocumentAdded getCourtDocuments(final CourtDocument courtDocument, final UUID materialId, final String fileStoreId, MaterialAddedWithWarnings materialAddedWithWarnings) {
        final AddMaterialSubmissionV2 addMaterialSubmissionV2 = AddMaterialSubmissionV2.addMaterialSubmissionV2()
                .withCaseId(materialAddedWithWarnings.getCaseId())
                .withCaseType(materialAddedWithWarnings.getCaseType())
                .withIsCpsCase(materialAddedWithWarnings.getIsCpsCase())
                .withExhibit(materialAddedWithWarnings.getExhibit())
                .withMaterial(materialId)
                .withWitnessStatement(materialAddedWithWarnings.getWitnessStatement())
                .withCourtApplicationSubject(materialAddedWithWarnings.getCourtApplicationSubject())
                .withProsecutionCaseSubject(materialAddedWithWarnings.getProsecutionCaseSubject())
                .withCaseSubFolderName(materialAddedWithWarnings.getCaseSubFolderName())
                .withFileName(materialAddedWithWarnings.getFileName())
                .withMaterialName(materialAddedWithWarnings.getMaterialName())
                .withMaterialType(materialAddedWithWarnings.getMaterialType())
                .withMaterialContentType(materialAddedWithWarnings.getMaterialContentType())
                .withSectionOrderSequence(materialAddedWithWarnings.getSectionOrderSequence())
                .withTag(materialAddedWithWarnings.getTag())
                .withDefendantId(materialAddedWithWarnings.getDefendantId())
                .build();

        return CourtDocumentAdded.courtDocumentAdded()
                .withCourtDocument(courtDocument)
                .withFileStoreId(fileStoreId)
                .withIsUnbundledDocument(false)
                .withMaterialId(materialId)
                .withMaterialSubmittedV2(addMaterialSubmissionV2)
                .build();
    }
}