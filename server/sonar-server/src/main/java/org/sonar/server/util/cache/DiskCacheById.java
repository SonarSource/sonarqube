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
import org.sonar.api.utils.System2;
import org.sonar.server.util.CloseableIterator;
import org.sonar.server.util.EmptyIterator;
import org.sonar.server.util.ObjectInputStreamIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Serialize and deserialize objects on disk in different files identified by an id. No search capabilities, only traversal (full scan).
 */
public class DiskCacheById<O extends Serializable> {

  private final File directory;
  private final System2 system2;

  public DiskCacheById(File folder, System2 system2) {
    this.system2 = system2;
    this.directory = folder;
  }

  public DiskAppender newAppender(int id) {
    return new DiskAppender(id);
  }

  public CloseableIterator<O> traverse(int id) {
    File file = getFile(id);
    if (!file.exists()) {
      return new EmptyIterator<>();
    }
    try {
      return new ObjectInputStreamIterator<>(FileUtils.openInputStream(file));
    } catch (IOException e) {
      throw new IllegalStateException("Fail to traverse file: " + file, e);
    }
  }

  private File getFile(int id) {
    return new File(directory, id + ".dat");
  }

  public class DiskAppender implements AutoCloseable {
    private final ObjectOutputStream output;
    private final File file;

    private DiskAppender(int id) {
      this.file = getFile(id);
      try {
        this.output = new ObjectOutputStream(new FileOutputStream(file, true));
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
