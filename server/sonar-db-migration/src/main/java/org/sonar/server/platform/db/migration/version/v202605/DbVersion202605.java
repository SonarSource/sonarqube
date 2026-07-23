/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202605 implements DbVersion {

  @Override
  @SuppressWarnings("java:S3937")
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2026_05_000, "Create table 'a3s_contexts'", CreateA3SContextsTable.class)
      .add(2026_05_001, "Create table 'a3s_context_items'", CreateA3SContextItemsTable.class)
      .add(2026_05_002, "Create table 'a3s_analysis_usages'", CreateA3SAnalysisUsagesTable.class)
      .add(2026_05_003, "Create table 'agent_schedules'", CreateAgentSchedulesTable.class)
      .add(2026_05_004, "Create table 'agent_sched_proc_issues'", CreateAgentScheduleProcessedIssuesTable.class)
      .add(2026_05_005, "Add 'reachability_analyzed' to 'sca_issues_releases'", AddReachabilityAnalyzedToScaIssuesReleases.class)
      .add(2026_05_006, "Seed global '*' row in 'agent_schedules'", SeedGlobalAgentSchedule.class)
      .add(2026_05_007, "Create 'issue_count_dimensions' table", CreateIssueCountDimensionsTable.class)
      .add(2026_05_008, "Create 'issue_count_history' table", CreateIssueCountHistoryTable.class)
      .add(2026_05_009, "Create 'measure_key_mapping' table", CreateMeasureKeyMappingTable.class)
      .add(2026_05_010, "Create 'measure_history' table", CreateMeasureHistoryTable.class);
  }
}
