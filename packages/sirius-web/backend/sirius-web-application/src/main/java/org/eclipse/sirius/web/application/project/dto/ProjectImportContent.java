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
package org.eclipse.sirius.web.application.project.dto;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectImportContent implements IProjectImportContent {

    private static final String DOCUMENTS_FOLDER = "documents";

    private static final String ZIP_FOLDER_SEPARATOR = "/";

    private static final String MANIFEST_JSON_FILE = "manifest.json";

    private static final String REPRESENTATIONS_FOLDER = "representations";

    private final Logger logger = LoggerFactory.getLogger(ProjectImportContent.class);

    private final String projectName;

    private final Map<String, ByteArrayOutputStream> fileContent;

    private final Map<String, Object> manifest;

    private final Map<String, UUID> documentIdMapping = new HashMap<>();

    private final Map<String, String> semanticElementsIdMappings = new HashMap<>();

    public ProjectImportContent( String projectName, Map<String, ByteArrayOutputStream> fileContent, Map<String,Object> manifest) {
        this.projectName = projectName;
        this.fileContent = fileContent;
        this.manifest = manifest;
    }

    @Override
    public String getName() {
        return projectName;
    }

    @Override
    public Map<String, ByteArrayOutputStream> getFileContent() {
        return fileContent;
    }

    public Map<String, Object> getManifest() {
            return manifest;
    }

    @Override
    public Map<String, UUID> getDocumentIdMapping() {
        return documentIdMapping;
    }

    @Override
    public Map<String, String> getSemanticElementsIdMappings() {
        return semanticElementsIdMappings;
    }

    public List<String> getNatures(){
        return (List<String>) getManifest().get("natures");
    }

}
