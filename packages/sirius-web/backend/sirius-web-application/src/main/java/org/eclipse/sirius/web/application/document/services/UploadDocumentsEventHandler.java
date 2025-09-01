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
package org.eclipse.sirius.web.application.document.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.sirius.components.collaborative.api.ChangeDescription;
import org.eclipse.sirius.components.collaborative.api.ChangeKind;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventHandler;
import org.eclipse.sirius.components.collaborative.api.Monitoring;
import org.eclipse.sirius.components.core.api.ErrorPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IEditingContextSearchService;
import org.eclipse.sirius.components.core.api.IInput;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.emf.ResourceMetadataAdapter;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.components.graphql.api.UploadFile;
import org.eclipse.sirius.components.graphql.api.UploadFile2;
import org.eclipse.sirius.web.application.UUIDParser;
import org.eclipse.sirius.web.application.document.dto.DocumentDTO;
import org.eclipse.sirius.web.application.document.dto.UploadDocumentsInput;
import org.eclipse.sirius.web.application.document.dto.UploadDocumentsSuccessPayload;
import org.eclipse.sirius.web.application.document.dto.UploadedDocumentDTO;
import org.eclipse.sirius.web.application.document.services.api.IUploadDocumentReportProvider;
import org.eclipse.sirius.web.application.document.services.api.IUploadFileLoader;
import org.eclipse.sirius.web.application.document.services.api.UploadedResource;
import org.eclipse.sirius.web.application.project.dto.UploadProjectContentInput;
import org.eclipse.sirius.web.application.views.explorer.services.ExplorerDescriptionProvider;
import org.eclipse.sirius.web.domain.services.Failure;
import org.eclipse.sirius.web.domain.services.IResult;
import org.eclipse.sirius.web.domain.services.Success;
import org.eclipse.sirius.web.domain.services.api.IMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

/**
 * Event handler used to create a new documents while uploading multiple file at once multiple file upload.
 *
 * @author Arthur Daussy
 * @technical-debt Event if the handler can upload multiple file at once, the current implementation requires that all files are self-sufficient, meaning they do not have proxy to external
 *         file (even the ones being uploaded simultaneously)
 */
@Service
public class UploadDocumentsEventHandler implements IEditingContextEventHandler {

    private final IEditingContextSearchService editingContextSearchService;

    private final List<IUploadDocumentReportProvider> uploadDocumentReportProviders;

    private final IMessageService messageService;

    private final IUploadFileLoader uploadDocumentLoader;

    private final Counter counter;

    private final boolean reuseActiveResourceSet;

    public UploadDocumentsEventHandler(IEditingContextSearchService editingContextSearchService, List<IUploadDocumentReportProvider> uploadDocumentReportProviders, IMessageService messageService,
            IUploadFileLoader uploadDocumentLoader, MeterRegistry meterRegistry, @Value("${sirius.web.upload.reuseActiveResourceSet:true}") boolean reuseActiveResourceSet) {
        this.editingContextSearchService = Objects.requireNonNull(editingContextSearchService);
        this.uploadDocumentReportProviders = Objects.requireNonNull(uploadDocumentReportProviders);
        this.messageService = Objects.requireNonNull(messageService);
        this.uploadDocumentLoader = Objects.requireNonNull(uploadDocumentLoader);
        this.counter = Counter.builder(Monitoring.EVENT_HANDLER).tag(Monitoring.NAME, this.getClass().getSimpleName()).register(meterRegistry);
        this.reuseActiveResourceSet = reuseActiveResourceSet;
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IInput input) {
        return input instanceof UploadDocumentsInput;
    }

    @Override
    public void handle(Sinks.One<IPayload> payloadSink, Sinks.Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, IInput input) {
        this.counter.increment();

        IPayload payload = new ErrorPayload(input.id(), this.messageService.unexpectedError());
        ChangeDescription changeDescription = new ChangeDescription(ChangeKind.NOTHING, editingContext.getId(), input);

        Optional<ResourceSet> optionalResourceSet;
        if (this.reuseActiveResourceSet && editingContext instanceof IEMFEditingContext emfEditingContext) {
            optionalResourceSet = Optional.of(emfEditingContext.getDomain().getResourceSet());
        } else {
            optionalResourceSet = this.createResourceSet(editingContext.getId());
        }
        if (input instanceof UploadDocumentsInput uploadDocumentsInput && editingContext instanceof IEMFEditingContext emfEditingContext && optionalResourceSet.isPresent()) {
            var resourceSet = optionalResourceSet.get();

            List<UploadedDocumentDTO> documents = new ArrayList<>();
            List<String> errorsMsgs = new ArrayList<>();

            for (UploadFile2 uploadFile : uploadDocumentsInput.files()) {

                IResult<UploadedResource> result = this.uploadDocumentLoader.load(resourceSet, emfEditingContext, new UploadFile(uploadFile.getName(), uploadFile.getInputStream()));
                if (result instanceof Success<UploadedResource> success) {
                    var newResource = success.data().resource();

                    var optionalId = new UUIDParser().parse(newResource.getURI().path().substring(1));

                    var optionalName = newResource.eAdapters().stream().filter(ResourceMetadataAdapter.class::isInstance).map(ResourceMetadataAdapter.class::cast).findFirst()
                            .map(ResourceMetadataAdapter::getName);

                    if (optionalId.isPresent() && optionalName.isPresent()) {
                        var id = optionalId.get();
                        var name = optionalName.get();
                        documents.add(new UploadedDocumentDTO(new DocumentDTO(id, name, ExplorerDescriptionProvider.DOCUMENT_KIND), this.getReport(newResource), success.data().idMapping()));
                        if (((UploadDocumentsInput) input).causeBy() instanceof UploadProjectContentInput uploadProjectContentInput) {
                            uploadProjectContentInput.projectStructure().getSemanticElementsIdMappings().putAll(success.data().idMapping());
                            uploadProjectContentInput.projectStructure().getDocumentIdMapping().put(uploadFile.getId(), id);
                        }
                    }

                } else if (result instanceof Failure<UploadedResource> failure) {
                    String errorMessage = failure.message();
                    if (errorMessage == null) {
                        errorMessage = "Fail to import " + uploadFile.getName();
                    }
                    errorsMsgs.add(errorMessage);

                }
            }

            if (!errorsMsgs.isEmpty()) {
                payload = new ErrorPayload(input.id(), errorsMsgs.stream().collect(Collectors.joining(System.lineSeparator())));
            } else {
                payload = new UploadDocumentsSuccessPayload(input.id(), documents);
                // we need to wrap the input to keep tracks of the all ids
                changeDescription = new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, editingContext.getId(), input);

            }
        }

        payloadSink.tryEmitValue(payload);
        changeDescriptionSink.tryEmitNext(changeDescription);
    }

    private Optional<ResourceSet> createResourceSet(String editingContextId) {
        return this.editingContextSearchService.findById(editingContextId).filter(IEMFEditingContext.class::isInstance).map(IEMFEditingContext.class::cast).map(IEMFEditingContext::getDomain)
                .map(AdapterFactoryEditingDomain::getResourceSet);
    }

    private String getReport(Resource resource) {
        return this.uploadDocumentReportProviders.stream().filter(provider -> provider.canHandle(resource)).map(provider -> provider.createReport(resource))
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
