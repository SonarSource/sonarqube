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
package org.sonar.server.platform.db.migration.version.v202604;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202604 implements DbVersion {

  @Override
  @SuppressWarnings("java:S3937")
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2026_04_000, "Drop redundant unique index 'uniq_iss_key_sof_qual' on 'issues_impacts'", DropUniqueIndexOnIssuesImpacts.class)
      .add(2026_04_001, "Drop redundant unique index 'uniq_rul_uuid_sof_qual' on 'rules_default_impacts'", DropUniqueIndexOnRulesDefaultImpacts.class)
      .add(2026_04_002, "Rename index 'uniq_iss_key_sof_qual' to 'pk_issues_impacts' on Oracle", RenameIndexOnIssuesImpactsToPk.class)
      .add(2026_04_003, "Rename index 'uniq_rul_uuid_sof_qual' to 'pk_rules_default_impacts' on Oracle", RenameIndexOnRulesDefaultImpactsToPk.class)
      .add(2026_04_004, "Drop redundant index 'issue_changes_issue_key' on 'issue_changes'", DropRedundantIndexOnIssueChangesIssueKey.class)
      .add(2026_04_005, "Create index 'sca_issues_updated_at' on 'sca_issues.updated_at'", CreateIndexOnScaIssuesUpdatedAt.class)
      .add(2026_04_006, "Create index 'sca_releases_updated_at' on 'sca_releases.updated_at'", CreateIndexOnScaReleasesUpdatedAt.class)
      .add(2026_04_007, "Create index 'sca_issues_releases_updated_at' on 'sca_issues_releases.updated_at'", CreateIndexOnScaIssuesReleasesUpdatedAt.class)
      .add(2026_04_008, "Set portfolios as private when their matching component is private", SetPrivatePortfoliosFromPrivateComponents.class)
      .add(2026_04_009, "Decommission 'Sonar way for AI Code' quality gate", DecommissionSonarWayForAiCodeQualityGate.class)
      .add(2026_04_010, "Add organization_uuid to sca_encountered_licenses and update unique index", AddOrganizationUuidToScaEncounteredLicenses.class)
      .add(2026_04_011, "Create table 'architecture_models'", CreateArchitectureModelsTable.class)
      .add(2026_04_012, "Drop table 'architecture_graphs'", DropArchitectureGraphsTable.class)
      .add(2026_04_013, "Add architectureadmin permission to existing project admins", AddArchitectureAdminPermissionToProjectAdmins.class);
  }
}
