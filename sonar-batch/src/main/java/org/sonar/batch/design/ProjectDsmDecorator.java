/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.design;

import com.google.common.collect.Lists;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.graph.Cycle;
import org.sonar.graph.CycleDetector;
import org.sonar.graph.Edge;
import org.sonar.graph.MinimumFeedbackEdgeSetSolver;

import java.util.List;
import java.util.Set;

public class ProjectDsmDecorator extends DsmDecorator {

  public ProjectDsmDecorator(SonarIndex index) {
    super(index);
  }

  /**
   * sub-projects, including all descendants but not only direct children
   */
  @Override
  protected List<Resource> getChildren(Resource resource, DecoratorContext context) {
    List<Resource> subProjects = Lists.newArrayList();
    addSubProjects((Project) resource, subProjects);
    return subProjects;
  }

  private void addSubProjects(Project project, List<Resource> subProjects) {
    for (Project subProject : project.getModules()) {
      Project indexedSubProject = getIndex().getResource(subProject);
      if (indexedSubProject != null) {
        subProjects.add(indexedSubProject);
      }
      addSubProjects(subProject, subProjects);
    }
  }

  @Override
  protected Set<Edge> doProcess(List<Resource> children, DecoratorContext context) {
    CycleDetector<Resource> cycleDetector = new CycleDetector<Resource>(getIndex(), children);
    Set<Cycle> cycles = cycleDetector.getCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycles);
    return solver.getEdges();
  }

  @Override
  protected boolean shouldDecorateResource(Resource resource, DecoratorContext context) {
    // Should not execute on views
    return (ResourceUtils.isRootProject(resource) || ResourceUtils.isModuleProject(resource))
      && !((Project) resource).getModules().isEmpty();
  }
}
