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
package org.sonar.scanner.phases;

import org.sonar.api.batch.events.ProjectAnalysisHandler;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.resources.Project;

class ProjectAnalysisEvent extends AbstractPhaseEvent<ProjectAnalysisHandler>
  implements ProjectAnalysisHandler.ProjectAnalysisEvent {
  private DefaultInputModule module;

  ProjectAnalysisEvent(DefaultInputModule module, boolean start) {
    super(start);
    this.module = module;
  }

  @Override
  public Project getProject() {
    return new Project(module);
  }

  @Override
  protected void dispatch(ProjectAnalysisHandler handler) {
    handler.onProjectAnalysis(this);
  }

  @Override
  protected Class getType() {
    return ProjectAnalysisHandler.class;
  }

}
