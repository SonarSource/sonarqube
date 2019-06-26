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
package org.sonar.server.platform.db.migration.version.v70;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion70 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1930, "Add QUALITY_GATES.IS_BUILT_IN", AddIsBuiltInToQualityGates.class)
      .add(1931, "Populate QUALITY_GATES.IS_BUILT_IN", PopulateQualityGatesIsBuiltIn.class)
      .add(1932, "Make QUALITY_GATES.IS_BUILT_IN not null", MakeQualityGatesIsBuiltInNotNullable.class)
      .add(1933, "Remove quality gates loaded templates", RemoveQualityGateLoadedTemplates.class)
      .add(1934, "Rename quality gate \"SonarQube way\" to \"Sonar way\"", RenameOldSonarQubeWayQualityGate.class)
      .add(1935, "Drop LOADED_TEMPLATES table", DropLoadedTemplatesTable.class)

      // optimizations of migrations from 7.0 on project_measures and live_measures
      .add(1936, "Delete person measures", DeletePersonMeasures.class)
      .add(1937, "Drop index on project_measures.person_id", DropIndexOnPersonMeasures.class)
      .add(1938, "Create table live_measures", CreateTableLiveMeasures.class)
      .add(1939, "Add temporary index on SNAPSHOTS.ISLAST", AddSnapshotIsLastIndex.class)
      .add(1940," Create temporary table LIVE_MEASURES_P", CreateTempTableLiveMeasuresP.class)
      .add(1941, "Populate table live_measures", PopulateLiveMeasures.class)
      .add(1942, "Add live_measures.metric_id index", AddLiveMeasuresMetricIndex.class)
      .add(1943, "Drop temporary table LIVE_MEASURES_P", DropTempTableLiveMeasuresP.class)
      .add(1944, "Drop temporary index on SNAPSHOTS.ISLAST", DropSnapshotIsLastIndex.class)
      .add(1945, "Delete file measures", DeleteFileMeasures.class)

      .add(1946, "Create ORG_QUALITY_GATES table", CreateOrgQualityGatesTable.class)
      .add(1947, "Add ORGANIZATIONS.DEFAULT_QUALITY_GATE_UUID", AddDefaultQualityGateUuidToOrganizations.class)
      .add(1948, "Create QUALITY_GATES.UUID", AddUuidToQualityGates.class)
      .add(1949, "Populate QUALITY_GATES.UUID", PopulateUuidOnQualityGates.class)
      .add(1950, "Make QUALITY_GATES.UUID not nullable", MakeUuidNotNullableOnQualityGates.class)
      .add(1951, "Drop unique index on QUALITY_GATES.NAME", DropUniqueIndexOnQualityGatesName.class)
      .add(1952, "Create builtin quality gate if required", CreateBuiltInQualityGate.class)
      .add(1953, "Populate ORG_QUALITY_GATES table", PopulateOrgQualityGates.class)
      .add(1954, "Populate default quality gate on organization", PopulateDefaultQualityGate.class)
      .add(1955, "Associate existing quality gates to default organization", AssociateQualityGatesToDefaultOrganization.class)
      .add(1956, "Read 'sonar.qualitygate' setting and set the value to default organization", ReadGlobalSonarQualityGateSettingToDefaultOrg.class)
      .add(1957, "Delete 'sonar.qualitygate' setting at global level", DeleteGlobalSonarQualityGateSetting.class)
      .add(1958, "Make ORGANIZATIONS.DEFAULT_QUALITY_GATE_UUID not nullable", SetDefaultQualityGateUuidAsNotNullableInOrganizations.class)
      .add(1959, "Add users.homepage_type and users.homepage_parameter", AddHomepageToUsers.class)
    ;
  }
}
