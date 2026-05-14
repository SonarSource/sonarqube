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
package org.sonar.server.platform.db.migration.version.v202603;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202603 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2026_03_000, "Add epss_score, epss_percentile, known_exploited columns to 'sca_vulnerability_issues'", AddEpssColumnsToScaVulnerabilityIssues.class)
      .add(2026_03_001, "Alter 'sca_analyses.failed_reason' column type to clob", AlterScaAnalysesFailedReasonColumnToClob.class)
      .add(2026_03_002, "Add GitLab Planner role permission mapping", AddGitlabPlannerRolePermissionMapping.class)
      .add(2026_03_003, "Add GitLab Security Manager role permission mapping", AddGitlabSecurityManagerRolePermissionMapping.class)
      .add(2026_03_004, "Add 'component_uuid' column to 'sca_issues_releases' table", AddComponentUuidToScaIssuesReleases.class)
      .add(2026_03_005, "Backfill 'component_uuid' on 'sca_issues_releases' from 'sca_releases'", BackfillComponentUuidOnScaIssuesReleases.class)
      .add(2026_03_006, "Make 'sca_issues_releases.component_uuid' not nullable", MakeScaIssuesReleasesComponentUuidNotNullable.class)
      .add(2026_03_007, "Create 'admin_alert_status' table", CreateAdminAlertStatusTable.class)
      .add(2026_03_008, "Drop redundant unique index 'uniq_iss_key_sof_qual' on 'issues_impacts'", DropUniqueIndexOnIssuesImpacts.class)
      .add(2026_03_009, "Drop redundant unique index 'uniq_rul_uuid_sof_qual' on 'rules_default_impacts'", DropUniqueIndexOnRulesDefaultImpacts.class)
      .add(2026_03_010, "Rename index 'uniq_iss_key_sof_qual' to 'pk_issues_impacts' on Oracle", RenameIndexOnIssuesImpactsToPk.class)
      .add(2026_03_011, "Rename index 'uniq_rul_uuid_sof_qual' to 'pk_rules_default_impacts' on Oracle", RenameIndexOnRulesDefaultImpactsToPk.class);
  }
}
