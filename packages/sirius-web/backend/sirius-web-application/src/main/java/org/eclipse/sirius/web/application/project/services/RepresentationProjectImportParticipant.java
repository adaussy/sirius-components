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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventProcessor;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventProcessorRegistry;
import org.eclipse.sirius.components.collaborative.dto.CreateRepresentationInput;
import org.eclipse.sirius.components.collaborative.dto.CreateRepresentationSuccessPayload;
import org.eclipse.sirius.web.application.document.dto.UploadDocumentsInput;
import org.eclipse.sirius.web.application.project.dto.IProjectImportContent;
import org.eclipse.sirius.web.application.project.dto.UploadProjectContentInput;
import org.eclipse.sirius.web.application.project.services.api.IProjectImportParticipant;
import org.eclipse.sirius.web.application.project.services.api.IRepresentationImporterUpdateService;
import org.eclipse.sirius.web.domain.boundedcontexts.projectsemanticdata.ProjectSemanticData;
import org.eclipse.sirius.web.domain.boundedcontexts.projectsemanticdata.services.api.IProjectSemanticDataSearchService;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.events.SemanticDataUpdatedEvent;
import org.eclipse.sirius.web.domain.events.IDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;

@Service
public class RepresentationProjectImportParticipant implements IProjectImportParticipant {

    private final Logger logger = LoggerFactory.getLogger(RepresentationProjectImportParticipant.class);

    private static final String ZIP_FOLDER_SEPARATOR = "/";

    private static final String REPRESENTATIONS_FOLDER = "representations";

    private final ObjectMapper objectMapper;

    private final List<IRepresentationImporterUpdateService> diagramImporterUpdateServices;
    private final IProjectSemanticDataSearchService projectSemanticDataSearchService;
    private final IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry;
    
    public RepresentationProjectImportParticipant(ObjectMapper objectMapper, List<IRepresentationImporterUpdateService> diagramImporterUpdateServices,
            IProjectSemanticDataSearchService projectSemanticDataSearchService, IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry) {
        this.objectMapper = objectMapper;
        this.diagramImporterUpdateServices = diagramImporterUpdateServices;
        this.projectSemanticDataSearchService = projectSemanticDataSearchService;
        this.editingContextEventProcessorRegistry = editingContextEventProcessorRegistry;
    }

    @Override
    public boolean canHandle(IDomainEvent event, IProjectImportContent projectUpdateContent) {
        return event instanceof SemanticDataUpdatedEvent
                && event.causedBy() instanceof UploadDocumentsInput uploadDocumentsInput
                && uploadDocumentsInput.causeBy() instanceof UploadProjectContentInput;
    }

    @Override
    public void handle(IDomainEvent event, IProjectImportContent projectUpdateContent) {
        SemanticDataUpdatedEvent semanticDataUpdatedEvent = (SemanticDataUpdatedEvent) event;


        var optionalEditingContextId = this.projectSemanticDataSearchService.findBySemanticDataId(AggregateReference.to(semanticDataUpdatedEvent.semanticData().getId()))
                .map(ProjectSemanticData::getSemanticData)
                .map(AggregateReference::getId)
                .map(UUID::toString);

        optionalEditingContextId.flatMap(this.editingContextEventProcessorRegistry::getOrCreateEditingContextEventProcessor)
                .ifPresent(editingContextEventProcessor -> createRepresentations(event.id(), projectUpdateContent, editingContextEventProcessor));

    }

    private List<RepresentationImportData> getRepresentationImportData(IProjectImportContent projectContent) {
        String representationsFolderInZip = projectContent.getName() + ZIP_FOLDER_SEPARATOR + REPRESENTATIONS_FOLDER + ZIP_FOLDER_SEPARATOR;

        List<ByteArrayOutputStream> representationDescritorsContent = this.selectAndTransformIntoRepresentationDescriptorsContent(projectContent.getFileContent(), representationsFolderInZip);

        return this.getRepresentationImportDatas(representationDescritorsContent);
    }

