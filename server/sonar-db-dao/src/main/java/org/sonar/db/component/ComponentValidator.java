/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.component;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.core.component.ComponentKeys.MAX_COMPONENT_KEY_LENGTH;

public class ComponentValidator {
  // b_name column is 500 characters wide
  public static final int MAX_COMPONENT_NAME_LENGTH = 500;
  public static final int MAX_COMPONENT_DESCRIPTION_LENGTH = 2_000;
  private static final int MAX_COMPONENT_TAGS_LENGTH = 500;
  private static final int MAX_COMPONENT_QUALIFIER_LENGTH = 10;

  private ComponentValidator() {
    // prevent instantiation
  }

  public static String checkComponentName(String name) {
    checkArgument(!isNullOrEmpty(name), "Component name can't be empty");
    checkArgument(name.length() <= MAX_COMPONENT_NAME_LENGTH, "Component name length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      name.length(), MAX_COMPONENT_NAME_LENGTH, name);
    return name;
  }

  public static String checkComponentLongName(@Nullable String value) {
    if (value == null) {
      return null;
    }
    checkArgument(value.length() <= MAX_COMPONENT_NAME_LENGTH, "Component name length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      value.length(), MAX_COMPONENT_NAME_LENGTH, value);
    return value;
  }

  public static String checkDescription(@Nullable String value) {
    if (value == null) {
      return null;
    }

    checkArgument(value.length() <= MAX_COMPONENT_NAME_LENGTH, "Component description length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      value.length(), MAX_COMPONENT_DESCRIPTION_LENGTH, value);
    return value;
  }

  public static String checkTags(@Nullable String value) {
    if (value == null) {
      return null;
    }

    checkArgument(value.length() <= MAX_COMPONENT_NAME_LENGTH, "Component tags length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      value.length(), MAX_COMPONENT_TAGS_LENGTH, value);
    return value;
  }

  public static String checkComponentKey(String key) {
    checkArgument(!isNullOrEmpty(key), "Component key can't be empty");
    checkArgument(key.length() <= MAX_COMPONENT_KEY_LENGTH, "Component key length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      key.length(), MAX_COMPONENT_KEY_LENGTH, key);
    return key;
  }

  public static String checkComponentQualifier(String qualifier) {
    checkArgument(!isNullOrEmpty(qualifier), "Component qualifier can't be empty");
    checkArgument(qualifier.length() <= MAX_COMPONENT_QUALIFIER_LENGTH, "Component qualifier length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      qualifier.length(), MAX_COMPONENT_QUALIFIER_LENGTH, qualifier);
    return qualifier;
  }
}
