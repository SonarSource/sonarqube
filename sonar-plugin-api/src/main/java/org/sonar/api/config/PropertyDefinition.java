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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerExtension;
import org.sonar.api.resources.Qualifiers;

import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 *
 * @since 3.0
 */
public final class PropertyDefinition implements BatchExtension, ServerExtension {

  private String key;
  private String defaultValue;
  private String name;
  private PropertyType type;
  private List<String> options;
  private String description;
  private String category;
  private List<String> qualifiers;
  private boolean global;
  private boolean multiValues;
  private String propertySetKey;
  private String deprecatedKey;
  private List<PropertyFieldDefinition> fields;
  /**
   * @since 3.6
   */
  private String subcategory;
  private int index;

  /**
   * @since 3.6
   */
  private PropertyDefinition(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.description = builder.description;
    this.defaultValue = builder.defaultValue;
    this.category = builder.category;
    this.subcategory = builder.subcategory;
    this.global = builder.global;
    this.type = builder.type;
    this.options = builder.options;
    this.multiValues = builder.multiValues;
    this.propertySetKey = builder.propertySetKey;
    this.fields = builder.fields;
    this.deprecatedKey = builder.deprecatedKey;
    this.qualifiers = builder.qualifiers;
    this.index = builder.index;
  }

  /**
   * @since 3.6
   */
  public static Builder build(String key) {
    return new Builder(key);
  }

  /**
   * @since 3.6
   */
  static PropertyDefinition create(Property annotation) {
    List<String> qualifiers = newArrayList();
    if (annotation.project()) {
      qualifiers.add(Qualifiers.PROJECT);
    }
    if (annotation.module()) {
      qualifiers.add(Qualifiers.MODULE);
    }
    return PropertyDefinition.build(annotation.key())
        .name(annotation.name())
        .defaultValue(annotation.defaultValue())
        .description(annotation.description())
        .global(annotation.global())
        .qualifiers(qualifiers)
        .category(annotation.category())
        .type(annotation.type())
        .options(annotation.options())
        .multiValues(annotation.multiValues())
        .propertySetKey(annotation.propertySetKey())
        .fields(PropertyFieldDefinition.create(annotation.fields()))
        .deprecatedKey(annotation.deprecatedKey())
        .index(annotation.index())
        .build();
  }

  public static Result validate(PropertyType type, @Nullable String value, List<String> options) {
    if (StringUtils.isNotBlank(value)) {
      if (type == PropertyType.BOOLEAN) {
        if (!StringUtils.equalsIgnoreCase(value, "true") && !StringUtils.equalsIgnoreCase(value, "false")) {
          return Result.newError("notBoolean");
        }
      } else if (type == PropertyType.INTEGER) {
        if (!NumberUtils.isDigits(value)) {
          return Result.newError("notInteger");
        }
      } else if (type == PropertyType.FLOAT) {
        try {
          Double.parseDouble(value);
        } catch (NumberFormatException e) {
          return Result.newError("notFloat");
        }
      } else if (type == PropertyType.SINGLE_SELECT_LIST) {
        if (!options.contains(value)) {
          return Result.newError("notInOptions");
        }
      }
    }
    return Result.SUCCESS;
  }

  public Result validate(@Nullable String value) {
    return validate(type, value, options);
  }

  /**
   * Unique key within all plugins. It's recommended to prefix the key by 'sonar.' and the plugin name. Examples :
   * 'sonar.cobertura.reportPath' and 'sonar.cpd.minimumTokens'.
   */
  public String key() {
    return key;
  }

  public String defaultValue() {
    return defaultValue;
  }

  public String name() {
    return name;
  }

  public PropertyType type() {
    return type;
  }

  /**
   * Options for *_LIST types
   * <p/>
   * Options for property of type PropertyType.SINGLE_SELECT_LIST</code>
   * For example {"property_1", "property_3", "property_3"}).
   * <p/>
   * Options for property of type PropertyType.METRIC</code>.
   * If no option is specified, any metric will match.
   * If options are specified, all must match for the metric to be displayed.
   * Three types of filter are supported <code>key:REGEXP</code>, <code>domain:REGEXP</code> and <code>type:comma_separated__list_of_types</code>.
   * For example <code>key:new_.*</code> will match any metric which key starts by <code>new_</code>.
   * For example <code>type:INT,FLOAT</code> will match any metric of type <code>INT</code> or <code>FLOAT</code>.
   * For example <code>type:NUMERIC</code> will match any metric of numerictype.
   */
  public List<String> options() {
    return options;
  }

