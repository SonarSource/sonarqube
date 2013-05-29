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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.period.DefaultPeriodCleaner;

import java.util.List;

@Properties({
  @Property(key = DbCleanerConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY, defaultValue = "24",
    name = "Number of hours before starting to keep only one snapshot per day",
    description = "After this number of hours, if there are several snapshots during the same day, "
      + "the DbCleaner keeps the most recent one and fully deletes the other ones.",
    global = true,
    project = true,
    type = PropertyType.INTEGER),
  @Property(key = DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK, defaultValue = "4",
    name = "Number of weeks before starting to keep only one snapshot per week",
    description = "After this number of weeks, if there are several snapshots during the same week, "
      + "the DbCleaner keeps the most recent one and fully deletes the other ones.",
    global = true,
    project = true,
    type = PropertyType.INTEGER),
  @Property(key = DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH, defaultValue = "52",
    name = "Number of weeks before starting to keep only one snapshot per month",
    description = "After this number of weeks, if there are several snapshots during the same month, "
      + "the DbCleaner keeps the most recent one and fully deletes the other ones.",
    global = true,
    project = true,
    type = PropertyType.INTEGER),
  @Property(key = DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS, defaultValue = "260",
    name = "Number of weeks before starting to delete all remaining snapshots",
    description = "After this number of weeks, all snapshots are fully deleted.",
    global = true,
    project = true,
    type = PropertyType.INTEGER),
  @Property(
    key = DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY,
    defaultValue = "true",
    name = "Clean history data of directories/packages",
    description = "If set to true, no history is kept at directory/package level. Setting this to false can cause database bloat.",
    global = true,
    project = true,
    module = false,
    type = PropertyType.BOOLEAN),
  @Property(
    key = DbCleanerConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES,
    defaultValue = "30",
    name = "Number of days before deleting closed issues",
    description = "Issues that have been closed for more than this number of days will be deleted.",
    global = true,
    project = true,
    type = PropertyType.INTEGER)
})
public final class DbCleanerPlugin extends SonarPlugin {

  public List getExtensions() {
    return ImmutableList.of(
      DefaultPeriodCleaner.class,
      DefaultPurgeTask.class,
      ProjectPurgePostJob.class);
  }
}
