/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.component;

import com.google.common.collect.MapMaker;
import com.tinkerpop.blueprints.Element;

import java.util.Map;

public class ElementWrappers {


  private final Map<ElementKey, ElementWrapper> cache;

  public ElementWrappers() {
    cache = new MapMaker().weakValues().makeMap();
  }

  public void clear() {
    cache.clear();
  }

  public <T extends ElementWrapper> T wrap(Element element, Class<T> wrapperClass, ComponentGraph graph) {
    ElementKey key = new ElementKey(element, wrapperClass);
    T wrapper = (T) cache.get(key);
    if (wrapper == null) {
      try {
      wrapper = (T)key.wrapperClass.newInstance();
      wrapper.setElement(key.element);
      wrapper.setGraph(graph);
      cache.put(key, wrapper);
      } catch (InstantiationException e) {
        throw new IllegalStateException("Class has no default constructor: " + wrapperClass, e);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Can not access to default constructor: " + wrapperClass, e);
      }
    }
    return wrapper;
  }

  public ElementWrappers remove(Element elt, Class wrapperClass) {
    cache.remove(new ElementKey(elt, wrapperClass));
    return this;
  }

  private static class ElementKey {
    Element element;
    Class<? extends ElementWrapper> wrapperClass;

    ElementKey(Element element, Class<? extends ElementWrapper> wrapperClass) {
      this.element = element;
      this.wrapperClass = wrapperClass;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ElementKey that = (ElementKey) o;
      if (!element.equals(that.element)) {
        return false;
      }
      return wrapperClass.equals(that.wrapperClass);
    }

    @Override
    public int hashCode() {
      int result = element.hashCode();
      result = 31 * result + wrapperClass.hashCode();
      return result;
    }
  }
}
