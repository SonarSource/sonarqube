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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.server.platform.db.migration.step.MigrationStep;

/**
 * This migration step is a substitute for the Ruby migration file which called two java migrations:
 * {@link FixProjectUuidOfDeveloperProjects} and {@link CleanUsurperRootComponents}.
 */
public class Migration1223 implements MigrationStep {
  private final FixProjectUuidOfDeveloperProjects projectUuidOfDeveloperProjects;
  private final CleanUsurperRootComponents cleanUsurperRootComponents;

  public Migration1223(FixProjectUuidOfDeveloperProjects projectUuidOfDeveloperProjects, CleanUsurperRootComponents cleanUsurperRootComponents) {
    this.projectUuidOfDeveloperProjects = projectUuidOfDeveloperProjects;
    this.cleanUsurperRootComponents = cleanUsurperRootComponents;
  }

  @Override
  public void execute() throws SQLException {
    projectUuidOfDeveloperProjects.execute();
    cleanUsurperRootComponents.execute();
  }
}
