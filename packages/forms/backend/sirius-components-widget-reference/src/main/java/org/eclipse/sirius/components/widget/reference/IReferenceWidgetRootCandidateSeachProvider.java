/*******************************************************************************
 * Copyright (c) 2023 Obeo.
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
package org.eclipse.sirius.components.widget.reference;

import java.util.List;

import org.eclipse.sirius.components.core.api.IEditingContext;

/**
 * TOTO.
 *
 * @author Arthur Daussy
 */
public interface IReferenceWidgetRootCandidateSeachProvider {

    boolean canHandle(String descriptionId);

    List<? extends Object> getRootElementsForReference(Object targetElement, String descriptionId, IEditingContext editingContext);
}
