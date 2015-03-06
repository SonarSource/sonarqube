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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmTopologicalSorter;
import org.sonar.graph.Edge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class DsmDecorator implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(DsmDecorator.class);

  private static final int MAX_DSM_DIMENSION = 200;
  private SonarIndex index;

  public DsmDecorator(SonarIndex index) {
    this.index = index;
  }

  public final SonarIndex getIndex() {
    return index;
  }

  @Override
  public final boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public final void decorate(final Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource, context)) {
      List<Resource> children = getChildren(resource, context);
      if (children.isEmpty()) {
        return;
      }
      Set<Edge> feedbackEdges = doProcess(children, context);

      if (children.size() > MAX_DSM_DIMENSION) {
        LOG.warn("Too many components under resource '" + resource.getName() + "'. DSM will not be displayed.");
        return;
      }
      Dsm<Resource> dsm = getDsm(children, feedbackEdges);
      saveDsm(context, dsm);
    }
  }

  protected abstract boolean shouldDecorateResource(Resource resource, DecoratorContext context);

  protected abstract List<Resource> getChildren(Resource resource, DecoratorContext context);

  protected abstract Set<Edge> doProcess(List<Resource> children, DecoratorContext context);

  protected final void saveDsm(DecoratorContext context, Dsm<Resource> dsm) {
    Measure measure = new Measure(CoreMetrics.DEPENDENCY_MATRIX, DsmSerializer.serialize(dsm));
    measure.setPersistenceMode(PersistenceMode.DATABASE);
    context.saveMeasure(measure);
  }

  protected final Dsm<Resource> getDsm(Collection<Resource> children, Set<Edge> feedbackEdges) {
    Dsm<Resource> dsm = new Dsm<Resource>(index, children, feedbackEdges);
    DsmTopologicalSorter.sort(dsm);
    return dsm;
  }

  protected final void savePositiveMeasure(DecoratorContext context, Metric<Integer> metric, double value) {
    if (value >= 0.0) {
      context.saveMeasure(new Measure(metric, value));
    }
  }

  protected final int getEdgesWeight(Collection<Resource> sourceCodes) {
    List<Dependency> edges = getEdges(sourceCodes);
    int total = 0;
    for (Dependency edge : edges) {
      total += edge.getWeight();
    }
    return total;
  }

  protected final List<Dependency> getEdges(Collection<Resource> vertices) {
    List<Dependency> result = new ArrayList<Dependency>();
    for (Resource vertice : vertices) {
      Collection<Dependency> outgoingEdges = index.getOutgoingEdges(vertice);
      if (outgoingEdges != null) {
        result.addAll(outgoingEdges);
      }
    }
    return result;
  }
}
