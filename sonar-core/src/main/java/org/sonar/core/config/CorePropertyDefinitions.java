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
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.computation.dbcleaner.DataCleanerProperties;

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
    defs.addAll(DataCleanerProperties.all());

    defs.addAll(ImmutableList.of(
      // WEB LOOK&FEEL
      PropertyDefinition.builder("sonar.lf.logoUrl")
        .deprecatedKey("sonar.branding.image")
        .name("Logo URL")
        .description("URL to logo image. Any standard format is accepted.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.logoWidthPx")
        .deprecatedKey("sonar.branding.image.width")
        .name("Width of image in pixels")
        .description("Width in pixels, given that the height of the the image is constrained to 30px")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),

      // ISSUES
      PropertyDefinition.builder(CoreProperties.DEFAULT_ISSUE_ASSIGNEE)
        .name("Default Assignee")
        .description("New issues will be assigned to this user each time it is not possible to determine the user who is the author of the issue.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.USER_LOGIN)
        .build(),

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
        .build(),

      // CPD
      PropertyDefinition.builder(CoreProperties.CPD_CROSS_PROJECT)
        .defaultValue(CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE + "")
        .name("Cross project duplication detection")
        .description("By default, SonarQube detects duplications at sub-project level. This means that a block "
          + "duplicated on two sub-projects of the same project won't be reported. Setting this parameter to \"true\" "
          + "allows to detect duplicates across sub-projects and more generally across projects. Note that activating "
          + "this property will slightly increase each SonarQube analysis time.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_SKIP_PROPERTY)
        .defaultValue("false")
        .name("Skip")
        .description("Disable detection of duplications")
        .hidden()
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_EXCLUSIONS)
        .defaultValue("")
        .name("Duplication Exclusions")
        .description("Patterns used to exclude some source files from the duplication detection mechanism. " +
          "See below to know how to use wildcards to specify this property.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS_EXCLUSIONS)
        .multiValues(true)
        .build()
      ));
    return defs;
  }
}
