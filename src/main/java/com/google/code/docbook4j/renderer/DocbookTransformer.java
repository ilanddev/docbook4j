package com.google.code.docbook4j.renderer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import com.google.code.docbook4j.FileObjectStreamSource;
import com.google.code.docbook4j.XslURIResolver;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

/**
 * TransformerWrapper.
 *
 * @author <a href="mailto:csnyder@iland.com">Cory Snyder</a>
 */
public final class DocbookTransformer extends Transformer
    implements Closeable, AutoCloseable {

  private final XslURIResolver xslURIResolver;

  private final Transformer transformer;

  DocbookTransformer(final FileObject xmlSource, final FileObject xslStylesheet,
      final Map<String, String> params) throws FileSystemException {
    this.xslURIResolver = new XslURIResolver();
    try {
      final TransformerFactory transformerFactory =
          TransformerFactory.newInstance();
      transformerFactory.setURIResolver(xslURIResolver);
      final Source source = new FileObjectStreamSource(xslStylesheet);
      this.transformer = transformerFactory.newTransformer(source);
      transformer.setParameter("use.extensions", "1");
      transformer.setParameter("callout.graphics", "0");
      transformer.setParameter("callout.unicode", "1");
      transformer.setParameter("callouts.extension", "1");
      transformer.setParameter("base.dir",
          xmlSource.getParent().getURL().toExternalForm());
      for (Map.Entry<String, String> entry : params.entrySet()) {
        transformer.setParameter(entry.getKey(), entry.getValue());
      }
    } catch (final TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void transform(final Source source, final Result result)
      throws TransformerException {
    transformer.transform(source, result);
  }

  @Override
  public void setParameter(final String s, final Object o) {
    transformer.setParameter(s, o);
  }

  @Override
  public Object getParameter(final String s) {
    return transformer.getParameter(s);
  }

  @Override
  public void clearParameters() {
    transformer.clearParameters();
  }

  @Override
  public void setURIResolver(final URIResolver uriResolver) {
    throw new RuntimeException(
        "DocbookTransformer does not implement setUriResolver");
  }

  @Override
  public URIResolver getURIResolver() {
    return xslURIResolver;
  }

  @Override
  public void setOutputProperties(final Properties properties) {
    transformer.setOutputProperties(properties);
  }

  @Override
  public Properties getOutputProperties() {
    return transformer.getOutputProperties();
  }

  @Override
  public void setOutputProperty(final String s, final String s1)
      throws IllegalArgumentException {
    transformer.setOutputProperty(s, s1);
  }

  @Override
  public String getOutputProperty(final String s)
      throws IllegalArgumentException {
    return transformer.getOutputProperty(s);
  }

  @Override
  public void setErrorListener(final ErrorListener errorListener)
      throws IllegalArgumentException {
    transformer.setErrorListener(errorListener);
  }

  @Override
  public ErrorListener getErrorListener() {
    return transformer.getErrorListener();
  }

  @Override
  public void close() throws IOException {
    this.xslURIResolver.close();
  }
}
