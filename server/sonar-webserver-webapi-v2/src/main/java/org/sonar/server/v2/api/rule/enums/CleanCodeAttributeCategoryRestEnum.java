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
package org.sonar.server.v2.api.rule.enums;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rules.CleanCodeAttributeCategory;

public enum CleanCodeAttributeCategoryRestEnum {
  ADAPTABLE(CleanCodeAttributeCategory.ADAPTABLE),
  CONSISTENT(CleanCodeAttributeCategory.CONSISTENT),
  INTENTIONAL(CleanCodeAttributeCategory.INTENTIONAL),
  RESPONSIBLE(CleanCodeAttributeCategory.RESPONSIBLE);

  private final CleanCodeAttributeCategory cleanCodeAttributeCategory;

  CleanCodeAttributeCategoryRestEnum(CleanCodeAttributeCategory cleanCodeAttributeCategory) {
    this.cleanCodeAttributeCategory = cleanCodeAttributeCategory;
  }

  public CleanCodeAttributeCategory getCleanCodeAttributeCategory() {
    return cleanCodeAttributeCategory;
  }

  @CheckForNull
  public static CleanCodeAttributeCategoryRestEnum from(@Nullable CleanCodeAttributeCategory cleanCodeAttributeCategory) {
    if (cleanCodeAttributeCategory == null) {
      return null;
    }
    return Arrays.stream(CleanCodeAttributeCategoryRestEnum.values())
      .filter(cleanCodeAttributeCategoryRestEnum -> cleanCodeAttributeCategoryRestEnum.cleanCodeAttributeCategory.equals(cleanCodeAttributeCategory))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported clean code attribute category: " + cleanCodeAttributeCategory));
  }


}
