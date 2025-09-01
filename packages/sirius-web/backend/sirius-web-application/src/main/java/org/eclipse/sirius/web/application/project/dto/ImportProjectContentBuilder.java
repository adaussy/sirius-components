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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImportProjectContentBuilder {

    private static final String ZIP_FOLDER_SEPARATOR = "/";

    private static final String MANIFEST_JSON_FILE = "manifest.json";
    private final Logger logger = LoggerFactory.getLogger(ImportProjectContentBuilder.class);

    private final ObjectMapper objectMapper;

    public ImportProjectContentBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProjectImportContent buildFromZip(InputStream data) throws IOException {
        Map<String, ByteArrayOutputStream> fileContent = this.readZipFile(data);
        String name = getName(fileContent);
        if(name == null || name.isEmpty()){
            throw new IOException("Unable to read project from zip.");
        }
        Map<String, Object> manifest = getManifest(fileContent, name);
        return new ProjectImportContent(name,fileContent,manifest);
    }


    /**
     * Returns the project name if all zip entries represented by couple (zipEntry name -> OutputStream) in the given
     * {@link Map}, have their zip entry name starting by the project name, which should be the first segment of the
     * path of each zip entries.
     *
     * @return The name of the project
     */
    private String getName(Map<String, ByteArrayOutputStream> zipEntryNameToContent) {

        Iterator<String> iterator = zipEntryNameToContent.keySet().iterator();
        if (!iterator.hasNext()) {
            // zip was empty
            return null;
        }

        String optionalProjectName = null;
        String possibleProjectName = iterator.next().split(ZIP_FOLDER_SEPARATOR)[0];
        if (!possibleProjectName.isBlank() && zipEntryNameToContent.keySet().stream().allMatch(key -> key.split(ZIP_FOLDER_SEPARATOR)[0].equals(possibleProjectName))) {
            return possibleProjectName;
        }

        return null;
    }

    private Map<String, Object> getManifest(Map<String, ByteArrayOutputStream> zipEntryNameToContent,String projectName) {
        String manifestPathInZip = projectName + ZIP_FOLDER_SEPARATOR + MANIFEST_JSON_FILE;

        byte[] manifestBytes = zipEntryNameToContent.entrySet().stream().filter(entry -> entry.getKey().equals(manifestPathInZip)).map(Map.Entry::getValue).map(ByteArrayOutputStream::toByteArray)
                .findFirst().orElse(new byte[0]);

        try {
            return this.objectMapper.readValue(manifestBytes, HashMap.class);
        } catch (IOException exception) {
            this.logger.warn(exception.getMessage(), exception);
        }

        return Map.of();
    }

    /**
     * Reads the zip file and returns the a map of zip entry name to zip entry content.
     *
     * @return The zip entry names mapped to its zip entry contents
     */
    private Map<String, ByteArrayOutputStream> readZipFile(InputStream inputStream) {
        Map<String, ByteArrayOutputStream> entryToHandle = new HashMap<>();
        try (var zipperProjectInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry = zipperProjectInputStream.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String name = zipEntry.getName();
                    ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                    zipperProjectInputStream.transferTo(entryBaos);
                    entryToHandle.put(name, entryBaos);
                }
                zipEntry = zipperProjectInputStream.getNextEntry();
            }
        } catch (IOException exception) {
            this.logger.warn(exception.getMessage(), exception);
        }

        return entryToHandle;
    }
}
