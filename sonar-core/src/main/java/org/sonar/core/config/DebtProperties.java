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

package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.measures.CoreMetrics;

class DebtProperties {

  private DebtProperties() {
    // only static stuff
  }

  static List<PropertyDefinition> all() {
    return ImmutableList.of(
      PropertyDefinition.builder(CoreProperties.HOURS_IN_DAY)
        .name("Number of working hours in a day")
        .type(PropertyType.INTEGER)
        .defaultValue("8")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("sqale.hoursInDay")
        .build(),

      PropertyDefinition.builder(CoreProperties.SIZE_METRIC)
        .defaultValue("" + CoreMetrics.NCLOC_KEY)
        .name("Size metric")
        .description("Metric used to estimate artifact's development cost.")
        .type(PropertyType.METRIC)
        .options("key:^(ncloc|complexity)$")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("sizeMetric")
        .build(),

      PropertyDefinition.builder(CoreProperties.DEVELOPMENT_COST)
        .defaultValue("" + CoreProperties.DEVELOPMENT_COST_DEF_VALUE)
        .name("Development cost")
        .description("Cost to develop one unit of code. If the unit is a line of code (LOC), and the cost to develop 1 LOC has been estimated at 30 minutes, " +
          "then the value of this property would be 30.")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("workUnitsBySizePoint")
        .build(),

      PropertyDefinition.builder(CoreProperties.RATING_GRID)
        .defaultValue("" + CoreProperties.RATING_GRID_DEF_VALUES)
        .name("Rating grid")
        .description("SQALE ratings range from A (very good) to E (very bad). The rating is determined by the value of the Technical Debt Ratio, " +
          "which compares the technical debt on a project to the cost it would take to rewrite the code from scratch. " +
          "The default values for A through D are 0.1,0.2,0.5,1. Anything over 1 is an E. " +
          "Example: assuming the size metric is lines of code (LOC), and the work unit is 30 (minutes to produce 1 LOC), " +
          "a project with a technical debt of 24,000 minutes for 2,500 LOC will have a technical debt ratio of 24000/(30 * 2,500) = 0.32. That yields a SQALE rating of C.")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("ratingGrid")
        .build(),

      PropertyDefinition.builder(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS)
        .name("Language specific parameters")
        .description("The parameters specified here for a given language will override the general parameters defined in this section.")
        .category(CoreProperties.CATEGORY_TECHNICAL_DEBT)
        .deprecatedKey("languageSpecificParameters")
        .fields(
          PropertyFieldDefinition.build(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY)
            .name("Language Key")
            .description("Ex: java, cs, cpp...")
            .type(PropertyType.STRING)
            .build(),
          PropertyFieldDefinition.build(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY)
            .name("Development cost")
            .description("If left blank, the generic value defined in this section will be used.")
            .type(PropertyType.FLOAT)
            .build(),
          PropertyFieldDefinition.build(CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY)
            .name("Size metric")
            .description("If left blank, the generic value defined in this section will be used.")
            .type(PropertyType.METRIC)
            .options("key:^(ncloc|complexity)$")
            .build()
        )
        .build()
      );
  }
}
