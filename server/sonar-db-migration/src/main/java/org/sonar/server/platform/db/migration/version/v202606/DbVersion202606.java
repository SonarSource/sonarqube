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
package org.sonar.server.platform.db.migration.version.v202606;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202606 implements DbVersion {

  @Override
  @SuppressWarnings("java:S3937")
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2026_06_000, "Remove duplicate permission templates", RemoveDuplicatePermissionTemplates.class)
      .add(2026_06_001, "Enforce unique permission template names", EnforceUniquePermissionTemplateNames.class)
      .add(2026_06_002, "Add 'type' to 'sca_analyses'", AddTypeToScaAnalyses.class)
      .add(2026_06_003, "Create table 'sca_container_analyses'", CreateScaContainerAnalysesTable.class)
      .add(2026_06_004, "Create sca_available_fixes table", CreateScaAvailableFixesTable.class)
      .add(2026_06_005, "Migrate Anyone group permissions to sonar-users", MigrateAnyoneGroupPermissionsToSonarUsers.class)
      .add(2026_06_006, "Remove Anyone group permissions", RemoveAnyoneGroupPermissions.class)
      .add(2026_06_007, "Create table 'sca_ref_epss_scores'", CreateScaRefdbEpssScoresTable.class)
      .add(2026_06_008, "Create table 'sca_ref_kev_vulns'", CreateScaRefdbKevVulnerabilitiesTable.class)
      .add(2026_06_009, "Create table 'sca_ref_licensing_infos'", CreateScaRefdbLicensingInfosTable.class)
      .add(2026_06_010, "Create table 'sca_ref_osv_aff_ranges'", CreateScaRefdbOsvAffectedRangesTable.class)
      .add(2026_06_011, "Create table 'sca_ref_osv_aff_versions'", CreateScaRefdbOsvAffectedVersionsTable.class)
      .add(2026_06_012, "Create table 'sca_ref_osv_vulns'", CreateScaRefdbOsvVulnerabilitiesTable.class)
      .add(2026_06_013, "Create table 'sca_ref_reachability_defs'", CreateScaRefdbReachabilityDefinitionsTable.class)
      .add(2026_06_014, "Create table 'sca_ref_upstream_pkgs'", CreateScaRefdbUpstreamPackagesTable.class)
      .add(2026_06_015, "Create table 'sca_ref_upstream_releases'", CreateScaRefdbUpstreamReleasesTable.class)
      .add(2026_06_016, "Widen 'hunter_agent_jobs' commit SHA columns to 64 chars", WidenHunterAgentJobsCommitShaColumns.class)
      .add(2026_06_017, "Widen 'hunter_agent_runs.analyzed_commit_sha' to 64 chars", WidenHunterAgentRunsAnalyzedCommitShaColumn.class)
      .add(2026_06_018, "Widen 'remediation_agent_jobs.result_commit' to 64 chars", WidenRemediationAgentJobsResultCommitColumn.class)
      .add(2026_06_019, "Create partial index on 'issues' for from_sonarqube_update = true", CreateIndexOnIssuesFromSonarQubeUpdate.class)
      .add(2026_06_020, "Create OAuth registered clients", CreateOAuthRegisteredClientsTable.class)
      .add(2026_06_021, "Create OAuth authorizations", CreateOAuthAuthorizationsTable.class)
      .add(2026_06_022, "Create OAuth authorization consent", CreateOAuthAuthorizationConsentsTable.class);
  }
}

