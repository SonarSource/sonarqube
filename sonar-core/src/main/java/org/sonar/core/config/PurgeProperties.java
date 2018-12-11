/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;

public final class PurgeProperties {

  private PurgeProperties() {
  }

  public static List<PropertyDefinition> all() {
    return asList(
      PropertyDefinition.builder(PurgeConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY)
        .defaultValue("24")
        .name("Keep only one analysis a day after")
        .description("After this number of hours, if there are several analyses during the same day, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(1)
        .build(),

      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK)
        .defaultValue("4")
        .name("Keep only one analysis a week after")
        .description("After this number of weeks, if there are several analyses during the same week, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(2)
        .build(),

      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH)
        .defaultValue("52")
        .name("Keep only one analysis a month after")
        .description("After this number of weeks, if there are several analyses during the same month, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(3)
        .build(),


      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ANALYSES_WITH_VERSION)
        .defaultValue("104")
        .name("Keep only analyses with a version event after")
        .description("After this number of weeks, the DbCleaner keeps only analyses with a version event associated.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(4)
        .build(),

      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS)
        .defaultValue("260")
        .name("Delete all analyses after")
        .description("After this number of weeks, all analyses are fully deleted.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(5)
        .build(),

      PropertyDefinition.builder(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES)
        .defaultValue("30")
        .name("Delete closed issues after")
        .description("Issues that have been closed for more than this number of days will be deleted.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(6)
        .build()
      );
  }
}
