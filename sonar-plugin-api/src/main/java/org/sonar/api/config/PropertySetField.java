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

import org.sonar.api.PropertyType;

public final class PropertySetField {
  private final String name;
  private final PropertyType type;
  private String description = "";
  private String defaultValue = "";

  private PropertySetField(String name, PropertyType type) {
    this.name = name;
    this.type = type;
  }

  public static PropertySetField create(String name, PropertyType type) {
    return new PropertySetField(name, type);
  }

  public String getName() {
    return name;
  }

  public PropertyType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public PropertySetField setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public PropertySetField setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }
}
