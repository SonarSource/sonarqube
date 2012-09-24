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
package org.sonar.api.config;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.NoSuchElementException;

public class PropertySetDefinitions {
  private final Map<String, PropertySet> index = Maps.newHashMap();

  public void register(String name, PropertySet propertySet) {
    if (null != index.put(name, propertySet)) {
      throw new IllegalStateException("Unable to register two property sets with same name " + name);
    }
  }

  public PropertySet findByName(String name) {
    PropertySet propertySet = index.get(name);
    if (propertySet == null) {
      throw new NoSuchElementException("Property set not found " + name);
    }
    return propertySet;
  }
}
