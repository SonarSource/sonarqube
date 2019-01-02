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
package org.sonar.db.event;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class EventValidator {
  public static final int MAX_NAME_LENGTH = 400;
  private static final int MAX_CATEGORY_LENGTH = 50;
  private static final int MAX_DESCRIPTION_LENGTH = 4000;

  private EventValidator() {
    // prevent instantiation
  }

  @CheckForNull
  static String checkEventName(@Nullable String name) {
    if (name == null) {
      return null;
    }
    checkArgument(name.length() <= MAX_NAME_LENGTH, "Event name length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      name.length(), MAX_NAME_LENGTH, name);
    return name;
  }

  @CheckForNull
  static String checkEventCategory(@Nullable String category) {
    if (category == null) {
      return null;
    }
    checkArgument(category.length() <= MAX_CATEGORY_LENGTH, "Event category length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      category.length(), MAX_CATEGORY_LENGTH, category);
    return category;
  }

  @CheckForNull
  static String checkEventDescription(@Nullable String description) {
    if (description == null) {
      return null;
    }
    checkArgument(description.length() <= MAX_DESCRIPTION_LENGTH, "Event description length (%s) is longer than the maximum authorized (%s). '%s' was provided.",
      description.length(), MAX_DESCRIPTION_LENGTH, description);
    return description;
  }
}
