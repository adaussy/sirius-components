/*******************************************************************************
 * Copyright (c) 2025 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.application.project.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventProcessor;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventProcessorRegistry;
import org.eclipse.sirius.components.graphql.api.UploadFile2;
import org.eclipse.sirius.web.application.document.dto.UploadDocumentsInput;
import org.eclipse.sirius.web.application.document.dto.UploadDocumentsSuccessPayload;
import org.eclipse.sirius.web.application.project.dto.IProjectImportContent;
import org.eclipse.sirius.web.application.project.dto.UploadProjectContentInput;
import org.eclipse.sirius.web.application.project.services.api.IProjectImportParticipant;
import org.eclipse.sirius.web.domain.boundedcontexts.project.events.ProjectCreatedEvent;
import org.eclipse.sirius.web.domain.boundedcontexts.projectsemanticdata.ProjectSemanticData;
import org.eclipse.sirius.web.domain.boundedcontexts.projectsemanticdata.services.api.IProjectSemanticDataSearchService;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.events.SemanticDataCreatedEvent;
import org.eclipse.sirius.web.domain.events.IDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;

@Service
public class SemanticProjectImportParticipant implements IProjectImportParticipant {

    private final Logger logger = LoggerFactory.getLogger(SemanticProjectImportParticipant.class);

    private static final String DOCUMENTS_FOLDER = "documents";

    private static final String ZIP_FOLDER_SEPARATOR = "/";

    private final IProjectSemanticDataSearchService projectSemanticDataSearchService;

    private final IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry;

    public SemanticProjectImportParticipant(IProjectSemanticDataSearchService projectSemanticDataSearchService, IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry) {
        this.projectSemanticDataSearchService = projectSemanticDataSearchService;
        this.editingContextEventProcessorRegistry = editingContextEventProcessorRegistry;
    }

    @Override
    public boolean canHandle(IDomainEvent event, IProjectImportContent projectUpdateContent) {
        return event instanceof SemanticDataCreatedEvent semDataEvent && semDataEvent.causedBy() instanceof ProjectCreatedEvent projectCreatedEvent
                && projectCreatedEvent.causedBy() instanceof UploadProjectContentInput;
    }

    @Override
    public void handle(IDomainEvent event, IProjectImportContent projectUploadContent) {
        SemanticDataCreatedEvent semCreationEvent = (SemanticDataCreatedEvent) event;
        Map<String, Object> projectManifest = projectUploadContent.getManifest();
        UploadProjectContentInput originalInput = (UploadProjectContentInput)event.causedBy().causedBy();

        var optionalEditingContextId = this.projectSemanticDataSearchService.findBySemanticDataId(AggregateReference.to(semCreationEvent.semanticData().getId()))
                .map(ProjectSemanticData::getSemanticData).map(AggregateReference::getId).map(UUID::toString);

        optionalEditingContextId.flatMap(this.editingContextEventProcessorRegistry::getOrCreateEditingContextEventProcessor)
                .ifPresent(editingContextEventProcessor -> createDocuments(event.id(), projectUploadContent, editingContextEventProcessor,originalInput));

    }

    private List<UploadFile2> getDocuments(IProjectImportContent projectUploadContent) {
        String documentsFolderInZip = projectUploadContent.getName() + ZIP_FOLDER_SEPARATOR + DOCUMENTS_FOLDER + ZIP_FOLDER_SEPARATOR;
        Map<String, ByteArrayOutputStream> documentIdToDocumentContent = this.selectAndTransformIntoDocumentIdToDocumentContent(projectUploadContent.getFileContent(), documentsFolderInZip);
        List<UploadFile2> documents = new ArrayList<>();
        for (Map.Entry<String, ByteArrayOutputStream> entry : documentIdToDocumentContent.entrySet()) {
            String documentId = entry.getKey();
            ByteArrayOutputStream outputStream = entry.getValue();
            String documentName = null;
            Object documentIdsToName = projectUploadContent.getManifest().get("documentIdsToName");
            if (documentIdsToName instanceof Map) {
                documentName = (String) ((Map<?, ?>) documentIdsToName).get(documentId);
            }
            documents.add(new UploadFile2(documentId,documentName, new ByteArrayInputStream(outputStream.toByteArray())));
        }
        return documents;
    }

    /**
     * Selects and transforms the given map of zip entry name to zip entry content into a map of document id to document content.
     *
     * <p>
     * Select entries in the given map where the key represented by the zip entry name are in the document folder (start with documentsFolderInZip). For each remaining entries extract the document id
     * by removing documentsFolderInZip from the zip entry name.
     * </p>
     *
     * @param zipEntryNameToContent
     *         The map of all zip entries to their content
     * @param documentsFolderInZip
     *         The path of documents folder in zip
     * @return The map of document id to document content
     */
    private Map<String, ByteArrayOutputStream> selectAndTransformIntoDocumentIdToDocumentContent(Map<String, ByteArrayOutputStream> zipEntryNameToContent, String documentsFolderInZip) {
        Function<Map.Entry<String, ByteArrayOutputStream>, String> mapZipEntryNameToDocumentId = e -> {
            String fullPath = e.getKey();
            String fileName = fullPath.substring(documentsFolderInZip.length());
            int extensionIndex = fileName.lastIndexOf('.');
            if (extensionIndex >= 0) {
                return fileName.substring(0, extensionIndex);
            } else {
                return fileName;
            }
        };

        return zipEntryNameToContent.entrySet().stream().filter(entry -> entry.getKey().startsWith(documentsFolderInZip))
                .collect(Collectors.toMap(mapZipEntryNameToDocumentId::apply, Map.Entry::getValue));
    }

    /**
     * Creates all documents in the project thanks to the {@link IEditingContextEventProcessor}. If at least one document has not been created it will return <code>false</code>.
     *
     * @param inputId
     * @param originalInput
     * @return <code>true</code> whether all documents has been created, <code>false</code> otherwise
     */
    private void createDocuments(UUID inputId, IProjectImportContent projectUploadContent, IEditingContextEventProcessor editingContextEventProcessor, UploadProjectContentInput originalInput) {
        /*List<UploadFile2> documentsToUpload = new ArrayList<>();
        List<String> oldDocumentIds = new ArrayList<>();
        for (Map.Entry<String, UploadFile> entry : getDocuments(projectUploadContent).entrySet()) {
            String oldDocumentId = entry.getKey();
            oldDocumentIds.add(oldDocumentId);
            UploadFile uploadFile = entry.getValue();
            documentsToUpload.add(uploadFile);

        }*/
var documentsToUpload = getDocuments(projectUploadContent);
        if (!documentsToUpload.isEmpty()) {
            UploadDocumentsInput input = new UploadDocumentsInput(inputId,originalInput, editingContextEventProcessor.getEditingContextId(), documentsToUpload);

            var optionalSuccess = editingContextEventProcessor.handle(input)
                    .filter(UploadDocumentsSuccessPayload.class::isInstance)
                    .map(UploadDocumentsSuccessPayload.class::cast)
                    .blockOptional();
            /*if (optionalSuccess.isPresent()) {
                List<UploadedDocumentDTO> uploadedDocuments = optionalSuccess.get().uploadedDocuments();
                for (int index = 0; index < uploadedDocuments.size(); index++) {
                    UploadedDocumentDTO uploadDocument = uploadedDocuments.get(index);
                    UUID newDocumentId = uploadDocument.document().id();
                    Map<String, String> idMapping = uploadDocument.idMapping();
                    String oldDocumentId = oldDocumentIds.get(index);
                    projectUploadContent.getDocumentIdMapping().put(oldDocumentId, newDocumentId);
                    projectUploadContent.getSemanticElementsIdMappings().putAll(idMapping);
                }
            }*/
        }

        Map<String, String> documentIds = new HashMap<>();
        for (Map.Entry<String, UUID> entry : projectUploadContent.getDocumentIdMapping().entrySet()) {
            documentIds.put(entry.getKey(), entry.getValue().toString());
        }
        RewriteProxiesInput rewriteInput = new RewriteProxiesInput(UUID.randomUUID(), editingContextEventProcessor.getEditingContextId(), documentIds);
        editingContextEventProcessor.handle(rewriteInput).blockOptional();

    }
}
