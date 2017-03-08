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

package org.sonar.server.platform.db.migration.version.v64;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion64 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1600, "Add PROJECTS.TAGS", AddTagsToProjects.class)
      .add(1601, "Set PROJECTS.COPY_COMPONENT_UUID on local views", SetCopyComponentUuidOnLocalViews.class)
      .add(1602, "Add RULES_PROFILES.ORGANIZATION_UUID", AddQualityProfileOrganizationUuid.class)
      .add(1603, "Set RULES_PROFILES.ORGANIZATION_UUID to default", SetQualityProfileOrganizationUuidToDefault.class)
      .add(1604, "Make RULES_PROFILES.ORGANIZATION_UUID not nullable", MakeQualityProfileOrganizationUuidNotNullable.class)

      .add(1605, "Drop unique index on RULES_PROFILES.KEE", DropUniqueIndexOnQualityProfileKey.class)
      .add(1606, "Make RULES_PROFILES.ORGANIZATION_UUID and KEE unique", MakeQualityProfileOrganizationUuidAndKeyUnique.class)
      .add(1607, "Drop unique index on RULES_PROFILES.ORGANIZATION_UUID and KEE", DropUniqueIndexOnQualityProfileOrganizationUuidAndKey.class)
      .add(1608, "Make RULES_PROFILES.KEE unique", MakeQualityProfileKeyUnique.class)
    ;
  }
}
