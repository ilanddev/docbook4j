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

package com.google.code.docbook4j.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import com.google.code.docbook4j.Docbook4JException;
import com.google.code.docbook4j.ExpressionEvaluatingXMLReader;
import com.google.code.docbook4j.FileObjectInputSource;
import com.google.code.docbook4j.FileObjectUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

abstract class BaseRenderer<T extends BaseRenderer<T>> implements Renderer<T> {

    private static final Logger log = LoggerFactory
            .getLogger(BaseRenderer.class);

    protected String xmlResource;

    protected String xslResource;

    protected String userConfigXmlResource;

    protected Map<String, String> params = new HashMap<String, String>();

    protected Map<String, Object> vars = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public T xml(String xmlResource) {
        this.xmlResource = xmlResource;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T userConfig(String userConfigXmlResource) {
        this.userConfigXmlResource = userConfigXmlResource;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T xsl(String xslResource) {
        this.xslResource = xslResource;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T fileSystemOptions(FileSystemOptions fileSystemOptions) {
        FileObjectUtils.setFileSystemOptions(fileSystemOptions);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T parameter(String name, String value) {
        this.params.put(name, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T parameters(Map<String, String> parameters) {
        if (parameters != null)
            this.params.putAll(parameters);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T variable(String name, Object value) {
        this.vars.put(name, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T variables(Map<String, Object> values) {
        if (values != null)
            this.vars.putAll(values);
        return (T) this;
    }

    public InputStream render() throws Docbook4JException {
        assertNotNull(xmlResource,
            "Value of the xml source should be not null!");
        final FileObject result;
        try (final FileObject xmlSourceFileObject = FileObjectUtils
            .resolveFile(xmlResource);
            final FileObject xslSourceFileObject = xslResource != null ?
                FileObjectUtils.resolveFile(xslResource) :
                getDefaultXslStylesheet();
            final DocbookTransformer transformer = new DocbookTransformer(
                xmlSourceFileObject, xslSourceFileObject, params);
            final FileObject xsltResult = FileObjectUtils
                .resolveFile("tmp://" + UUID.randomUUID().toString());
            final FileObject userConfigXmlSourceFileObject =
                userConfigXmlResource != null ?
                    FileObjectUtils.resolveFile(userConfigXmlResource) :
                    null) {
            final SAXParserFactory factory = createParserFactory();
            final XMLReader reader = factory.newSAXParser().getXMLReader();

            // prepare xml sax source
            final ExpressionEvaluatingXMLReader piReader =
                new ExpressionEvaluatingXMLReader(reader, vars);
            piReader.setEntityResolver((publicId, systemId) -> {
                log.debug("Resolving file {}", systemId);
                FileObject inc = FileObjectUtils.resolveFile(systemId);
                return new InputSource(inc.getContent().getInputStream());
            });

            final SAXSource source = new SAXSource(piReader,
                new FileObjectInputSource(xmlSourceFileObject));

            // prepare xslt result
            xsltResult.createFile();

            // create transformer and do transformation
            transformer.transform(source,
                new StreamResult(xsltResult.getContent().getOutputStream()));

            // do post processing
            result = postProcess(xmlSourceFileObject, xslSourceFileObject,
                xsltResult, userConfigXmlSourceFileObject);
        } catch (final TransformerException | ParserConfigurationException | SAXException e) {
            throw new Docbook4JException("Error transforming xml!", e);
        } catch (IOException e) {
            throw new Docbook4JException("Error transforming xml !", e);
        }
        try {
            return result.getContent().getInputStream();
        } catch (final FileSystemException e) {
            throw new Docbook4JException("Error transforming xml!", e);
        }
    }

    protected FileObject postProcess(FileObject xmlSource,
                                     FileObject xslSource, FileObject xsltResult, FileObject userConfigXml)
            throws Docbook4JException {
        return xsltResult;
    }

    protected SAXParserFactory createParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setXIncludeAware(true);
        factory.setNamespaceAware(true);
        return factory;
    }

    protected abstract FileObject getDefaultXslStylesheet();

    protected FileObject resolveXslStylesheet(String location) {
        try {
            return FileObjectUtils.resolveFile(location);

        } catch (FileSystemException e) {
            throw new IllegalStateException("Error resolving xsl stylesheet: "
                    + location, e);
        }

    }

    private void assertNotNull(Object value, String message) {
        if (value == null)
            throw new IllegalArgumentException(message);
    }

}
