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
import com.google.common.collect.Lists;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class CorePropertyDefinitions {

  private CorePropertyDefinitions() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    List<PropertyDefinition> defs = Lists.newArrayList();
    defs.addAll(IssueExclusionProperties.all());
    defs.addAll(ExclusionProperties.all());
    defs.addAll(SecurityProperties.all());
    defs.addAll(DebtProperties.all());

    defs.addAll(ImmutableList.of(
      // BATCH

      PropertyDefinition.builder(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY)
        .defaultValue("en")
        .name("Locale used for issue messages")
        .description("Deprecated property. Keep default value for backward compatibility.")
        .hidden()
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 1)
        .name("Period 1")
        .description("Period used to compare measures and track new violations. Values are : <ul class='bullet'><li>Number of days before " +
          "analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, for example 2010-12-25</li><li>'previous_analysis' to " +
          "compare to previous analysis</li><li>'previous_version' to compare to the previous version in the project history</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is " +
          " the first one available inside the corresponding time range. </p>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_1)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 2)
        .name("Period 2")
        .description("See the property 'Period 1'")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_2)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 3)
        .name("Period 3")
        .description("See the property 'Period 1'")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_3)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 4)
        .name("Period 4")
        .description("Period used to compare measures and track new violations. This property is specific to the project. Values are : " +
          "<ul class='bullet'><li>Number of days before analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, " +
          "for example 2010-12-25</li><li>'previous_analysis' to compare to previous analysis</li>" +
          "<li>'previous_version' to compare to the previous version in the project history</li><li>A version, for example 1.2</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is the first one available inside the corresponding time range. </p>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_4)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 5)
        .name("Period 5")
        .description("See the property 'Period 4'")
        .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_5)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build()
      ));
    return defs;
  }
}
