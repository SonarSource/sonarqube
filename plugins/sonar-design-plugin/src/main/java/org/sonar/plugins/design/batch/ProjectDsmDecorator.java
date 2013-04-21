/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.design.batch;

import com.google.common.collect.Lists;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.graph.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * For performance reasons, this decorator is currently limited to matrix between modules.
 * Squid is optimized for cycle detections (better hashCode and equals methods of SourceCode classes than Resource).
 */
public class ProjectDsmDecorator implements Decorator {

  // hack as long as DecoratorContext does not implement SonarIndex
  private SonarIndex index;

  public ProjectDsmDecorator(SonarIndex index) {
    this.index = index;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(final Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource, context)) {
      Collection<Resource> subProjects = getSubProjects((Project) resource);

      if (!subProjects.isEmpty()) {
        Dsm<Resource> dsm = getDsm(subProjects);
        saveDsm(context, dsm);
      }
    }
  }

  private void saveDsm(DecoratorContext context, Dsm<Resource> dsm) {
    Measure measure = new Measure(CoreMetrics.DEPENDENCY_MATRIX, DsmSerializer.serialize(dsm));
    measure.setPersistenceMode(PersistenceMode.DATABASE);
    context.saveMeasure(measure);
  }

  private Dsm<Resource> getDsm(Collection<Resource> subProjects) {
    CycleDetector<Resource> cycleDetector = new CycleDetector<Resource>(index, subProjects);
    Set<Cycle> cycles = cycleDetector.getCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycles);
    Set<Edge> feedbackEdges = solver.getEdges();

    Dsm<Resource> dsm = new Dsm<Resource>(index, subProjects, feedbackEdges);
    DsmTopologicalSorter.sort(dsm);
    return dsm;
  }

  /**
   * sub-projects, including all descendants but not only direct children
   */
  private Collection<Resource> getSubProjects(final Project project) {
    List<Resource> subProjects = Lists.newArrayList();
    addSubProjects(project, subProjects);
    return subProjects;
  }

  private void addSubProjects(Project project, List<Resource> subProjects) {
    for (Project subProject : project.getModules()) {
      Project indexedSubProject = index.getResource(subProject);
      if (indexedSubProject != null) {
        subProjects.add(indexedSubProject);
      }
      addSubProjects(subProject, subProjects);
    }
  }

  private boolean shouldDecorateResource(Resource resource, DecoratorContext context) {
    return ResourceUtils.isProject(resource) && context.getMeasure(CoreMetrics.DEPENDENCY_MATRIX) == null;
  }
}
