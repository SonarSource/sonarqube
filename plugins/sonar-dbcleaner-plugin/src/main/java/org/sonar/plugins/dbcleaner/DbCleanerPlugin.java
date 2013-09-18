/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.dbcleaner;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.period.DefaultPeriodCleaner;

import java.util.Arrays;
import java.util.List;

public final class DbCleanerPlugin extends SonarPlugin {

  public List getExtensions() {
    return ImmutableList.builder().add(DefaultPeriodCleaner.class, DefaultPurgeTask.class, ProjectPurgePostJob.class)
      .addAll(propertyDefinitions()).build();
  }

  static List<PropertyDefinition> propertyDefinitions() {
    return Arrays.asList(
      PropertyDefinition.builder(DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY)
        .defaultValue("true")
        .name("Clean history data of directories/packages")
        .description("If set to true, no history is kept at directory/package level. Setting this to false can cause database bloat.")
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(1)
        .build(),

      PropertyDefinition.builder(DbCleanerConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES)
        .defaultValue("30")
        .name("Number of days before deleting closed issues")
        .description("Issues that have been closed for more than this number of days will be deleted.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(2)
        .build(),

      PropertyDefinition.builder(DbCleanerConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY)
        .defaultValue("24")
        .name("Number of hours before starting to keep only one snapshot per day")
        .description("After this number of hours, if there are several snapshots during the same day, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(3)
        .build(),

      PropertyDefinition.builder(DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK)
        .defaultValue("4")
        .name("Number of weeks before starting to keep only one snapshot per week")
        .description("After this number of weeks, if there are several snapshots during the same week, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(4)
        .build(),

      PropertyDefinition.builder(DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH)
        .defaultValue("52")
        .name("Number of weeks before starting to keep only one snapshot per month")
        .description("After this number of weeks, if there are several snapshots during the same month, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(5)
        .build(),

      PropertyDefinition.builder(DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS)
        .defaultValue("260")
        .name("Number of weeks before starting to delete all remaining snapshots")
        .description("After this number of weeks, all snapshots are fully deleted.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(6)
        .build()
    );
  }
}
