/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.AnnotationUtils;

import static java.util.Objects.requireNonNull;

/**
 * Metadata of all the properties declared by plugins
 *
 * @since 2.12
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public final class PropertyDefinitions {

  private final Map<String, PropertyDefinition> definitions = new HashMap<>();
  private final Map<String, Category> categories = new HashMap<>();
  private final Map<String, SubCategory> subcategories = new HashMap<>();

  // deprecated key -> new key
  private final Map<String, String> deprecatedKeys = new HashMap<>();

  public PropertyDefinitions(Object... components) {
    addComponents(Arrays.asList(components));
  }

  public PropertyDefinitions(Collection<PropertyDefinition> components) {
    addComponents(components);
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
      String category = StringUtils.defaultIfBlank(definition.category(), defaultCategory);
      categories.put(definition.key(), new Category(category));
      String subcategory = StringUtils.defaultIfBlank(definition.subCategory(), category);
      subcategories.put(definition.key(), new SubCategory(subcategory));
      if (!StringUtils.isEmpty(definition.deprecatedKey()) && !definition.deprecatedKey().equals(definition.key())) {
        deprecatedKeys.put(definition.deprecatedKey(), definition.key());
      }
    }
    return this;
  }

  @CheckForNull
  public PropertyDefinition get(String key) {
    return definitions.get(validKey(key));
  }

  public Collection<PropertyDefinition> getAll() {
    return definitions.values();
  }

  public String validKey(String key) {
    requireNonNull(key, "key can't be null");
    String trimmedKey = key.trim();
    return StringUtils.defaultString(deprecatedKeys.get(trimmedKey), trimmedKey);
  }

  /**
   * @since 3.7
   */
  public Map<Category, Map<SubCategory, Collection<PropertyDefinition>>> propertiesByCategory(@Nullable String qualifier) {
    Map<Category, Map<SubCategory, Collection<PropertyDefinition>>> byCategory = new HashMap<>();
    if (qualifier == null) {
      // Special categories on global page
      Map<SubCategory, Collection<PropertyDefinition>> emailSubCategories = new HashMap<>();
      emailSubCategories.put(new SubCategory("email", true), new ArrayList<PropertyDefinition>());
      byCategory.put(new Category(CoreProperties.CATEGORY_GENERAL, false), emailSubCategories);

      HashMap<SubCategory, Collection<PropertyDefinition>> licenseSubCategories = new HashMap<>();
      licenseSubCategories.put(new SubCategory("server_id", true), new ArrayList<PropertyDefinition>());
      byCategory.put(new Category(CoreProperties.CATEGORY_LICENSES, false), licenseSubCategories);

      HashMap<SubCategory, Collection<PropertyDefinition>> encryptionSubCategories = new HashMap<>();
      encryptionSubCategories.put(new SubCategory("encryption", true), new ArrayList<PropertyDefinition>());
      byCategory.put(new Category(CoreProperties.CATEGORY_SECURITY, false), encryptionSubCategories);
    }
    for (PropertyDefinition definition : getAll()) {
      if (qualifier == null ? definition.global() : definition.qualifiers().contains(qualifier)) {
        Category category = categories.get(definition.key());
        if (!byCategory.containsKey(category)) {
          byCategory.put(category, new HashMap<SubCategory, Collection<PropertyDefinition>>());
        }
        SubCategory subCategory = subcategories.get(definition.key());
        if (!byCategory.get(category).containsKey(subCategory)) {
          byCategory.get(category).put(subCategory, new ArrayList<PropertyDefinition>());
        }
        byCategory.get(category).get(subCategory).add(definition);
      }
    }
    return byCategory;
  }

  @CheckForNull
  public String getDefaultValue(String key) {
    PropertyDefinition def = get(key);
    if (def == null) {
      return null;
    }
    return StringUtils.defaultIfEmpty(def.defaultValue(), null);
  }

  public String getCategory(String key) {
    return categories.get(validKey(key)).toString();
  }

  public String getSubCategory(String key) {
    return subcategories.get(validKey(key)).toString();
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
