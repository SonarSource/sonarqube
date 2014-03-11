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

import com.google.common.collect.MapMaker;
import com.tinkerpop.blueprints.Element;

import java.util.Map;

class BeanElements {

  private final Map<ElementKey, BeanElement> cache;

  BeanElements() {
    cache = new MapMaker().weakValues().makeMap();
  }

  <T extends BeanElement> T wrap(Element element, Class<T> beanClass, BeanGraph graph) {
    ElementKey key = new ElementKey(element, beanClass);
    T bean = (T) cache.get(key);
    if (bean == null) {
      try {
        bean = (T) key.beanClass.newInstance();
        bean.setElement(key.element);
        bean.setBeanGraph(graph);
        cache.put(key, bean);
      } catch (InstantiationException e) {
        throw new IllegalStateException("Class has no default constructor: " + beanClass.getName(), e);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Can not access to default constructor: " + beanClass.getName(), e);
      }
    }
    return bean;
  }

  void clear() {
    cache.clear();
  }

  private static class ElementKey {
    Element element;
    Class<? extends BeanElement> beanClass;

    ElementKey(Element element, Class<? extends BeanElement> beanClass) {
      this.element = element;
      this.beanClass = beanClass;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      ElementKey that = (ElementKey) o;
      if (!element.equals(that.element)) {
        return false;
      }
      return beanClass.equals(that.beanClass);
    }

    @Override
    public int hashCode() {
      int result = element.hashCode();
      result = 31 * result + beanClass.hashCode();
      return result;
    }
  }
}
