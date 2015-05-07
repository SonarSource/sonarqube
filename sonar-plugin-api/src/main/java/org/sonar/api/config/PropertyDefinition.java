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
package org.sonar.api.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Qualifiers;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Declare a plugin property. Values are available at runtime through the component {@link Settings}.
 * <p/>
 * It's the programmatic alternative to the annotation {@link org.sonar.api.Property}. It is more
 * testable and adds new features like sub-categories and ordering.
 * <p/>
 * Example:
 * <pre><code>
 *   public class MyPlugin extends SonarPlugin {
 *     public List getExtensions() {
 *       return Arrays.asList(
 *         PropertyDefinition.builder("sonar.foo").name("Foo").build(),
 *         PropertyDefinition.builder("sonar.bar").name("Bar").defaultValue("123").type(PropertyType.INTEGER).build()
 *       );
 *     }
 *   }
 * </code></pre>
 * <p/>
 * Keys in localization bundles are:
 * <ul>
 * <li>{@code property.<key>.name} is the label of the property</li>
 * <li>{@code property.<key>.description} is the optional description of the property</li>
 * <li>{@code property.category.<category>} is the category label</li>
 * <li>{@code property.category.<category>.description} is the category description</li>
 * <li>{@code property.category.<category>.<subcategory>} is the sub-category label</li>
 * <li>{@code property.category.<category>.<subcategory>.description} is the sub-category description</li>
 * </ul>
 *
 * @since 3.6
 */
@BatchSide
@ServerSide
@ExtensionPoint
public final class PropertyDefinition {

  private String key;
  private String defaultValue;
  private String name;
  private PropertyType type;
  private List<String> options;
  private String description;
  /**
   * @see org.sonar.api.config.PropertyDefinition.Builder#category(String)
   */
  private String category;
  private List<String> qualifiers;
  private boolean global;
  private boolean multiValues;
  private String propertySetKey;
  private String deprecatedKey;
  private List<PropertyFieldDefinition> fields;
  /**
   * @see org.sonar.api.config.PropertyDefinition.Builder#subCategory(String)
   */
  private String subCategory;
  private int index;

  private PropertyDefinition(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.description = builder.description;
    this.defaultValue = builder.defaultValue;
    this.category = builder.category;
    this.subCategory = builder.subCategory;
    this.global = builder.global;
    this.type = builder.type;
    this.options = builder.options;
    this.multiValues = builder.multiValues;
    this.propertySetKey = builder.propertySetKey;
    this.fields = builder.fields;
    this.deprecatedKey = builder.deprecatedKey;
    this.qualifiers = builder.onQualifiers;
    this.qualifiers.addAll(builder.onlyOnQualifiers);
    this.index = builder.index;
  }

  public static Builder builder(String key) {
    return new Builder(key);
  }

  static PropertyDefinition create(Property annotation) {
    Builder builder = PropertyDefinition.builder(annotation.key())
      .name(annotation.name())
      .defaultValue(annotation.defaultValue())
      .description(annotation.description())
      .category(annotation.category())
      .type(annotation.type())
      .options(Arrays.asList(annotation.options()))
      .multiValues(annotation.multiValues())
      .propertySetKey(annotation.propertySetKey())
      .fields(PropertyFieldDefinition.create(annotation.fields()))
      .deprecatedKey(annotation.deprecatedKey());
    List<String> qualifiers = new ArrayList<>();
    if (annotation.project()) {
      qualifiers.add(Qualifiers.PROJECT);
    }
    if (annotation.module()) {
      qualifiers.add(Qualifiers.MODULE);
    }
    if (annotation.global()) {
      builder.onQualifiers(qualifiers);
    } else {
      builder.onlyOnQualifiers(qualifiers);
    }
    return builder.build();
  }

  public static Result validate(PropertyType type, @Nullable String value, List<String> options) {
    if (StringUtils.isNotBlank(value)) {
      if (type == PropertyType.BOOLEAN) {
        return validateBoolean(value);
      } else if (type == PropertyType.INTEGER) {
        return validateInteger(value);
      } else if (type == PropertyType.FLOAT) {
        return validateFloat(value);
      } else if (type == PropertyType.SINGLE_SELECT_LIST && !options.contains(value)) {
        return Result.newError("notInOptions");
      }
    }
    return Result.SUCCESS;
  }

  private static Result validateBoolean(String value) {
    if (!StringUtils.equalsIgnoreCase(value, "true") && !StringUtils.equalsIgnoreCase(value, "false")) {
      return Result.newError("notBoolean");
    }
    return Result.SUCCESS;
  }

  private static Result validateInteger(String value) {
    if (!NumberUtils.isDigits(value)) {
      return Result.newError("notInteger");
    }
    return Result.SUCCESS;
  }

