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
package org.sonar.server.util.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.System2;
import org.sonar.server.util.CloseableIterator;
import org.sonar.server.util.ObjectInputStreamIterator;

import java.io.*;

/**
 * Serialize and deserialize objects on disk. No search capabilities, only traversal (full scan).
 */
public class DiskCache<O extends Serializable> {

  private final File file;
  private final System2 system2;

  public DiskCache(File file, System2 system2) {
    this.system2 = system2;
    this.file = file;
    OutputStream output = null;
    try {
      // writes the serialization stream header required when calling "traverse()"
      // on empty stream. Moreover it allows to call multiple times "newAppender()"
      output = new ObjectOutputStream(new FileOutputStream(file));
      output.flush();

      // raise an exception if can't close
      system2.close(output);
    } catch (IOException e) {
      // do not hide cause exception -> close quietly
      IOUtils.closeQuietly(output);
      throw new IllegalStateException("Fail to write into file: " + file, e);
    }
  }

  public DiskAppender newAppender() {
    return new DiskAppender();
  }

  public CloseableIterator<O> traverse() {
    try {
      return new ObjectInputStreamIterator<>(FileUtils.openInputStream(file));
    } catch (IOException e) {
      throw new IllegalStateException("Fail to traverse file: " + file, e);
    }
  }

  public class DiskAppender implements AutoCloseable {
    private final ObjectOutputStream output;

    private DiskAppender() {
      try {
        this.output = new ObjectOutputStream(new FileOutputStream(file, true)) {
          @Override
          protected void writeStreamHeader() throws IOException {
            // do not write stream headers as it's already done in constructor of DiskCache
          }
        };
      } catch (IOException e) {
        throw new IllegalStateException("Fail to open file " + file, e);
      }
    }

    public DiskAppender append(O object) {
      try {
        output.writeObject(object);
        output.reset();
        return this;
      } catch (IOException e) {
        throw new IllegalStateException("Fail to write into file " + file, e);
      }
    }

    @Override
    public void close() {
      system2.close(output);
    }
  }
}