    private List<ByteArrayOutputStream> selectAndTransformIntoRepresentationDescriptorsContent(Map<String, ByteArrayOutputStream> zipEntryNameToContent, String representationsFolderInZip) {
        return zipEntryNameToContent.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(representationsFolderInZip))
                .map(Map.Entry::getValue)
                .toList();
    }

    private List<RepresentationImportData> getRepresentationImportDatas(List<ByteArrayOutputStream> outputStreamToTransformToRepresentationDescriptor) {
        List<RepresentationImportData> representations = new ArrayList<>();
        for (ByteArrayOutputStream outputStream : outputStreamToTransformToRepresentationDescriptor) {
            try {
            byte[] representationDescriptorBytes = outputStream.toByteArray();
            RepresentationSerializedImportData representationSerializedImportData = null;
                representationSerializedImportData = this.objectMapper.readValue(representationDescriptorBytes, RepresentationSerializedImportData.class);
            var representationDescriptor = new RepresentationImportData(representationSerializedImportData.id(), representationSerializedImportData.projectId(),
                    representationSerializedImportData.descriptionId(), representationSerializedImportData.targetObjectId(), representationSerializedImportData.label(),
                    representationSerializedImportData.kind(), representationSerializedImportData.representation());
            representations.add(representationDescriptor);
            } catch (IOException e) {
                logger.warn("Unable to convert one of the given representation : {}", e.getMessage(), e);
            }
        }
        return representations;
    }

    /**
     * Get the representation (type, targetObjectUri, descriptionUri) described into the Manifest from a given
     * representation identifier.
     *
     * @param representationImportData
     *            the representation to look for in Manifest
     * @return the representation details from Manifest
     */
    private Map<?, ?> getRepresentationManifest(RepresentationImportData representationImportData, IProjectImportContent projectContent) {
        Object representationsFromManifest = projectContent.getManifest().get("representations");
        UUID representationId = representationImportData.id();
        if (representationsFromManifest instanceof Map && representationId != null) {
            Object representationFromManifest = ((Map<?, ?>) representationsFromManifest).get(representationImportData.id().toString());
            if (representationFromManifest instanceof Map) {
                return (Map<?, ?>) representationFromManifest;
            }
        }
        return new HashMap<>();
    }

    /**
     * Adapt the targetObjectURI/object id stored in the archive to point to the equivalent object after import. The new
     * object will be in a document with a different id, and will itself have been given a new, unique id during the
     * upload.
     *
     * @param targetObjectURI
     *            the target object URI/id stored in the manifest.
     * @return the URI/id of the equivalent model element in the newly imported documents.
     */
    private String getNewObjectId(String targetObjectURI,IProjectImportContent projectContent) {
        String objectId;

        String oldDocumentId = getNewDocumentId(targetObjectURI);
        UUID newDocumentId = projectContent.getDocumentIdMapping().get(oldDocumentId);
        if (newDocumentId != null) {
            objectId = targetObjectURI.replace(oldDocumentId, newDocumentId.toString());
        } else {
            objectId = targetObjectURI;
        }

        String oldSemantidElementId = URI.create(targetObjectURI).getFragment();
        String newSemanticElementId = projectContent.getSemanticElementsIdMappings().get(oldSemantidElementId);
        if (newSemanticElementId != null) {
            objectId = objectId.replace(oldSemantidElementId, newSemanticElementId);
        }
        return objectId;
    }

    private static String getNewDocumentId(String targetObjectURI) {
        return URI.create(targetObjectURI).getPath().substring(1);
    }

    /**
     * Creates all representations in the project thanks to the {@link IEditingContextEventProcessor} and the create
     * representation input. If at least one representation has not been created it will return <code>false</code>.
     *
     * @param inputId
     *            The identifier of the input which has triggered this import
     * @return <code>true</code> whether all representations has been created, <code>false</code> otherwise
     */
    private boolean createRepresentations(UUID inputId, IProjectImportContent projectContent,IEditingContextEventProcessor editingContextEventProcessor )  {
        boolean allRepresentationCreated = true;

        for (RepresentationImportData representationImportData : getRepresentationImportData(projectContent)) {
            Map<?, ?> representationManifest = this.getRepresentationManifest(representationImportData,projectContent);

            String targetObjectURI = (String) representationManifest.get("targetObjectURI");

            String objectId = this.getNewObjectId(targetObjectURI,projectContent);

            String descriptionURI = (String) representationManifest.get("descriptionURI");

            boolean representationCreated = false;

            CreateRepresentationInput createRepresentationInput = new CreateRepresentationInput(inputId, editingContextEventProcessor.getEditingContextId(), descriptionURI, objectId, representationImportData.label());

            var representationPayloadCreated = editingContextEventProcessor.handle(createRepresentationInput)
                    .filter(CreateRepresentationSuccessPayload.class::isInstance)
                    .map(CreateRepresentationSuccessPayload.class::cast)
                    .blockOptional();

            representationCreated = representationPayloadCreated
                    .map(CreateRepresentationSuccessPayload::representation)
                    .isPresent();

            if (representationPayloadCreated.isPresent()) {
                var newRepresentationId = representationPayloadCreated.get().representation().id();
                var editingContextId = editingContextEventProcessor.getEditingContextId();
                this.diagramImporterUpdateServices.stream()
                        .filter(diagramImporterUpdateService -> diagramImporterUpdateService.canHandle(editingContextId, representationImportData))
                        .forEach(diagramImporterUpdateService -> diagramImporterUpdateService.handle(projectContent.getSemanticElementsIdMappings(), createRepresentationInput, editingContextId, newRepresentationId, representationImportData));
            }

            if (!representationCreated) {
                this.logger.warn("The representation {} has not been created", representationImportData.label());
            }

            allRepresentationCreated = allRepresentationCreated && representationCreated;
        }

        return allRepresentationCreated;
    }

}
