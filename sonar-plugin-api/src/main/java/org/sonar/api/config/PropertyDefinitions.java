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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.AnnotationUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Metadata of all the properties declared by plugins
 *
 * @since 2.12
 */
public final class PropertyDefinitions implements BatchComponent, ServerComponent {

  private Map<String, PropertyDefinition> definitions = Maps.newHashMap();
  private Map<String, String> categories = Maps.newHashMap();

  public PropertyDefinitions(Object... components) {
    if (components != null) {
      addComponents(Arrays.asList(components));
    }
  }

  public PropertyDefinitions addComponents(Collection components) {
    return addComponents(components, "");
  }

  public PropertyDefinitions addComponents(Collection components, String defaultCategory) {
    for (Object component : components) {
      addComponent(component, defaultCategory);
    }
    return this;
  }

  public PropertyDefinitions addComponent(Object object) {
    return addComponent(object, "");
  }

  public PropertyDefinitions addComponent(Object component, String defaultCategory) {
    Properties annotations = AnnotationUtils.getAnnotation(component, Properties.class);
    if (annotations != null) {
      for (Property property : annotations.value()) {
        addProperty(property, defaultCategory);
      }
    }
    Property annotation = AnnotationUtils.getAnnotation(component, Property.class);
    if (annotation != null) {
      addProperty(annotation, defaultCategory);
    }
    return this;
  }

  private PropertyDefinitions addProperty(Property property, String defaultCategory) {
    PropertyDefinition definition = PropertyDefinition.create(property);
    return add(definition, defaultCategory);
  }

  private PropertyDefinitions add(PropertyDefinition definition, String defaultCategory) {
    if (!definitions.containsKey(definition.getKey())) {
      definitions.put(definition.getKey(), definition);
      categories.put(definition.getKey(), StringUtils.defaultIfBlank(definition.getCategory(), defaultCategory));
    }
    return this;
  }

  public PropertyDefinition get(String key) {
    return definitions.get(key);
  }

  public Collection<PropertyDefinition> getAll() {
    return definitions.values();
  }

  public String getDefaultValue(String key) {
    PropertyDefinition def = get(key);
    if (def != null) {
      return StringUtils.defaultIfEmpty(def.getDefaultValue(), null);
    }
    return null;
  }

  public String getCategory(String key) {
    return categories.get(key);
  }

  public String getCategory(Property prop) {
    return getCategory(prop.key());
  }
}
