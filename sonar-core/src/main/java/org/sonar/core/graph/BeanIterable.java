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
package org.sonar.core.graph;

import com.tinkerpop.blueprints.Element;

import java.util.Iterator;

public class BeanIterable<T extends BeanElement> implements Iterable<T> {

  private final Iterable<? extends Element> iterable;
  private final BeanGraph graph;
  private final Class<T> beanClass;

  public BeanIterable(BeanGraph graph, Class<T> beanClass, Iterable<? extends Element> iterable) {
    this.iterable = iterable;
    this.graph = graph;
    this.beanClass = beanClass;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private final Iterator<? extends Element> iterator = iterable.iterator();

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean hasNext() {
        return this.iterator.hasNext();
      }

      @Override
      public T next() {
        return graph.wrap(this.iterator.next(), beanClass);
      }
    };
  }
}
