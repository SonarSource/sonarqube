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
package org.sonar.server.platform.db.migration.version.v72;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion72 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2100, "Increase size of USERS.CRYPTED_PASSWORD", IncreaseCryptedPasswordSize.class)
      .add(2101, "Add HASH_METHOD to table users", AddHashMethodToUsersTable.class)
      .add(2102, "Populate HASH_METHOD on table users", PopulateHashMethodOnUsers.class)
      .add(2103, "Add isExternal boolean to rules", AddRuleExternal.class)
      .add(2104, "Create ALM_APP_INSTALLS table", CreateAlmAppInstallsTable.class)
      .add(2105, "Add LINE_HASHES_VERSION to table FILE_SOURCES", AddLineHashesVersionToFileSources.class)
      .add(2106, "Create PROJECT_MAPPINGS table", CreateProjectMappingsTable.class)
      .add(2107, "Add UUID on table USERS", AddUUIDtoUsers.class)
      .add(2108, "Populate USERS.UUID with USERS.LOGIN", PopulateUUIDOnUsers.class)
      .add(2109, "Add EXTERNAL_ID on table users", AddExternalIdToUsers.class)
      .add(2110, "Rename EXTERNAL_IDENTITY to EXTERNAL_LOGIN on table users", RenameExternalIdentityToExternalLoginOnUsers.class)
      .add(2111, "Update null values from external columns and login of users", UpdateNullValuesFromExternalColumnsAndLoginOfUsers.class)
      .add(2112, "Populate EXTERNAL_ID on table users", PopulateExternalIdOnUsers.class)
      .add(2113, "Makes same columns of table users not nullable", MakeSomeColumnsOfUsersNotNullable.class)
      .add(2114, "Add unique indexes on table users", AddUniqueIndexesOnUsers.class)
      .add(2115, "Add ORGANIZATION_UUID on table users", AddOrganizationUuidToUsers.class)
      .add(2116, "Populate ORGANIZATION_UUID in table users", PopulateOrganizationUuidOnUsers.class)
      .add(2117, "Drop USER_ID from table organizations", DropUserIdFromOrganizations.class)
      .add(2118, "Rename USER_LOGIN TO USER_UUID on table QPROFILE_CHANGES", RenameUserLoginToUserUuidOnTableQProfileChanges.class)
      .add(2119, "Rename LOGIN TO USER_UUID on table USER_TOKENS", RenameLoginToUserUuidOnTableUserTokens.class)
      .add(2120, "Rename USER_LOGIN TO USER_UUID on table MANUAL_MEASURES", RenameUserLoginToUserUuidOnTableManualMeasures.class)
      .add(2121, "Rename NOTE_USER_LOGIN TO NOTE_USER_UUID on table RULES_METADATA", RenameNoteUserLoginToNoteUserUuidOnTableRulesMetadata.class)
      .add(2122, "Rename SUBMITTER_LOGIN TO SUBMITTER_UUID on table CE_QUEUE", RenameSubmitterLoginToSubmitterUuidOnTableCeQueue.class)
      .add(2123, "Rename SUBMITTER_LOGIN TO SUBMITTER_UUID on table CE_ACTIVITY", RenameSubmitterLoginToSubmitterUuidOnTableCeActivity.class)
      .add(2124, "Add FILE_SOURCE.LINE_COUNT", AddFileSourceLineCount.class)
      .add(2125, "Populate FILE_SOURCE.LINE_COUNT", PopulateFileSourceLineCount.class)
      .add(2126, "Make FILE_SOURCE.LINE_COUNT not nullable", MakeFileSourceLineCountNotNullable.class)
      .add(2127, "Purge orphans for Compute Engine", PurgeOrphansForCE.class)
      .add(2128, "Purge duplicate rules_parameters and their orphans", PurgeDuplicateRulesParameters.class)
      .add(2129, "Add unique index on rule_id + name on rules_parameters", AddUniqueIndexOnRulesParameters.class)
    ;
  }
}
