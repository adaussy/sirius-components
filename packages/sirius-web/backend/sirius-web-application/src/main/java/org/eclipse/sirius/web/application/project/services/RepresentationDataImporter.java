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
import org.eclipse.sirius.web.application.document.dto.UploadDocumentsInput;
import org.eclipse.sirius.web.application.project.dto.UploadProjectContentInput;
import org.eclipse.sirius.web.application.project.services.api.IProjectImportParticipant;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.events.SemanticDataUpdatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class RepresentationDataImporter {

    private final List<IProjectImportParticipant> importParticipants;

    public RepresentationDataImporter(List<IProjectImportParticipant> importParticipants) {
        this.importParticipants = importParticipants;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSemanticDataCreated(SemanticDataUpdatedEvent event) {
        if (event.causedBy() instanceof UploadDocumentsInput uploadDocumentsInput && uploadDocumentsInput.causeBy() instanceof UploadProjectContentInput uploadProjectContentInput){
            importParticipants.stream()
                    .filter(p -> p.canHandle(event,uploadProjectContentInput.projectStructure()))
                    .forEach(p -> p.handle(event,uploadProjectContentInput.projectStructure()));

        }
    }
}
