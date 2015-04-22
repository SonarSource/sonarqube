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

import javax.annotation.Nullable;

import java.util.Set;

public abstract class BeanElement<T extends Element, C extends BeanElement<T, C>> {
  private T element;
  private BeanGraph graph;

  public T element() {
    return element;
  }

  void setElement(T element) {
    this.element = element;
  }

  public BeanGraph beanGraph() {
    return graph;
  }

  void setBeanGraph(BeanGraph graph) {
    this.graph = graph;
  }

  protected final Object getProperty(String key) {
    return element.getProperty(key);
  }

  protected final Set<String> getPropertyKeys() {
    return element.getPropertyKeys();
  }

  public final C setProperty(String key, @Nullable Object value) {
    if (value != null) {
      element.setProperty(key, value);
    } else {
      element.removeProperty(key);
    }
    return (C) this;
  }

  protected final Object removeProperty(String key) {
    return element.removeProperty(key);
  }
}
