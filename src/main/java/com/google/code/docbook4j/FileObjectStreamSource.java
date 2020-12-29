package com.google.code.docbook4j;

import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

/**
 * FileObjectStreamSource.
 *
 * @author <a href="mailto:csnyder@iland.com">Cory Snyder</a>
 */
public final class FileObjectStreamSource extends StreamSource {

  private final FileObject fileObject;

  public FileObjectStreamSource(final FileObject fileObject)
      throws FileSystemException {
    super(fileObject.getURL().toExternalForm());
    this.fileObject = fileObject;
  }

  @Override
  public InputStream getInputStream() {
    try {
      return fileObject.getContent().getInputStream();
    } catch (final FileSystemException e) {
      throw new RuntimeException(e);
    }
  }

}
