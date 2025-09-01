/*******************************************************************************
 * Copyright (c) 2019, 2022 Obeo.
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
package org.eclipse.sirius.components.graphql.api;

import java.io.InputStream;
import java.text.MessageFormat;

/**
 * A file.
 *
 * @author hmarchadour
 */
public class UploadFile2 {

    private final String id;

    private final String name;


    private final InputStream inputStream;

    public UploadFile2( String id,String name, InputStream inputStream) {
        this.name = name;
        this.inputStream = inputStream;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        String pattern = "{0} '{' name: {1} '}'";
        return MessageFormat.format(pattern, this.getClass().getSimpleName(), this.name);
    }
}
