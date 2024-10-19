/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.rules.CleanCodeAttribute;

public enum CleanCodeAttributeRestEnum {
  CONVENTIONAL(CleanCodeAttribute.CONVENTIONAL),
  FORMATTED(CleanCodeAttribute.FORMATTED),
  IDENTIFIABLE(CleanCodeAttribute.IDENTIFIABLE),

  CLEAR(CleanCodeAttribute.CLEAR),
  COMPLETE(CleanCodeAttribute.COMPLETE),
  EFFICIENT(CleanCodeAttribute.EFFICIENT),
  LOGICAL(CleanCodeAttribute.LOGICAL),

  DISTINCT(CleanCodeAttribute.DISTINCT),
  FOCUSED(CleanCodeAttribute.FOCUSED),
  MODULAR(CleanCodeAttribute.MODULAR),
  TESTED(CleanCodeAttribute.TESTED),

  LAWFUL(CleanCodeAttribute.LAWFUL),
  RESPECTFUL(CleanCodeAttribute.RESPECTFUL),
  TRUSTWORTHY(CleanCodeAttribute.TRUSTWORTHY);

  private final CleanCodeAttribute cleanCodeAttribute;

  CleanCodeAttributeRestEnum(CleanCodeAttribute cleanCodeAttribute) {
    this.cleanCodeAttribute = cleanCodeAttribute;
  }

  public CleanCodeAttribute getCleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  @CheckForNull
  public static CleanCodeAttributeRestEnum from(@Nullable CleanCodeAttribute cleanCodeAttribute) {
    if (cleanCodeAttribute == null) {
      return null;
    }
    return Arrays.stream(CleanCodeAttributeRestEnum.values())
      .filter(cleanCodeAttributeRestEnum -> cleanCodeAttributeRestEnum.cleanCodeAttribute.equals(cleanCodeAttribute))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported clean code attribute: " + cleanCodeAttribute));
  }
}
