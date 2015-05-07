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

package org.sonar.server.computation.step;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.graph.Cycle;
import org.sonar.graph.DirectedGraphAccessor;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmTopologicalSorter;
import org.sonar.graph.Edge;
import org.sonar.graph.IncrementalCyclesAndFESSolver;
import org.sonar.graph.MinimumFeedbackEdgeSetSolver;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.design.DsmDataBuilder;
import org.sonar.server.computation.design.FileDependenciesCache;
import org.sonar.server.computation.design.FileDependency;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasuresCache;
import org.sonar.server.computation.source.ReportIterator;
import org.sonar.server.design.db.DsmDataEncoder;
import org.sonar.server.design.db.DsmDb;
import org.sonar.server.measure.ServerMetrics;
import org.sonar.server.util.CloseableIterator;
import org.sonar.server.util.cache.DiskCacheById;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ComputeFileDependenciesStep implements ComputationStep {

  private static final Logger LOG = LoggerFactory.getLogger(ComputeFileDependenciesStep.class);

  private static final int MAX_DSM_DIMENSION = 200;

  private final FileDependenciesCache fileDependenciesCache;
  private final MeasuresCache measuresCache;

  public ComputeFileDependenciesStep(FileDependenciesCache fileDependenciesCache, MeasuresCache measuresCache) {
    this.fileDependenciesCache = fileDependenciesCache;
    this.measuresCache = measuresCache;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    ComponentUuidsCache uuidsByRef = new ComponentUuidsCache(context.getReportReader());
    Map<Integer, Integer> directoryByFile = directoryByFile(context);
    recursivelyProcessComponent(context, uuidsByRef, directoryByFile, rootComponentRef, rootComponentRef);
  }

  private void recursivelyProcessComponent(ComputationContext context, ComponentUuidsCache uuidsByRef, Map<Integer, Integer> directoryByFile,
    int componentRef, int parentModuleRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, uuidsByRef, directoryByFile, childRef, componentRef);
    }

    readFileDependenciesReport(context, component);
    processDependencies(context, uuidsByRef, directoryByFile, component);
  }

  private void readFileDependenciesReport(ComputationContext context, BatchReport.Component component) {
    File fileDependencyReport = context.getReportReader().readFileDependencies(component.getRef());
    if (fileDependencyReport != null) {
      ReportIterator<BatchReport.FileDependency> fileDependenciesIterator = new ReportIterator<>(fileDependencyReport, BatchReport.FileDependency.PARSER);
      try {
        DiskCacheById<FileDependency>.DiskAppender fileDependencyAppender = fileDependenciesCache.newAppender(component.getRef());
        while (fileDependenciesIterator.hasNext()) {
          BatchReport.FileDependency fileDependency = fileDependenciesIterator.next();
          fileDependencyAppender.append(
            new FileDependency(component.getRef(), fileDependency.getToFileRef(), fileDependency.getWeight())
            );
        }
      } finally {
        fileDependenciesIterator.close();
      }
    }
  }

  private void processDependencies(ComputationContext context, ComponentUuidsCache uuidsByRef, Map<Integer, Integer> directoryByFile, BatchReport.Component component) {
    Collection<FileDependency> fileDependencies = getDependenciesFromChildren(context, component.getRef());
    if (!fileDependencies.isEmpty()) {
      DependenciesGraph dependenciesGraph = new DependenciesGraph();
      for (FileDependency fileDependency : fileDependencies) {
        dependenciesGraph.addDependency(fileDependency);
      }

      if (dependenciesGraph.getVertices().size() > MAX_DSM_DIMENSION) {
        LOG.warn(String.format("Too many components under component '%s'. DSM will not be computed.", component.getPath()));
        return;
      }

      DsmDb.Data dsmData = computeDsmData(dependenciesGraph, uuidsByRef);

      Measure measure = new Measure();
      measure.setMetricKey(ServerMetrics.DEPENDENCY_MATRIX_KEY);
      measure.setComponentUuid(component.getUuid());
      measure.setByteValue(DsmDataEncoder.encodeSourceData(dsmData));

      DiskCacheById<Measure>.DiskAppender measureAppender = measuresCache.newAppender(component.getRef());
      try {
        measureAppender.append(measure);
      } finally {
        measureAppender.close();
      }
      feedParentDependencies(context, fileDependencies, directoryByFile, component.getRef());
    }
  }

  private void feedParentDependencies(ComputationContext context, Collection<FileDependency> fileDependencies, Map<Integer, Integer> directoryByFile, int ref) {
    Bag parentDependenciesBag = new HashBag();
    for (FileDependency fileDependency : fileDependencies) {
      Integer directoryRef = directoryByFile.get(fileDependency.getTo());
      if (directoryRef != null) {
        int toDirectoryRef = context.getReportReader().readComponent(directoryRef).getRef();
        parentDependenciesBag.add(toDirectoryRef);
      }
    }

    DiskCacheById<FileDependency>.DiskAppender fileDependencyAppender = fileDependenciesCache.newAppender(ref);
    try {
      for (Object dirRef : parentDependenciesBag.uniqueSet()) {
        fileDependencyAppender.append(
          new FileDependency(ref, (Integer) dirRef, parentDependenciesBag.getCount(dirRef))
          );
      }
    } finally {
      fileDependencyAppender.close();
    }
  }

  private Collection<FileDependency> getDependenciesFromChildren(ComputationContext context, int ref) {
    Collection<FileDependency> dependencies = new ArrayList<>();
    for (Integer child : context.getReportReader().readComponent(ref).getChildRefList()) {
      CloseableIterator<FileDependency> fileDependencies = fileDependenciesCache.traverse(child);
      try {
        while (fileDependencies.hasNext()) {
          dependencies.add(fileDependencies.next());
        }
      } finally {
        fileDependencies.close();
      }
    }
    return dependencies;
  }

  private static DsmDb.Data computeDsmData(DependenciesGraph dependenciesGraph, ComponentUuidsCache uuidsByRef) {
    IncrementalCyclesAndFESSolver<Integer> cycleDetector = new IncrementalCyclesAndFESSolver<>(dependenciesGraph, dependenciesGraph.getVertices());
    Set<Cycle> cycles = cycleDetector.getCycles();
    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycles);
    Set<Edge> feedbackEdges = solver.getEdges();
    Dsm<Integer> dsm = new Dsm<>(dependenciesGraph, dependenciesGraph.getVertices(), feedbackEdges);
    DsmTopologicalSorter.sort(dsm);
    return DsmDataBuilder.build(dsm, uuidsByRef);
  }

  /**
   * Browse all components of the report to create a map of directory ref by file ref
   */
  private static Map<Integer, Integer> directoryByFile(ComputationContext context) {
    Map<Integer, Integer> directoryByFile = new HashMap<>();
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    recursivelyProcessDirectoryByFile(context, directoryByFile, rootComponentRef, rootComponentRef);
    return directoryByFile;
  }

  private static void recursivelyProcessDirectoryByFile(ComputationContext context, Map<Integer, Integer> directoryByFile, int componentRef, int parentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getType().equals(Constants.ComponentType.FILE)) {
      directoryByFile.put(componentRef, parentRef);
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessDirectoryByFile(context, directoryByFile, childRef, componentRef);
    }
  }

  private static class DependenciesGraph implements DirectedGraphAccessor<Integer, FileDependency> {

    private Set<Integer> vertices = new HashSet<>();
    private Map<Integer, Map<Integer, FileDependency>> outgoingDependenciesByComponent = new LinkedHashMap<>();

    private void addDependency(FileDependency dependency) {
      this.vertices.add(dependency.getFrom());
      this.vertices.add(dependency.getTo());
      registerOutgoingDependency(dependency);
    }

    @Override
    @CheckForNull
    public FileDependency getEdge(Integer from, Integer to) {
      Map<Integer, FileDependency> map = outgoingDependenciesByComponent.get(from);
      if (map != null) {
        return map.get(to);
      }
      return null;
    }

    @Override
    public boolean hasEdge(Integer from, Integer to) {
      throw new UnsupportedOperationException("This method is not used");
    }

    @Override
    public Set<Integer> getVertices() {
      return vertices;
    }

    @Override
    public Collection<FileDependency> getOutgoingEdges(Integer from) {
      Map<Integer, FileDependency> deps = outgoingDependenciesByComponent.get(from);
      if (deps != null) {
        return deps.values();
      }
      return Collections.emptyList();
    }

    @Override
    public Collection<FileDependency> getIncomingEdges(Integer to) {
      throw new UnsupportedOperationException("This method is not used");
    }

    private void registerOutgoingDependency(FileDependency dependency) {
      Map<Integer, FileDependency> outgoingDeps = outgoingDependenciesByComponent.get(dependency.getFrom());
      if (outgoingDeps == null) {
        outgoingDeps = new HashMap<>();
        outgoingDependenciesByComponent.put(dependency.getFrom(), outgoingDeps);
      }
      outgoingDeps.put(dependency.getTo(), dependency);
    }
  }

  @Override
  public String getDescription() {
    return "Compute file dependencies";
  }
}
