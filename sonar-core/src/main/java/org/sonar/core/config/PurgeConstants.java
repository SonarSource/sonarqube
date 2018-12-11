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

public interface PurgeConstants {

  String HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY = "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay";
  String WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK = "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByWeek";
  String WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH = "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByMonth";
  String WEEKS_BEFORE_KEEPING_ONLY_ANALYSES_WITH_VERSION = "sonar.dbcleaner.weeksBeforeKeepingOnlyAnalysesWithVersion";
  String WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS = "sonar.dbcleaner.weeksBeforeDeletingAllSnapshots";
  String DAYS_BEFORE_DELETING_CLOSED_ISSUES = "sonar.dbcleaner.daysBeforeDeletingClosedIssues";
  String DAYS_BEFORE_DELETING_INACTIVE_SHORT_LIVING_BRANCHES = "sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches";
}
