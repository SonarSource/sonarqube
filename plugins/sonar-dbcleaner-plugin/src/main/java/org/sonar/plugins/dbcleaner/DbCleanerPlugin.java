/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner;

import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.plugins.dbcleaner.period.PeriodPurge;
import org.sonar.plugins.dbcleaner.purges.*;
import org.sonar.plugins.dbcleaner.runner.PurgeRunner;

import java.util.Arrays;
import java.util.List;

@Properties({
    @Property(key = DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK,
        defaultValue = DbCleanerConstants.ONE_MONTH, name = "Number of months before starting to keep only one snapshot by week",
        description = "After this number of months, if there are several snapshots during the same week, "
            + "the DbCleaner keeps the first one and fully delete the other ones.", global = true, project = true),
    @Property(key = DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH,
        defaultValue = DbCleanerConstants.ONE_YEAR, name = "Number of months before starting to keep only one snapshot by month",
        description = "After this number of months, if there are several snapshots during the same month, "
            + "the DbCleaner keeps the first one and fully delete the other ones.", global = true, project = true),
    @Property(key = DbCleanerConstants.MONTHS_BEFORE_DELETING_ALL_SNAPSHOTS, defaultValue = DbCleanerConstants.FIVE_YEARS,
        name = "Number of months before starting to delete all remaining snapshots",
        description = "After this number of months, all snapshots are fully deleted.", global = true, project = true)})
public final class DbCleanerPlugin implements Plugin {

  @Override
  public String toString() {
    return DbCleanerConstants.PLUGIN_NAME;
  }

  public String getKey() {
    return DbCleanerConstants.PLUGIN_KEY;
  }

  public String getName() {
    return DbCleanerConstants.PLUGIN_NAME;
  }

  public String getDescription() {
    return "The DbCleaner optimizes the Sonar DB performances by removing old and useless quality snapshots.";
  }

  public List getExtensions() {
    return Arrays.asList(
        // shared components
        DefaultPeriodCleaner.class,

        // purges
        PurgeOrphanResources.class, PurgeEntities.class, PurgeRuleMeasures.class, PurgeUnprocessed.class,
        PurgeDeletedResources.class, PurgeDeprecatedLast.class, UnflagLastDoublons.class, PurgeDisabledResources.class,
        PurgeResourceRoles.class, PurgeEventOrphans.class, PurgePropertyOrphans.class, PeriodPurge.class, PurgeDependencies.class,

        // post-job
        PurgeRunner.class);
  }
}
