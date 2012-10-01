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

import org.sonar.api.PropertyField;
import org.sonar.api.PropertyType;

import javax.annotation.Nullable;

/**
 * @since 3.3
 */
public final class PropertyFieldDefinition {
  private String key;
  private String defaultValue;
  private String name;
  private PropertyType type = PropertyType.STRING;
  private String[] options;
  private String description;

  private PropertyFieldDefinition(PropertyField annotation) {
    this.key = annotation.key();
    this.name = annotation.name();
    this.defaultValue = annotation.defaultValue();
    this.description = annotation.description();
    this.type = annotation.type();
    this.options = annotation.options();
  }

  public static PropertyFieldDefinition create(PropertyField annotation) {
    return new PropertyFieldDefinition(annotation);
  }

  public static PropertyFieldDefinition[] create(PropertyField[] fields) {
    PropertyFieldDefinition[] definitions = new PropertyFieldDefinition[fields.length];

    for (int i = 0; i < fields.length; i++) {
      definitions[i] = create(fields[i]);
    }

    return definitions;
  }

  public String getKey() {
    return key;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String getName() {
    return name;
  }

  public PropertyType getType() {
    return type;
  }

  public String[] getOptions() {
    return options.clone();
  }

  public String getDescription() {
    return description;
  }

  public PropertyDefinition.Result validate(@Nullable String value) {
    return PropertyDefinition.validate(type, value, options);
  }
}
