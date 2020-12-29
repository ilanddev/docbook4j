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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;

import com.google.code.docbook4j.Docbook4JException;
import com.google.code.docbook4j.FileObjectInputSource;
import com.google.code.docbook4j.FileObjectStreamSource;
import com.google.code.docbook4j.FileObjectUtils;
import com.google.code.docbook4j.VfsResourceResolver;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactoryBuilder;
import org.xml.sax.SAXException;

abstract class FORenderer<T extends FORenderer<T>> extends BaseRenderer<T> {

  private static final String defaultXslStylesheet =
      "res:xsl/docbook/fo/docbook.xsl";

  @Override
  protected FileObject getDefaultXslStylesheet() {
    return resolveXslStylesheet(defaultXslStylesheet);
  }

  @Override
  protected FileObject postProcess(final FileObject xmlSource,
      final FileObject xslSource, final FileObject xsltResult,
      final FileObject userConfigXml)
      throws Docbook4JException {
    final String tmpPdf = "tmp://" + UUID.randomUUID().toString();
    try (final FileObject target = FileObjectUtils.resolveFile(tmpPdf)) {
      FopFactoryBuilder builder =
          new FopFactoryBuilder(xmlSource.getParent().getURL().toURI(),
              new VfsResourceResolver());
      builder.setBaseURI(xmlSource.getParent().getURL().toURI());

      target.createFile();

      final Configuration configuration = createFOPConfig(userConfigXml);
      if (configuration != null) {
        final String configUrl =
            userConfigXml.getParent().getURL().toExternalForm();
        final String baseUrl;
        if (!configUrl.endsWith("/")) {
          baseUrl = userConfigXml.getParent().getURL().toExternalForm() + "/";
        } else {
          baseUrl = configUrl;
        }
        builder = new FopFactoryBuilder(new URI(baseUrl))
            .setConfiguration(configuration);
      }

      final Fop fop = builder.build()
          .newFop(getMimeType(), target.getContent().getOutputStream());

      final TransformerFactory factory = TransformerFactory.newInstance();
      final Transformer transformer = factory.newTransformer(); // identity
      // transformer
      transformer.setParameter("use.extensions", "1");
      transformer.setParameter("fop.extensions", "0");
      transformer.setParameter("fop1.extensions", "1");
      final Result res = new SAXResult(fop.getDefaultHandler());
      transformer.transform(new FileObjectStreamSource(xsltResult), res);
      return target;
    } catch (final URISyntaxException e) {
      throw new Docbook4JException("Error resolving URI!", e);
    } catch (final FileSystemException e) {
      throw new Docbook4JException("Error create filesystem manager!", e);
    } catch (final TransformerException | FOPException e) {
      throw new Docbook4JException("Error transforming fo to pdf!", e);
    } catch (final ConfigurationException | SAXException | IOException e) {
      throw new Docbook4JException("Error loading user configuration!", e);
    }
  }

  protected Configuration createFOPConfig(final FileObject userConfigXml)
      throws IOException, SAXException, ConfigurationException {
    if (userConfigXml == null) {
      return null;
    }
    DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
    return cfgBuilder.build(new FileObjectInputSource(userConfigXml));
  }

  protected abstract String getMimeType();

}
