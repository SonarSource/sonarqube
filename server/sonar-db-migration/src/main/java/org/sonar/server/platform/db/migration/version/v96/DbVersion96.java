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
package org.sonar.server.platform.db.migration.version.v96;

import org.sonar.server.platform.db.migration.step.ForceReloadingOfAllPlugins;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion96 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6500, "remove root column from users table", DropRootColumnFromUsersTable.class)
      .add(6501, "Add columns 'context_key' and 'context_display_name' into rule_desc_sections", AddContextColumnsToRuleDescSectionsTable.class)
      .add(6502, "Drop unique index uniq_rule_desc_sections_kee", DropIndexForRuleDescSection.class)
      .add(6503, "Create unique uniq_rule_desc_sections", CreateIndexForRuleDescSections.class)
      .add(6504, "Add column 'expiration_date' to 'user_tokens'", AddExpirationDateColumnToUserTokens.class)
      .add(6505, "Add column 'rule_description_context_key' to 'issues'", AddRuleDescriptionContextKeyInIssuesTable.class)
      .add(6506, "Add column 'education_principles' to 'rules'", AddEducationPrinciplesColumnToRuleTable.class)
      .add(6507, "Overwrite plugin file hash to force reloading rules", ForceReloadingOfAllPlugins.class)
      .add(6508, "Migrate 'sonarlint_ad_seen' from users to properties", MigrateSonarlintAdSeenFromUsersToProperties.class)
      .add(6509, "Drop column sonarlint_ad_seen in 'users'", DropSonarlintAdSeenColumnInUsersTable.class)
      .add(6510, "Create table 'push_events'", CreatePushEventsTable.class)
      .add(6511, "Create index 'idx_push_even_crea_uuid_proj' on 'push_events'", CreateIndexForPushEvents.class)
      .add(6512, "Add column 'language' to 'push_events'", AddLanguageColumnToPushEventsTable.class)
      .add(6513, "Delete duplicated rows in 'project_badge_token'", DeleteDuplicatedProjectBadgeTokens.class)
      .add(6514, "Add unique index on 'project_uuid' in 'project_badge_token'", CreateIndexForProjectBadgeTokens.class)

    ;
  }
}