  public String description() {
    return description;
  }

  public String category() {
    return category;
  }

  /**
   * @since 3.6
   */
  public String subcategory() {
    return subcategory;
  }

  /**
   * Qualifiers that can display this property
   *
   * @since 3.6
   */
  public List<String> qualifiers() {
    return qualifiers;
  }

  /**
   * Is the property displayed in global settings page ?
   */
  public boolean global() {
    return global;
  }

  /**
   * @since 3.3
   */
  public boolean multiValues() {
    return multiValues;
  }

  /**
   * @since 3.3
   */
  public String propertySetKey() {
    return propertySetKey;
  }

  /**
   * @since 3.3
   */
  public List<PropertyFieldDefinition> fields() {
    return fields;
  }

  /**
   * @since 3.4
   */
  public String deprecatedKey() {
    return deprecatedKey;
  }

  /**
   * @since 3.6
   */
  public int index() {
    return index;
  }

  public static final class Result {
    private static final Result SUCCESS = new Result(null);
    private String errorKey = null;

    @Nullable
    private Result(@Nullable String errorKey) {
      this.errorKey = errorKey;
    }

    private static Result newError(String key) {
      return new Result(key);
    }

    public boolean isValid() {
      return StringUtils.isBlank(errorKey);
    }

    @Nullable
    public String getErrorKey() {
      return errorKey;
    }
  }

  /**
   * @since 3.6
   */
  public static class Builder {
    private String key;
    private String name;
    private String description;
    private String defaultValue;
    private String category;
    private String subcategory;
    private List<String> qualifiers;
    private boolean global;
    private PropertyType type;
    private List<String> options;
    private boolean multiValues;
    private String propertySetKey;
    private List<PropertyFieldDefinition> fields;
    private String deprecatedKey;
    private int index;

    private Builder(String key) {
      this.key = key;
      this.name = "";
      this.description = "";
      this.defaultValue = "";
      this.category = "";
      this.subcategory = "default";
      this.propertySetKey = "";
      this.deprecatedKey = "";
      this.global = true;
      this.type = PropertyType.STRING;
      this.qualifiers = newArrayList();
      this.options = newArrayList();
      this.fields = newArrayList();
      this.index = 999;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder defaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder category(String category) {
      this.category = category;
      return this;
    }

    public Builder subcategory(String subcategory) {
      this.subcategory = subcategory;
      return this;
    }

    public Builder qualifiers(String... qualifiers) {
      this.qualifiers.addAll(newArrayList(qualifiers));
      return this;
    }

    public Builder qualifiers(List<String> qualifiers) {
      this.qualifiers.addAll(qualifiers);
      return this;
    }

    public Builder global(boolean global) {
      this.global = global;
      return this;
    }

    public Builder type(PropertyType type) {
      this.type = type;
      return this;
    }

    public Builder options(String... options) {
      this.options.addAll(ImmutableList.copyOf(options));
      return this;
    }

    public Builder options(List<String> options) {
      this.options.addAll(ImmutableList.copyOf(options));
      return this;
    }

    public Builder multiValues(boolean multiValues) {
      this.multiValues = multiValues;
      return this;
    }

    public Builder propertySetKey(String propertySetKey) {
      this.propertySetKey = propertySetKey;
      return this;
    }

    public Builder fields(PropertyFieldDefinition... fields) {
      this.fields.addAll(ImmutableList.copyOf(fields));
      return this;
    }

    public Builder fields(List<PropertyFieldDefinition> fields) {
      this.fields.addAll(ImmutableList.copyOf(fields));
      return this;
    }

    public Builder deprecatedKey(String deprecatedKey) {
      this.deprecatedKey = deprecatedKey;
      return this;
    }

    public Builder index(int index) {
      this.index = index;
      return this;
    }

    public PropertyDefinition build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Key must be set");
      fixType(key, type);
      return new PropertyDefinition(this);
    }

    private void fixType(String key, PropertyType type) {
      // Auto-detect passwords and licenses for old versions of plugins that
      // do not declare property types
      if (type == PropertyType.STRING) {
        if (StringUtils.endsWith(key, ".password.secured")) {
          this.type = PropertyType.PASSWORD;
        } else if (StringUtils.endsWith(key, ".license.secured")) {
          this.type = PropertyType.LICENSE;
        }
      }
    }
  }

}
