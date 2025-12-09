/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.rule;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.sonar.api.utils.Preconditions.checkArgument;

public class RuleDescriptionSectionContextDto {

  static final String KEY_MUST_BE_SET_ERROR = "key must be set";
  static final String DISPLAY_NAME_MUST_BE_SET_ERROR = "displayName must be set";
  private final String key;
  private final String displayName;

  private RuleDescriptionSectionContextDto(String key, String displayName) {
    checkArgument(isNotEmpty(key), KEY_MUST_BE_SET_ERROR);
    checkArgument(isNotEmpty(displayName), DISPLAY_NAME_MUST_BE_SET_ERROR);
    this.key = key;
    this.displayName = displayName;
  }

  public static RuleDescriptionSectionContextDto of(String key, String displayName) {
    return new RuleDescriptionSectionContextDto(key, displayName);
  }

  public String getKey() {
    return key;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleDescriptionSectionContextDto that = (RuleDescriptionSectionContextDto) o;
    return getKey().equals(that.getKey()) && getDisplayName().equals(that.getDisplayName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKey(), getDisplayName());
  }

  @Override
  public String toString() {
    return "RuleDescriptionSectionContextDto[" +
      "key='" + key + '\'' +
      ", displayName='" + displayName + '\'' +
      ']';
  }
}
