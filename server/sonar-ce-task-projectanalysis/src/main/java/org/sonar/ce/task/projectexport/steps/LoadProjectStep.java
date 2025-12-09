/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport.steps;

import java.util.List;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;

import static java.lang.String.format;

/**
 * Loads project from database and verifies that it's valid: it must exist and be a project !
 */
public class LoadProjectStep implements ComputationStep {

  private final ProjectDescriptor descriptor;
  private final MutableProjectHolder definitionHolder;
  private final DbClient dbClient;

  public LoadProjectStep(ProjectDescriptor descriptor, MutableProjectHolder definitionHolder,
    DbClient dbClient) {
    this.descriptor = descriptor;
    this.definitionHolder = definitionHolder;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = dbClient.projectDao().selectProjectByKey(dbSession, descriptor.getKey())
        .orElseThrow(() -> MessageException.of(format("Project with key [%s] does not exist", descriptor.getKey())));
      definitionHolder.setProjectDto(project);

      List<BranchDto> branches = dbClient.branchDao().selectByProject(dbSession, project).stream().toList();
      definitionHolder.setBranches(branches);
    }
  }

  @Override
  public String getDescription() {
    return "Load project";
  }
}
