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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

import javax.annotation.Nullable;

/**
 * @since 2.15
 */
public final class PropertyDefinition {

  public static final class Result {
    private static final Result SUCCESS = new Result(null);

    private String errorKey = null;

    private static Result newError(String key) {
      return new Result(key);
    }

    @Nullable
    private Result(@Nullable String errorKey) {
      this.errorKey = errorKey;
    }

    public boolean isValid() {
      return StringUtils.isBlank(errorKey);
    }

    @Nullable
    public String getErrorKey() {
      return errorKey;
    }
  }

  private String key;
  private String defaultValue;
  private String name;
  private PropertyType type = PropertyType.STRING;
  private String[] options;
  private String description;
  private String category;
  private boolean onProject = false;
  private boolean onModule = false;
  private boolean isGlobal = true;

  private PropertyDefinition(Property annotation) {
    this.key = annotation.key();
    this.name = annotation.name();
    this.defaultValue = annotation.defaultValue();
    this.description = annotation.description();
    this.isGlobal = annotation.global();
    this.onProject = annotation.project();
    this.onModule = annotation.module();
    this.category = annotation.category();
    this.type = fixType(annotation.key(), annotation.type());
    this.options = annotation.options();
  }

  private PropertyType fixType(String key, PropertyType type) {
    // Auto-detect passwords and licenses for old versions of plugins that
    // do not declare property types
    PropertyType fix = type;
    if (type == PropertyType.STRING) {
      if (StringUtils.endsWith(key, ".password.secured")) {
        fix = PropertyType.PASSWORD;
      } else if (StringUtils.endsWith(key, ".license.secured")) {
        fix = PropertyType.LICENSE;
      }
    }
    return fix;
  }

  private PropertyDefinition(String key, PropertyType type, String[] options) {
    this.key = key;
    this.type = type;
    this.options = options;
  }

  public static PropertyDefinition create(Property annotation) {
    return new PropertyDefinition(annotation);
  }

  public static PropertyDefinition create(String key, PropertyType type, String[] options) {
    return new PropertyDefinition(key, type, options);
  }

  public Result validate(@Nullable String value) {
    // TODO REFACTORING REQUIRED HERE
    Result result = Result.SUCCESS;
    if (StringUtils.isNotBlank(value)) {
      if (type == PropertyType.BOOLEAN) {
        if (!StringUtils.equalsIgnoreCase(value, "true") && !StringUtils.equalsIgnoreCase(value, "false")) {
          result = Result.newError("notBoolean");
        }
      } else if (type == PropertyType.INTEGER) {
        if (!NumberUtils.isDigits(value)) {
          result = Result.newError("notInteger");
        }
      } else if (type == PropertyType.FLOAT) {
        try {
          Double.parseDouble(value);
        } catch (NumberFormatException e) {
          result = Result.newError("notFloat");
        }
      } else if (type == PropertyType.SINGLE_SELECT_LIST) {
        if (!ArrayUtils.contains(options, value)) {
          result = Result.newError("notInOptions");
        }
      }
    }
    return result;
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

  public String getCategory() {
    return category;
  }

  public boolean isOnProject() {
    return onProject;
  }

  public boolean isOnModule() {
    return onModule;
  }

  public boolean isGlobal() {
    return isGlobal;
  }
}
