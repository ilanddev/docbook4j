/*
 * Copyright 2013 Maxim Kalina
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.docbook4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XslURIResolver implements URIResolver, Closeable, AutoCloseable {

    private static final Logger log =
        LoggerFactory.getLogger(XslURIResolver.class);

    private final List<FileObject> fileObjects = new ArrayList<>();

    private String docbookXslBase;

    public Source resolve(String href, String base) {
        log.debug("Resolving href={} for base={}", href, base);
        if (href == null || href.trim().length() == 0)
            return null;
        if (docbookXslBase == null && href.startsWith("res:") && href
            .endsWith("docbook.xsl")) {
            try {
                docbookXslBase =
                    resolveFile(href).getParent().getURL().toExternalForm();
            } catch (FileSystemException e) {
                docbookXslBase = null;
            }
        }
        String normalizedBase = null;
        if (base != null) {
            try {
                normalizedBase =
                    resolveFile(base).getParent().getURL().toExternalForm();
            } catch (final FileSystemException ignored) {
            }
        }
        try {
            FileObject urlFileObject = resolveFile(href, normalizedBase);
            if (!urlFileObject.exists())
                throw new FileSystemException(
                    "File object not found: " + urlFileObject);
            return new FileObjectStreamSource(urlFileObject);
        } catch (final FileSystemException e) {
            // not exists for given base? try with docbook base...
            try {
                if (docbookXslBase != null) {
                    return new FileObjectStreamSource(
                        resolveFile(href, docbookXslBase));
                }
            } catch (final FileSystemException e1) {
                // do nothing.
            }
            log.error("Error resolving href=" + href + " for base=" + base, e);
        }
        return null;
    }

    private FileObject resolveFile(final String href)
        throws FileSystemException {
        return resolveFile(href, null);
    }

    private FileObject resolveFile(final String href, final String base)
        throws FileSystemException {
        final FileObject fileObject = base != null ?
            FileObjectUtils.resolveFile(href, base) :
            FileObjectUtils.resolveFile(href);
        fileObjects.add(fileObject);
        return fileObject;
    }

    @Override
    public void close() throws IOException {
        for (final FileObject fileObject : fileObjects) {
            try {
                fileObject.close();
            } catch (final IOException e) {
                log.error("Failed to close file: {}. {}", e.getMessage(),
                    fileObject.getURL().toString());
            }
        }
    }

}
