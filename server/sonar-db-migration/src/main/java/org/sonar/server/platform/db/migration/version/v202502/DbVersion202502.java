/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202502;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion202502 implements DbVersion {

  /**
   * We use the start of the 10.X cycle as an opportunity to align migration numbers with the SQ version number.
   * Please follow this pattern:
   * 2025_02_000
   * 2025_02_001
   * 2025_02_002
   */
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2025_02_000, "Drop 'issues_dependency' table", DropIssuesDependencyTable.class)
      .add(2025_02_001, "Drop 'cve_cwe' table", DropCveCweTable.class)
      .add(2025_02_002, "Drop 'cves' table", DropCvesTable.class)
      .add(2025_02_003, "Create table for SCA releases", CreateScaReleasesTable.class)
      .add(2025_02_004, "Create index for SCA releases component", CreateIndexOnScaReleasesComponent.class)
      .add(2025_02_005, "Create table for SCA dependencies", CreateScaDependenciesTable.class)
      .add(2025_02_006, "Create index for SCA dependencies to releases", CreateIndexOnScaDependenciesRelease.class)
      .add(2025_02_007, "Create table for SCA issues", CreateScaIssuesTable.class)
      .add(2025_02_008, "Create unique index on SCA issues", CreateUniqueIndexOnScaIssues.class)
      .add(2025_02_009, "Create table for vulnerability subtype of SCA issues", CreateScaVulnerabilityIssuesTable.class)
      .add(2025_02_010, "Create table for SCA issues to releases", CreateScaIssuesReleasesTable.class)
      .add(2025_02_011, "Create index for SCA issues to releases to SCA issues", CreateIndexOnScaIssuesReleasesScaIssueUuid.class)
      .add(2025_02_012, "Create index for SCA issues to releases to SCA releases", CreateIndexOnScaIssuesReleasesScaReleaseUuid.class)
      .add(2025_02_013, "Create unique index for SCA issues to releases", CreateUniqueIndexOnScaIssuesReleases.class)
      .add(2025_02_014, "Add new_in_pull_request column to SCA releases", AddNewInPullRequestToScaReleasesTable.class)
      .add(2025_02_015, "Add new_in_pull_request column to SCA dependencies", AddNewInPullRequestToScaDependenciesTable.class)
      .add(2025_02_016, "Insert default AI Codefix provider key and modelKey properties", InsertDefaultAiSuggestionProviderKeyAndModelKeyProperties.class)
      .add(2025_02_017, "Add table 'architecture_graphs'", CreateArchitectureGraphsTable.class)
      .add(2025_02_018, "Drop 'sca_releases_comp_uuid' index", DropIndexOnScaReleasesComponent.class)
      .add(2025_02_019, "Create 'sca_releases_comp_uuid_uuid' index", CreateIndexOnScaReleasesComponentUuid.class)
      .add(2025_02_020, "Add 'sca_dependencies.production_scope' column", AddProductionScopeToScaDependenciesTable.class)
      .add(2025_02_021, "Add declared_license_expression to SCA releases", AddDeclaredLicenseExpressionToScaReleasesTable.class);
    ;
  }
}
