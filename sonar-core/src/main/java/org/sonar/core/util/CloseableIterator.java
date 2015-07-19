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
package org.sonar.core.util;

import com.google.common.base.Throwables;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public abstract class CloseableIterator<O> implements Iterator<O>, AutoCloseable {
  private static final CloseableIterator<?> EMPTY_CLOSEABLE_ITERATOR = new CloseableIterator<Object>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    protected Object doNext() {
      // never called anyway
      throw new NoSuchElementException("Empty closeable Iterator has no element");
    }

    @Override
    protected void doClose() throws Exception {
      // do nothing
    }
  };

  @SuppressWarnings("unchecked")
  public static <T> CloseableIterator<T> emptyCloseableIterator() {
    return (CloseableIterator<T>) EMPTY_CLOSEABLE_ITERATOR;
  }

  /**
   * Creates a CloseableIterator from a regular {@link Iterator}.
   *
   * @throws IllegalArgumentException if the specified {@link Iterator} is a CloseableIterator
   */
  public static <T> CloseableIterator<T> from(final Iterator<T> iterator) {
    // early fail
    requireNonNull(iterator);
    checkArgument(!(iterator instanceof AutoCloseable), "This method does not support creating a CloseableIterator from an Iterator which is Closeable");
    return new RegularIteratorWrapper<>(iterator);
  }

  private O nextObject = null;
  boolean isClosed = false;

  @Override
  public boolean hasNext() {
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
      nextObject = doNext();
      return nextObject;
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
  public O next() {
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

  private static class RegularIteratorWrapper<T> extends CloseableIterator<T> {
    private final Iterator<T> iterator;

    public RegularIteratorWrapper(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public T next() {
      return iterator.next();
    }

    @Override
    protected T doNext() {
      throw new UnsupportedOperationException("hasNext has been override, doNext is never called");
    }

    @Override
    protected void doClose() throws Exception {
      // do nothing
    }
  }
}
