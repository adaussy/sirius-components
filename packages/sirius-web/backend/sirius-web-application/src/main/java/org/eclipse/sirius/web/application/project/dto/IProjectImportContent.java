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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IProjectImportContent {

    String getName();

    Map<String, ByteArrayOutputStream> getFileContent();

    Map<String, Object> getManifest();

    Map<String, UUID> getDocumentIdMapping();

    Map<String, String> getSemanticElementsIdMappings();

    List<String> getNatures();


}
