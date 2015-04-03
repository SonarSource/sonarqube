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
package org.sonar.server.util;

import com.google.common.base.Throwables;

import javax.annotation.CheckForNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class CloseableIterator<O> implements Iterator<O>, AutoCloseable {
  private O nextObject = null;
  boolean isClosed = false;

  @Override
  public final boolean hasNext() {
    // Optimization to not call bufferNext() when already closed
    if (isClosed) {
      return false;
    }
    boolean hasNext = nextObject != null || bufferNext() != null;
    if (!hasNext) {
      close();
    }
    return hasNext;
  }

  private O bufferNext() {
    try {
      return nextObject = doNext();
    } catch (RuntimeException e) {
      close();
      throw e;
    }
  }

  /**
   * Reads next item and returns null if no more items.
   */
  @CheckForNull
  protected abstract O doNext();

  @Override
  public final O next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    O result = nextObject;
    nextObject = null;
    return result;
  }

  @Override
  public final void remove() {
    try {
      doRemove();
    } catch (RuntimeException e) {
      close();
      throw e;
    }
  }

  /**
   * By default it throws an UnsupportedOperationException. Override this method
   * to change behavior.
   */
  protected void doRemove() {
    throw new UnsupportedOperationException("remove() is not supported by default. Override doRemove() if needed.");
  }

  /**
   * Do not declare "throws IOException"
   */
  @Override
  public final void close() {
    try {
      doClose();
      isClosed = true;
    } catch (Exception e) {
      Throwables.propagate(e);
    }
  }

  protected abstract void doClose() throws Exception;

}
