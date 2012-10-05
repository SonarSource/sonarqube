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

import com.google.common.collect.Lists;
import org.sonar.api.PropertyField;
import org.sonar.api.PropertyType;

import javax.annotation.Nullable;

import java.util.List;

/**
 * @since 3.3
 */
public final class PropertyFieldDefinition {
  private final String key;
  private final String name;
  private final String description;
  private final int indicativeSize;
  private final PropertyType type;
  private final String[] options;

  private PropertyFieldDefinition(PropertyField annotation) {
    this.key = annotation.key();
    this.name = annotation.name();
    this.description = annotation.description();
    this.indicativeSize = annotation.indicativeSize();
    this.type = annotation.type();
    this.options = annotation.options();
  }

  public static List<PropertyFieldDefinition> create(PropertyField[] fields) {
    List<PropertyFieldDefinition> definitions = Lists.newArrayList();
    for (PropertyField field : fields) {
      definitions.add(new PropertyFieldDefinition(field));
    }
    return definitions;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getIndicativeSize() {
    return indicativeSize;
  }

  public PropertyType getType() {
    return type;
  }

  public String[] getOptions() {
    return options.clone();
  }

  public PropertyDefinition.Result validate(@Nullable String value) {
    return PropertyDefinition.validate(type, value, options);
  }
}