  private static Result validateFloat(String value) {
    try {
      Double.parseDouble(value);
      return Result.SUCCESS;
    } catch (NumberFormatException e) {
      return Result.newError("notFloat");
    }
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

  /**
   * Category where the property appears in settings pages. By default equal to plugin name.
   */
  public String category() {
    return category;
  }

  /**
   * Sub-category where property appears in settings pages. By default sub-category is the category.
   */
  public String subCategory() {
    return subCategory;
  }

  /**
   * Qualifiers that can display this property
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

  public boolean multiValues() {
    return multiValues;
  }

  public String propertySetKey() {
    return propertySetKey;
  }

  public List<PropertyFieldDefinition> fields() {
    return fields;
  }

  public String deprecatedKey() {
    return deprecatedKey;
  }

  /**
   * Order to display properties in Sonar UI. When two properties have the same index then it is sorted by
   * lexicographic order of property name.
   */
  public int index() {
    return index;
  }

  @Override
  public String toString() {
    if (StringUtils.isEmpty(propertySetKey)) {
      return key;
    }
    return new StringBuilder().append(propertySetKey).append('|').append(key).toString();
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

  public static class Builder {
    private final String key;
    private String name = "";
    private String description = "";
    private String defaultValue = "";
    /**
     * @see PropertyDefinition.Builder#category(String)
     */
    private String category = "";
    /**
     * @see PropertyDefinition.Builder#subCategory(String)
     */
    private String subCategory = "";
    private List<String> onQualifiers = new ArrayList<>();
    private List<String> onlyOnQualifiers = new ArrayList<>();
    private boolean global = true;
    private PropertyType type = PropertyType.STRING;
    private List<String> options = new ArrayList<>();
    private boolean multiValues = false;
    private String propertySetKey = "";
    private List<PropertyFieldDefinition> fields = new ArrayList<>();
    private String deprecatedKey = "";
    private boolean hidden = false;
    private int index = 999;

    private Builder(String key) {
      this.key = key;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * @see PropertyDefinition#name()
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * @see PropertyDefinition#defaultValue()
     */
    public Builder defaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    /**
     * @see PropertyDefinition#category()
     */
    public Builder category(String category) {
      this.category = category;
      return this;
    }

    /**
     * @see PropertyDefinition#subCategory()
     */
    public Builder subCategory(String subCategory) {
      this.subCategory = subCategory;
      return this;
    }

    /**
     * The property will be available in General Settings AND in the components
     * with the given qualifiers.
     * <p/>
     * For example @{code onQualifiers(Qualifiers.PROJECT)} allows to configure the
     * property in General Settings and in Project Settings.
     * <p/>
     * See supported constant values in {@link Qualifiers}. By default property is available
     * only in General Settings.
     */
    public Builder onQualifiers(String first, String... rest) {
      this.onQualifiers.addAll(Lists.asList(first, rest));
      this.global = true;
      return this;
    }

    /**
     * The property will be available in General Settings AND in the components
     * with the given qualifiers.
     * <p/>
     * For example @{code onQualifiers(Arrays.asList(Qualifiers.PROJECT))} allows to configure the
     * property in General Settings and in Project Settings.
     * <p/>
     * See supported constant values in {@link Qualifiers}. By default property is available
     * only in General Settings.
     */
    public Builder onQualifiers(List<String> qualifiers) {
      this.onQualifiers.addAll(ImmutableList.copyOf(qualifiers));
      this.global = true;
      return this;
    }

    /**
     * The property will be available in the components
     * with the given qualifiers, but NOT in General Settings.
     * <p/>
     * For example @{code onlyOnQualifiers(Qualifiers.PROJECT)} allows to configure the
     * property in Project Settings only.
     * <p/>
     * See supported constant values in {@link Qualifiers}. By default property is available
     * only in General Settings.
     */
    public Builder onlyOnQualifiers(String first, String... rest) {
      this.onlyOnQualifiers.addAll(Lists.asList(first, rest));
      this.global = false;
      return this;
    }

    /**
     * The property will be available in the components
     * with the given qualifiers, but NOT in General Settings.
     * <p/>
     * For example @{code onlyOnQualifiers(Arrays.asList(Qualifiers.PROJECT))} allows to configure the
     * property in Project Settings only.
     * <p/>
     * See supported constant values in {@link Qualifiers}. By default property is available
     * only in General Settings.
     */
    public Builder onlyOnQualifiers(List<String> qualifiers) {
      this.onlyOnQualifiers.addAll(ImmutableList.copyOf(qualifiers));
      this.global = false;
      return this;
    }

    /**
     * @see org.sonar.api.config.PropertyDefinition#type()
     */
    public Builder type(PropertyType type) {
      this.type = type;
      return this;
    }

    public Builder options(String first, String... rest) {
      this.options.addAll(Lists.asList(first, rest));
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

    public Builder fields(PropertyFieldDefinition first, PropertyFieldDefinition... rest) {
      this.fields.addAll(Lists.asList(first, rest));
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

    /**
     * Flag the property as hidden. Hidden properties are not displayed in Settings pages
     * but allow plugins to benefit from type and default values when calling {@link Settings}.
     */
    public Builder hidden() {
      this.hidden = true;
      return this;
    }

    /**
     * Set the order index in Settings pages. A property with a lower index is displayed
     * before properties with higher index.
     */
    public Builder index(int index) {
      this.index = index;
      return this;
    }

    public PropertyDefinition build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Key must be set");
      fixType(key, type);
      Preconditions.checkArgument(onQualifiers.isEmpty() || onlyOnQualifiers.isEmpty(), "Cannot define both onQualifiers and onlyOnQualifiers");
      Preconditions.checkArgument(!hidden || (onQualifiers.isEmpty() && onlyOnQualifiers.isEmpty()), "Cannot be hidden and defining qualifiers on which to display");
      if (hidden) {
        global = false;
      }
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
