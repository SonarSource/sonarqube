/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.home.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteFileOnCloseInputStream extends InputStream {
  private final InputStream is;
  private final Path p;

  public DeleteFileOnCloseInputStream(InputStream stream, Path p) {
    this.is = stream;
    this.p = p;
  }

  @Override
  public int read() throws IOException {
    return is.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return is.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return is.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return is.skip(n);
  }

  @Override
  public synchronized void mark(int readlimit) {
    is.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    is.reset();
  }

  @Override
  public int available() throws IOException {
    return is.available();
  }

  @Override
  public boolean markSupported() {
    return is.markSupported();
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      Files.delete(p);
    }
  }
}
