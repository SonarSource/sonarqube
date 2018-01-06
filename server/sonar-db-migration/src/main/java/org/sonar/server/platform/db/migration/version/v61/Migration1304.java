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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class Migration1304 implements MigrationStep {
  private final ShrinkModuleUuidPathOfProjects shrinkModuleUuidPathOfProjects;
  private final AddBUuidPathToProjects addBUuidPathToProjects;

  public Migration1304(ShrinkModuleUuidPathOfProjects shrinkModuleUuidPathOfProjects, AddBUuidPathToProjects addBUuidPathToProjects) {
    this.shrinkModuleUuidPathOfProjects = shrinkModuleUuidPathOfProjects;
    this.addBUuidPathToProjects = addBUuidPathToProjects;
  }

  @Override
  public void execute() throws SQLException {
    shrinkModuleUuidPathOfProjects.execute();
    addBUuidPathToProjects.execute();
  }
}
