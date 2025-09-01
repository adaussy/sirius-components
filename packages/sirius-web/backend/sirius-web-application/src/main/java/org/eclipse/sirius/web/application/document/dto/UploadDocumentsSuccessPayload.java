/*******************************************************************************
 * Copyright (c) 2024, 2025 Obeo.
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
package org.eclipse.sirius.web.application.document.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.eclipse.sirius.components.core.api.IPayload;

/**
 * The payload of the upload of multiple documents mutation.
 *
 * <p>Note that the order of the return {@link DocumentDTO} match the order to the given file to upload.</p>
 *
 * @author Arthur Daussy
 */
public record UploadDocumentsSuccessPayload(@NotNull UUID id, @NotNull List<UploadedDocumentDTO> uploadedDocuments) implements IPayload {
}
