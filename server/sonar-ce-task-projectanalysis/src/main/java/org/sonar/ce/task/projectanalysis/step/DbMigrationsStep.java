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
package org.sonar.ce.task.projectanalysis.step;

import java.sql.SQLException;
import org.sonar.ce.task.projectanalysis.dbmigration.ProjectAnalysisDataChange;
import org.sonar.ce.task.projectanalysis.dbmigration.ProjectAnalysisDataChanges;
import org.sonar.ce.task.step.ComputationStep;

public class DbMigrationsStep implements ComputationStep {
  private final ProjectAnalysisDataChanges dataChanges;

  public DbMigrationsStep(ProjectAnalysisDataChanges dataChanges) {
    this.dataChanges = dataChanges;
  }

  @Override
  public String getDescription() {
    return "Execute DB migrations for current project";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    dataChanges.getDataChanges().forEach(DbMigrationsStep::execute);
  }

  private static void execute(ProjectAnalysisDataChange dataChange) {
    try {
      dataChange.execute();
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to perform DB migration for project", e);
    }
  }

}
