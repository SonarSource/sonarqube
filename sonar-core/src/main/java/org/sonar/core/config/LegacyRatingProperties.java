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
package org.sonar.core.config;

import java.util.Collections;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static org.sonar.core.config.LegacyRatingConstants.LEGACY_RATING_CATEGORY;
import static org.sonar.core.config.LegacyRatingConstants.LEGACY_RATING_MODE_ENABLED;
import static org.sonar.core.config.LegacyRatingConstants.LEGACY_RATING_SUB_CATEGORY;

public final class LegacyRatingProperties {

  private LegacyRatingProperties() {
  }

  public static List<PropertyDefinition> all() {
    return Collections.singletonList(
      PropertyDefinition.builder(LEGACY_RATING_MODE_ENABLED)
        .defaultValue(Boolean.FALSE.toString())
        .name("Enable legacy mode")
        .description("Ratings have updated logic and have grades ranging from A to D, while the old scale ranges from A to E " +
          "(<a href=\"https://docs.sonarsource.com/sonarqube/latest/user-guide/code-metrics/metrics-definition/\">read more about why</a>)." +
          "<br><br>" +
          "If you choose legacy mode, ratings and other counts will be calculated using the former logic. The old ratings scale (A-E) is " +
          "deprecated and is scheduled for replacement by the new scale (A-D) in the next LTA.")
        .type(PropertyType.BOOLEAN)
        .category(LEGACY_RATING_CATEGORY)
        .subCategory(LEGACY_RATING_SUB_CATEGORY)
        .index(1)
        .build()
    );

  }
}
