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

public class PurgeConstants {

  public static final String HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY = "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay";
  public static final String WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK = "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByWeek";
  public static final String WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH = "sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByMonth";
  public static final String WEEKS_BEFORE_KEEPING_ONLY_ANALYSES_WITH_VERSION = "sonar.dbcleaner.weeksBeforeKeepingOnlyAnalysesWithVersion";
  public static final String WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS = "sonar.dbcleaner.weeksBeforeDeletingAllSnapshots";
  public static final String DAYS_BEFORE_DELETING_CLOSED_ISSUES = "sonar.dbcleaner.daysBeforeDeletingClosedIssues";
  public static final String DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS = "sonar.dbcleaner.daysBeforeDeletingInactiveBranchesAndPRs";
  public static final String BRANCHES_TO_KEEP_WHEN_INACTIVE = "sonar.dbcleaner.branchesToKeepWhenInactive";
  public static final String AUDIT_HOUSEKEEPING_FREQUENCY = "sonar.dbcleaner.auditHousekeeping";
  public static final String DAYS_BEFORE_DELETING_ANTICIPATED_TRANSITIONS = "sonar.dbcleaner.daysBeforeDeletingAnticipatedTransitions";

  private PurgeConstants() {
    //class cannot be instantiated
  }

}
