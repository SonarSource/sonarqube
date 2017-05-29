/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v65;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion65 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1700, "Drop table AUTHORS", DropTableAuthors.class)
      .add(1701, "Add rules_profiles.is_built_in", AddBuiltInFlagToRulesProfiles.class)
      .add(1702, "Set rules_profiles.is_built_in to false", SetRulesProfilesIsBuiltInToFalse.class)
      .add(1703, "Make rules_profiles.is_built_in not null", MakeRulesProfilesIsBuiltInNotNullable.class)
      .add(1704, "Delete unused loaded_templates on quality profiles", DeleteLoadedTemplatesOnQProfiles.class)
      .add(1705, "Create table default_qprofiles", CreateTableDefaultQProfiles.class)
      .add(1706, "Populate table default_qprofiles", PopulateTableDefaultQProfiles.class)
      .add(1707, "Drop rules_profiles.is_default", DropIsDefaultColumnFromRulesProfiles.class)
      .add(1708, "Create table qprofiles", CreateTableQProfiles.class)
      .add(1709, "Populate table qprofiles", PopulateQProfiles.class);
  }
}
