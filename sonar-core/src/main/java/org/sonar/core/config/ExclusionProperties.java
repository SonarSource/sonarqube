/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class ExclusionProperties {

  private ExclusionProperties() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    return ImmutableList.of(

      // COVERAGE
      PropertyDefinition.builder(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_COVERAGE_EXCLUSIONS)
        .type(PropertyType.STRING)
        .multiValues(true)
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .build(),

      // FILES
      PropertyDefinition.builder(CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY)
        .name("Global Source File Exclusions")
        .multiValues(true)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_FILES_EXCLUSIONS)
        .index(0)
        .build(),
      PropertyDefinition.builder(CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY)
        .name("Global Test File Exclusions")
        .multiValues(true)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_FILES_EXCLUSIONS)
        .index(1)
        .build(),

      PropertyDefinition.builder(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY)
        .name("Source File Exclusions")
        .multiValues(true)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_FILES_EXCLUSIONS)
        .onQualifiers(Qualifiers.PROJECT)
        .index(0)
        .build(),

      PropertyDefinition.builder(CoreProperties.PROJECT_INCLUSIONS_PROPERTY)
        .name("Source File Inclusions")
        .multiValues(true)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_FILES_EXCLUSIONS)
        .onQualifiers(Qualifiers.PROJECT)
        .index(1)
        .build(),
      PropertyDefinition.builder(CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY)
        .name("Test File Exclusions")
        .multiValues(true)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_FILES_EXCLUSIONS)
        .onQualifiers(Qualifiers.PROJECT)
        .index(2)
        .build(),
      PropertyDefinition.builder(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY)
        .name("Test File Inclusions")
        .multiValues(true)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_FILES_EXCLUSIONS)
        .onQualifiers(Qualifiers.PROJECT)
        .index(3)
        .build()

    );
  }
}
