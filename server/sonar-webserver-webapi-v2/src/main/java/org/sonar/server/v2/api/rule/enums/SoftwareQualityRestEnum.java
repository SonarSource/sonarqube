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
import org.sonar.api.issue.impact.SoftwareQuality;

public enum SoftwareQualityRestEnum {
  MAINTAINABILITY(SoftwareQuality.MAINTAINABILITY),
  RELIABILITY(SoftwareQuality.RELIABILITY),
  SECURITY(SoftwareQuality.SECURITY);


  private final SoftwareQuality softwareQuality;

  SoftwareQualityRestEnum(SoftwareQuality softwareQuality) {

    this.softwareQuality = softwareQuality;
  }

  public SoftwareQuality getSoftwareQuality() {
    return softwareQuality;
  }

  public static SoftwareQualityRestEnum from(SoftwareQuality softwareQuality) {
    return Arrays.stream(SoftwareQualityRestEnum.values())
      .filter(softwareQualityRest -> softwareQualityRest.softwareQuality.equals(softwareQuality))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported SoftwareQuality: " + softwareQuality));
  }
}
