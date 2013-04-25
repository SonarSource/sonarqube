/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.config;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.AnnotationUtils;

import javax.annotation.Nullable;

import java.util.*;

/**
 * Metadata of all the properties declared by plugins
 *
 * @since 2.12
 */
public final class PropertyDefinitions implements BatchComponent, ServerComponent {

  private final Map<String, PropertyDefinition> definitions = Maps.newHashMap();
  private final Map<String, String> categories = Maps.newHashMap();
  private final Map<String, String> subcategories = Maps.newHashMap();

  // deprecated key -> new key
  private final Map<String, String> deprecatedKeys = Maps.newHashMap();

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
    addComponentFromAnnotationProperty(component, defaultCategory);
    if (component instanceof PropertyDefinition) {
      PropertyDefinition propertyDefinition = (PropertyDefinition) component;
      add(propertyDefinition, defaultCategory);
    }
    return this;
  }

  private PropertyDefinitions addComponentFromAnnotationProperty(Object component, String defaultCategory) {
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
    if (!definitions.containsKey(definition.key())) {
      definitions.put(definition.key(), definition);
      categories.put(definition.key(), StringUtils.defaultIfBlank(definition.category(), defaultCategory));
      subcategories.put(definition.key(), StringUtils.defaultIfBlank(definition.subcategory(), "default"));
      if (!Strings.isNullOrEmpty(definition.deprecatedKey()) && !definition.deprecatedKey().equals(definition.key())) {
        deprecatedKeys.put(definition.deprecatedKey(), definition.key());
      }
    }
    return this;
  }

  public PropertyDefinition get(String key) {
    return definitions.get(validKey(key));
  }

  public Collection<PropertyDefinition> getAll() {
    return definitions.values();
  }

  public String validKey(String key) {
    return StringUtils.defaultString(deprecatedKeys.get(key), key);
  }

  /**
   * @since 3.6
   */
  public Map<String, Map<String, Collection<PropertyDefinition>>> getPropertiesByCategory(@Nullable String qualifier) {
    Map<String, Map<String, Collection<PropertyDefinition>>> byCategory = new HashMap<String, Map<String, Collection<PropertyDefinition>>>();

    for (PropertyDefinition definition : getAll()) {
      if (qualifier == null ? definition.global() : definition.qualifiers().contains(qualifier)) {
        String category = getCategory(definition.key());
        if (!byCategory.containsKey(category)) {
          byCategory.put(category, new HashMap<String, Collection<PropertyDefinition>>());
        }
        String subCategory = getSubCategory(definition.key());
        if (!byCategory.get(category).containsKey(subCategory)) {
          byCategory.get(category).put(subCategory, new ArrayList<PropertyDefinition>());
        }
        byCategory.get(category).get(subCategory).add(definition);
      }
    }

    return byCategory;
  }

  public Map<String, Map<String, Collection<PropertyDefinition>>> getPropertiesByCategory() {
    return getPropertiesByCategory(null);
  }

  public String getDefaultValue(String key) {
    PropertyDefinition def = get(key);
    if (def == null) {
      return null;
    }
    return StringUtils.defaultIfEmpty(def.defaultValue(), null);
  }

  public String getCategory(String key) {
    return categories.get(validKey(key));
  }

  public String getSubCategory(String key) {
    return subcategories.get(validKey(key));
  }

  public String getCategory(Property prop) {
    return getCategory(prop.key());
  }

  public String getNewKey(String deprecatedKey) {
    return deprecatedKeys.get(deprecatedKey);
  }

  public String getDeprecatedKey(String key) {
    PropertyDefinition def = get(key);
    if (def == null) {
      return null;
    }
    return StringUtils.defaultIfEmpty(def.deprecatedKey(), null);
  }
}
