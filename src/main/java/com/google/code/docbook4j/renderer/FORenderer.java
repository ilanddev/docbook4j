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

import com.google.code.docbook4j.Docbook4JException;
import com.google.code.docbook4j.FileObjectUtils;
import com.google.code.docbook4j.VfsResourceResolver;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
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

    FileObject target = null;
    try {

      FopFactoryBuilder builder =
          new FopFactoryBuilder(xmlSource.getParent().getURL().toURI(),
              new VfsResourceResolver());
      builder.setBaseURI(xmlSource.getParent().getURL().toURI());

      String tmpPdf = "tmp://" + UUID.randomUUID().toString();
      target = FileObjectUtils.resolveFile(tmpPdf);
      target.createFile();

      final Configuration configuration = createFOPConfig(userConfigXml);
      if (configuration != null) {
        final String configUrl = userConfigXml.getParent().getURL().toExternalForm();
        final String baseUrl;
        if(!configUrl.endsWith("/")) {
          baseUrl = userConfigXml.getParent().getURL().toExternalForm() + "/";
        } else {
          baseUrl  = configUrl;
        }
        builder = new FopFactoryBuilder(new URI(baseUrl))
            .setConfiguration(configuration);
      }

      final Fop fop =
          builder.build().newFop(getMimeType(), target
              .getContent().getOutputStream());

      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer(); // identity
      // transformer
      transformer.setParameter("use.extensions", "1");
      transformer.setParameter("fop.extensions", "0");
      transformer.setParameter("fop1.extensions", "1");

      Source src = new StreamSource(xsltResult.getContent()
          .getInputStream());
      Result res = new SAXResult(fop.getDefaultHandler());
      transformer.transform(src, res);
      return target;

    } catch (final URISyntaxException e) {
      throw new Docbook4JException("Error resovling URI!", e);
    } catch (FileSystemException e) {
      throw new Docbook4JException("Error create filesystem manager!", e);
    } catch (TransformerException e) {
      throw new Docbook4JException("Error transforming fo to pdf!", e);
    } catch (FOPException e) {
      throw new Docbook4JException("Error transforming fo to pdf!", e);
    } catch (ConfigurationException e) {
      throw new Docbook4JException("Error loading user configuration!", e);
    } catch (SAXException e) {
      throw new Docbook4JException("Error loading user configuration!", e);
    } catch (IOException e) {
      throw new Docbook4JException("Error loading user configuration!", e);
    } finally {
      FileObjectUtils.closeFileObjectQuietly(target);
    }

  }

  protected Configuration createFOPConfig(final FileObject userConfigXml)
      throws IOException, SAXException, ConfigurationException {
    if (userConfigXml == null) {
      return null;
    }
    DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
    Configuration cfg =
        cfgBuilder.build(userConfigXml.getContent().getInputStream());
    return cfg;
  }

  protected abstract String getMimeType();

}
