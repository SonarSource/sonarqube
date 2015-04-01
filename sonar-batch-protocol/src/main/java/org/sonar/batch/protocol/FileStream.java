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

package org.sonar.batch.protocol;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * An object to iterate over lines in a file.
 * A LineStream is opened upon creation and is closed by invoking the close method.
 *
 * Inspired by {@link java.nio.file.DirectoryStream}
 */
public class FileStream implements Closeable, Iterable<String> {

  private final File file;
  private InputStream inputStream;

  public FileStream(File file) {
    this.file = file;
  }

  @Override
  public Iterator<String> iterator() {
    if (this.inputStream != null) {
      throw new IllegalStateException("Iterator already obtained");
    } else {
      try {
        this.inputStream = ProtobufUtil.createInputStream(file);
        return IOUtils.lineIterator(inputStream, Charsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read lines from file " + file, e);
      }
    }
  }

  @Override
  public void close() {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to close input stream of file " + file, e);
      }
    }
  }
}
