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

import java.util.List;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventProcessorRegistry;
import org.eclipse.sirius.web.application.project.dto.UploadProjectContentInput;
import org.eclipse.sirius.web.application.project.services.api.IProjectImportParticipant;
import org.eclipse.sirius.web.domain.boundedcontexts.project.events.ProjectCreatedEvent;
import org.eclipse.sirius.web.domain.boundedcontexts.projectsemanticdata.services.api.IProjectSemanticDataSearchService;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.events.SemanticDataCreatedEvent;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.repositories.ISemanticDataRepository;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.services.api.ISemanticDataSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class SemanticDataImporter {

    private final Logger logger = LoggerFactory.getLogger(SemanticDataImporter.class);

    private static final String ZIP_FOLDER_SEPARATOR = "/";

    private final IProjectSemanticDataSearchService projectSemanticDataSearchService;

    private final IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry;

    private final ISemanticDataSearchService semanticDataSearchService;

    private final ISemanticDataRepository semanticDataRepository;

    private final List<IProjectImportParticipant> importParticipants;

    public SemanticDataImporter(IProjectSemanticDataSearchService projectSemanticDataSearchService, IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry,
            ISemanticDataSearchService semanticDataSearchService, ISemanticDataRepository semanticDataRepository, List<IProjectImportParticipant> importParticipants) {
        this.projectSemanticDataSearchService = projectSemanticDataSearchService;
        this.editingContextEventProcessorRegistry = editingContextEventProcessorRegistry;
        this.semanticDataSearchService = semanticDataSearchService;
        this.semanticDataRepository = semanticDataRepository;
        this.importParticipants = importParticipants;
    }


    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSemanticDataCreated(SemanticDataCreatedEvent event) {
        if (event.causedBy() instanceof ProjectCreatedEvent projectCreatedEvent && projectCreatedEvent.causedBy() instanceof UploadProjectContentInput uploadProjectContentInput){
            importParticipants.stream()
                    .filter(p -> p.canHandle(event,uploadProjectContentInput.projectStructure()))
                    .forEach(p -> p.handle(event,uploadProjectContentInput.projectStructure()));

        }
    }






}
