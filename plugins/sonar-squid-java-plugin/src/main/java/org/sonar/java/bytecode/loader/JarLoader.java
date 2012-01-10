/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.bytecode.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;

class JarLoader implements Loader {

  private final JarFile jarFile;
  private final URL jarUrl;

  /**
   * @throws IOException if an I/O error has occurred
   */
  public JarLoader(File file) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("file can't be null");
    }
    jarFile = new JarFile(file);
    jarUrl = new URL("jar", "", -1, file.getAbsolutePath() + "!/");
  }

  public URL findResource(String name) {
    ZipEntry entry = jarFile.getEntry(name);
    if (entry != null) {
      try {
        return new URL(jarUrl, name, new JarEntryHandler(entry));
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return null;
  }

  public byte[] loadBytes(String name) {
    InputStream is = null;
    try {
      ZipEntry entry = jarFile.getEntry(name);
      if (entry == null) {
        return null;
      }
      is = jarFile.getInputStream(entry);
      return IOUtils.toByteArray(is);
    } catch (IOException e) {
      return null;
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  public void close() {
    try {
      jarFile.close();
    } catch (IOException e) {
      // ignore
    }
  }

  private class JarEntryHandler extends URLStreamHandler {

    private ZipEntry entry;

    JarEntryHandler(ZipEntry entry) {
      this.entry = entry;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      return new URLConnection(u) {
        @Override
        public void connect() throws IOException {
        }

        @Override
        public int getContentLength() {
          return (int) entry.getSize();
        }

        @Override
        public InputStream getInputStream() throws IOException {
          return jarFile.getInputStream(entry);
        }
      };
    }
  }

}
