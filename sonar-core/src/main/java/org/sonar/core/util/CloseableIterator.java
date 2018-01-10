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
package org.sonar.core.util;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.ArrayUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public abstract class CloseableIterator<O> implements Iterator<O>, AutoCloseable {
  private O nextObject = null;
  boolean isClosed = false;
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
    protected void doClose() {
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
  public static <T> CloseableIterator<T> from(Iterator<T> iterator) {
    // early fail
    requireNonNull(iterator);
    checkArgument(!(iterator instanceof AutoCloseable), "This method does not support creating a CloseableIterator from an Iterator which is Closeable");
    return new RegularIteratorWrapper<>(iterator);
  }

  /**
   * Wraps a {@code CloseableIterator} and optionally other instances of {@code AutoCloseable} that must be closed
   * at the same time. The wrapped iterator is closed first then the other {@code AutoCloseable} in the defined order.
   * 
   * @throws IllegalArgumentException if the parameter {@code otherCloseables} contains the wrapped iterator
   */
  public static <T> CloseableIterator<T> wrap(CloseableIterator<T> iterator, AutoCloseable... otherCloseables) {
    return new CloseablesIteratorWrapper<>(iterator, otherCloseables);
  }

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
   * Reads next item and returns {@code null} if no more items.
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
    protected void doClose() {
      // do nothing
    }
  }

  private static class CloseablesIteratorWrapper<T> extends CloseableIterator<T> {
    private final CloseableIterator<T> iterator;
    private final List<AutoCloseable> otherCloseables;

    private CloseablesIteratorWrapper(CloseableIterator<T> iterator, AutoCloseable... otherCloseables) {
      requireNonNull(iterator);
      checkArgument(!ArrayUtils.contains(otherCloseables, iterator));
      this.iterator = iterator;
      // the advantage of using ImmutableList is that it does not accept null elements, so it fails fast, during
      // construction of the wrapper, but not in close()
      this.otherCloseables = ImmutableList.copyOf(otherCloseables);
    }

    @Override
    protected T doNext() {
      return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    protected void doClose() throws Exception {
      // iterator can be already closed by doNext(), but closing here ensures
      // that iterator is closed when it is not fully traversed.
      iterator.close();

      for (AutoCloseable otherCloseable : otherCloseables) {
        otherCloseable.close();
      }
    }
  }
}
