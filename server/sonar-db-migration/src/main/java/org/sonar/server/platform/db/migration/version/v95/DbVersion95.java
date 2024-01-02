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
package org.sonar.server.platform.db.migration.version.v95;

import org.sonar.server.platform.db.migration.step.ForceReloadingOfAllPlugins;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion95 implements DbVersion {
  static final String DEFAULT_DESCRIPTION_KEY = "default";

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6401, "Add column 'project_key' to 'user_tokens'", AddProjectKeyColumnToUserTokens.class)
      .add(6402, "Add column 'type' to 'user_tokens'", AddTypeColumnToUserTokens.class)
      .add(6403, "Upsert value of type in 'user_tokens'", UpsertUserTokensTypeValue.class)
      .add(6404, "Make column 'type' in 'user_tokens' not nullable", MakeTypeColumnNotNullableOnUserTokens.class)
      .add(6405, "Create table RULE_DESC_SECTIONS", CreateRuleDescSectionsTable.class)
      .add(6406, "Insert description from RULES into RULE_DESC_SECTIONS", InsertRuleDescriptionIntoRuleDescSections.class)
      .add(6407, "Create index for RULE_DESC_SECTIONS", CreateIndexForRuleDescSections.class)
      .add(6408, "Drop column DESCRIPTIONS from RULES table", DropRuleDescriptionColumn.class)
      .add(6409, "Drop column CREATED_AT from RULES_METADATA table", DropRuleMetadataCreatedAtColumn.class)
      .add(6410, "Drop column UPDATED_AT from RULES_METADATA table", DropRuleMetadataUpdatedAtColumn.class)
      .add(6411, "Overwrite plugin file hash to force reloading rules", ForceReloadingOfAllPlugins.class)

      .add(6412, "Add rules_metadata columns to rules table", AddRulesMetadataColumnsToRulesTable.class)
      .add(6413, "Populate rules metadata in rules table", PopulateRulesMetadataInRuleTable.class)
      .add(6414, "Drop rules_metadata table", DropRuleMetadataTable.class)

      .add(6415, "Migrate hotspot rule descriptions", MigrateHotspotRuleDescriptions.class)

      .add(6416, "Remove onboarded column from User table", DropOnboardedColumnFromUserTable.class)

      .add(6417, "Add column 'user_triggered' to 'audits'", AddUserTriggeredColumnToAudits.class)
      .add(6418, "Upsert value of 'user_triggered' in 'audits'", UpsertAuditsUserTriggeredValue.class)
    ;
  }
}
