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
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.sirius.components.core.api.ErrorPayload;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.graphql.api.UploadFile;
import org.eclipse.sirius.web.application.project.dto.DuplicateProjectSuccessPayload;
import org.eclipse.sirius.web.application.project.dto.ImportProjectContentBuilder;
import org.eclipse.sirius.web.application.project.dto.ProjectImportContent;
import org.eclipse.sirius.web.application.project.dto.UploadProjectContentInput;
import org.eclipse.sirius.web.application.project.dto.UploadProjectInput;
import org.eclipse.sirius.web.application.project.services.api.IProjectDuplicateService;
import org.eclipse.sirius.web.application.project.services.api.IProjectExportService;
import org.eclipse.sirius.web.application.project.services.api.IProjectMapper;
import org.eclipse.sirius.web.domain.boundedcontexts.project.Project;
import org.eclipse.sirius.web.domain.boundedcontexts.project.services.api.IProjectCreationService;
import org.eclipse.sirius.web.domain.services.IResult;
import org.eclipse.sirius.web.domain.services.Success;
import org.springframework.stereotype.Service;

/**
 * Service used to duplicate a project.
 *
 * @author Arthur Daussy
 */
@Service
public class DuplicateProjectService implements IProjectDuplicateService {

    private final IProjectExportService exportService;

    private final ImportProjectContentBuilder importProjectContentBuilder;

    private final IProjectCreationService projectCreationService;

    private final IProjectMapper projectMapper;

    public DuplicateProjectService(IProjectExportService exportService, ImportProjectContentBuilder importProjectContentBuilder, IProjectCreationService projectCreationService,
            IProjectMapper projectMapper) {
        this.exportService = Objects.requireNonNull(exportService);
        this.importProjectContentBuilder = Objects.requireNonNull(importProjectContentBuilder);
        this.projectCreationService = Objects.requireNonNull(projectCreationService);
        this.projectMapper = Objects.requireNonNull(projectMapper);
    }

    @Override
    public IPayload duplicateProject(UUID inputId, Project project) {

        byte[] content = exportService.export(project);

        UploadFile zipFile = new UploadFile(project.getName() + ".zip", new ByteArrayInputStream(content));

        IPayload payload = new ErrorPayload(inputId, "");
        try {
            ProjectImportContent projectStructure = importProjectContentBuilder.buildFromZip(zipFile.getInputStream());

            UploadProjectContentInput uploadContentInput = new UploadProjectContentInput(inputId, new UploadProjectInput(inputId, zipFile), projectStructure);

            IResult<Project> result = this.projectCreationService.createProject(uploadContentInput, projectStructure.getName() + " - Copy", projectStructure.getNatures());
            if (result instanceof Success<Project> success) {
                payload = new DuplicateProjectSuccessPayload(inputId, projectMapper.toDTO(success.data()));

            }
        } catch (IOException e) {
            payload = new ErrorPayload(inputId, "Unable to import project: " + e.getMessage());
        }

        return payload;

    }


}
