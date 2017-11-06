/*
 * Copyright (c) 2013,2014,2015,2016 iland Internet Solutions
 *
 *
 * This software is licensed under the Terms and Conditions contained within the
 * "LICENSE.txt" file that accompanied this software. Any inquiries concerning
 * the scope or enforceability of the license should be addressed to:
 *
 * iland Internet Solutions
 * 1235 North Loop West, Suite 800
 * Houston, TX 77008
 * USA
 *
 * http://www.iland.com
 */

package com.google.code.docbook4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import org.apache.commons.vfs2.FileObject;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;

/**
 * VfsResourceResolver.
 *
 * @author <a href="mailto:bsnyder@iland.com">Brett Snyder</a>
 */
public class VfsResourceResolver implements ResourceResolver {

  public Resource getResource(final URI uri) throws IOException {
    final FileObject urlFileObject = FileObjectUtils.resolveFile(uri.getPath());
    return new Resource(urlFileObject.getContent().getInputStream());
  }

  public OutputStream getOutputStream(final URI uri) throws IOException {
    final FileObject urlFileObject =
        FileObjectUtils.resolveFile(uri.getPath());
    return urlFileObject.getContent().getOutputStream();
  }

}
