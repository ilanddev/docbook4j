package com.google.code.docbook4j;

import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.xml.sax.InputSource;

/**
 * FileObjectInputSource.
 *
 * @author <a href="mailto:csnyder@iland.com">Cory Snyder</a>
 */
public final class FileObjectInputSource extends InputSource {

  private final FileObject fileObject;

  public FileObjectInputSource(final FileObject fileObject)
      throws FileSystemException {
    super(fileObject.getURL().toExternalForm());
    this.fileObject = fileObject;
  }

  @Override
  public InputStream getByteStream() {
    try {
      return fileObject.getContent().getInputStream();
    } catch (final FileSystemException e) {
      throw new RuntimeException(e);
    }
  }
}
