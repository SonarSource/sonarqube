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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class DequeBasedPath<T> implements PathAwareVisitor.Path<T>, Iterable<PathAwareVisitor.PathElement<T>> {
  private final Deque<PathAwareVisitor.PathElement<T>> deque = new ArrayDeque<>();

  @Override
  public T current() {
    return deque.getFirst().getElement();
  }

  @Override
  public T parent() {
    Iterator<PathAwareVisitor.PathElement<T>> iterator = deque.iterator();
    if (iterator.hasNext()) {
      iterator.next();
      if (iterator.hasNext()) {
        return iterator.next().getElement();
      }
    }
    throw new NoSuchElementException("Path is either empty or has only one element. There is no parent");
  }

  @Override
  public boolean isRoot() {
    return deque.size() == 1;
  }

  @Override
  public T root() {
    return deque.getLast().getElement();
  }

  @Override
  public Iterator<PathAwareVisitor.PathElement<T>> iterator() {
    return deque.iterator();
  }

  @Override
  public Iterable<PathAwareVisitor.PathElement<T>> getCurrentPath() {
    return this;
  }

  public void add(PathAwareVisitor.PathElement<T> pathElement) {
    deque.addFirst(pathElement);
  }

  public PathAwareVisitor.PathElement<T> pop() {
    return deque.pop();
  }
}
