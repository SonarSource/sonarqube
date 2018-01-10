/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.util.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.util.ObjectInputStreamIterator;

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
    boolean threw = true;
    try {
      // writes the serialization stream header required when calling "traverse()"
      // on empty stream. Moreover it allows to call multiple times "newAppender()"
      output = new ObjectOutputStream(new FileOutputStream(file));
      output.flush();
      threw = false;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to write into file: " + file, e);
    } finally {
      if (threw) {
        // do not hide initial exception
        IOUtils.closeQuietly(output);
      } else {
        // raise an exception if can't close
        system2.close(output);
      }
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
          protected void writeStreamHeader() {
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
