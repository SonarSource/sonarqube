/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.util.cache;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.sonar.core.util.CloseableIterator;

public class ObjectInputStreamIterator<E> extends CloseableIterator<E> {

  private ObjectInputStream stream;

  public ObjectInputStreamIterator(InputStream stream) throws IOException {
    this.stream = new ObjectInputStream(stream);
  }

  @Override
  protected E doNext() {
    try {
      return (E) stream.readObject();
    } catch (EOFException e) {
      return null;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected void doClose() {
    IOUtils.closeQuietly(stream);
  }
}
