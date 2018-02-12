/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v71;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion71 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2000, "Delete settings defined in sonar.properties from PROPERTIES table", DeleteSettingsDefinedInSonarDotProperties.class)
      .add(2001, "Add scope to rules", AddRuleScope.class)
      .add(2002, "Set rules scope to MAIN", SetRuleScopeToMain.class)
      .add(2003, "Make scope not nullable in rules", MakeScopeNotNullableInRules.class)
      .add(2004, "Use rule id in QPROFILE_CHANGES", UseRuleIdInQPChangesData.class)
      .add(2005, "Create table DEPRECATED_RULE_KEYS", CreateDeprecatedRuleKeysTable.class)
      .add(2006, "Clean orphans in Compute Engine child tables", CleanCeChildTablesOrphans.class)
      .add(2007, "Update PERMISSION_TEMPLATES.KEYS ", UpdatePermissionTooLongTemplateKeys.class)
    ;
  }
}
