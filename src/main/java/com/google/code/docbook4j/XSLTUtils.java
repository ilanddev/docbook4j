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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.xerces.impl.dv.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XSLTUtils {

    private static final Logger log = LoggerFactory.getLogger(XSLTUtils.class);

    public static String toBase64(final String baseDir, final String location) {
        try (final FileObject fo = FileObjectUtils
            .resolveFile(location, baseDir);
            final InputStream inputStream = fo.getContent().getInputStream()) {
            return String
                .format("data:%s;base64,%s", determineMimeType(location),
                    Base64.encode(IOUtils.toByteArray(inputStream)));
        } catch (Exception e) {
            log.error("Error reading image file: " + location, e);
        }
        return location;
    }

    public static String dumpCss(final String baseDir, final String location) {
        try (final FileObject fo = FileObjectUtils
            .resolveFile(location, baseDir);
            final InputStream inputStream = fo.getContent().getInputStream()) {
            return String
                .format("<!--\n%s\n-->\n", IOUtils.toString(inputStream));
        } catch (Exception e) {
            log.error("Error reading css file: " + location, e);
        }
        return "";
    }

    private static String determineMimeType(final String location) {
        String s = location.toLowerCase().trim();
        if (s.endsWith("png"))
            return "image/png";
        if (s.endsWith("gif"))
            return "image/gif";
        if (s.endsWith("jpg") || s.endsWith("jpeg"))
            return "image/jpeg";
        return "image/gif"; // default
    }
}
