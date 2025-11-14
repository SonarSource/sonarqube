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
package org.sonar.core.config;

import java.util.Collections;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.UI_MODE;
import static org.sonar.core.config.MQRModeConstants.UI_MODE_SUB_CATEGORY;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

public final class MQRModeProperties {

  private MQRModeProperties() {
  }

  public static List<PropertyDefinition> all() {
    return Collections.singletonList(
      PropertyDefinition.builder(MULTI_QUALITY_MODE_ENABLED)
        .defaultValue(Boolean.toString(MULTI_QUALITY_MODE_DEFAULT_VALUE))
        .name("Enable Multi-Quality Rule Mode")
        .description("Aims to more accurately represent the impact software has on all software qualities. " +
                "It does this by mapping rules to every software quality they can impact, not just the one " +
                "they impact most significantly. Each rule has a separate severity " +
                "for the impact it has on each quality that it has been mapped to. \n" +
                "This approach focuses on ensuring the impact on all software qualities is clear, " +
                "not just the most severe one.")
        .type(PropertyType.BOOLEAN)
        .category(UI_MODE)
        .subCategory(UI_MODE_SUB_CATEGORY)
        .hidden()
        .index(1)
        .build()
    );

  }
}
