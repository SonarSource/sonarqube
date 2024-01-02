/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.measure.live;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureComparator;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.setting.ProjectConfigurationLoader;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.groupingBy;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class LiveMeasureComputerImpl implements LiveMeasureComputer {

  private final DbClient dbClient;
  private final MeasureUpdateFormulaFactory formulaFactory;
  private final ComponentIndexFactory componentIndexFactory;
  private final LiveQualityGateComputer qGateComputer;
  private final ProjectConfigurationLoader projectConfigurationLoader;
  private final ProjectIndexers projectIndexer;
  private final LiveMeasureTreeUpdater treeUpdater;

  public LiveMeasureComputerImpl(DbClient dbClient, MeasureUpdateFormulaFactory formulaFactory, ComponentIndexFactory componentIndexFactory,
    LiveQualityGateComputer qGateComputer, ProjectConfigurationLoader projectConfigurationLoader, ProjectIndexers projectIndexer, LiveMeasureTreeUpdater treeUpdater) {
    this.dbClient = dbClient;
    this.formulaFactory = formulaFactory;
    this.componentIndexFactory = componentIndexFactory;
    this.qGateComputer = qGateComputer;
    this.projectConfigurationLoader = projectConfigurationLoader;
    this.projectIndexer = projectIndexer;
    this.treeUpdater = treeUpdater;
  }

  @Override
  public List<QGChangeEvent> refresh(DbSession dbSession, Collection<ComponentDto> components) {
    if (components.isEmpty()) {
      return emptyList();
    }

    List<QGChangeEvent> result = new ArrayList<>();
    Map<String, List<ComponentDto>> componentsByProjectUuid = components.stream().collect(groupingBy(ComponentDto::branchUuid));
    for (List<ComponentDto> groupedComponents : componentsByProjectUuid.values()) {
      Optional<QGChangeEvent> qgChangeEvent = refreshComponentsOnSameProject(dbSession, groupedComponents);
      qgChangeEvent.ifPresent(result::add);
    }
    return result;
  }

  private Optional<QGChangeEvent> refreshComponentsOnSameProject(DbSession dbSession, List<ComponentDto> touchedComponents) {
    ComponentIndex components = componentIndexFactory.create(dbSession, touchedComponents);
    ComponentDto branchComponent = components.getBranch();
    Optional<SnapshotDto> lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, branchComponent.uuid());
    if (lastAnalysis.isEmpty()) {
      return Optional.empty();
    }

    BranchDto branch = loadBranch(dbSession, branchComponent);
    Configuration config = projectConfigurationLoader.loadProjectConfiguration(dbSession, branchComponent);
    ProjectDto project = loadProject(dbSession, branch.getProjectUuid());
    QualityGate qualityGate = qGateComputer.loadQualityGate(dbSession, project, branch);
    MeasureMatrix matrix = loadMeasureMatrix(dbSession, components.getAllUuids(), qualityGate);

    treeUpdater.update(dbSession, lastAnalysis.get(), config, components, branch, matrix);

    Metric.Level previousStatus = loadPreviousStatus(dbSession, branchComponent);
    EvaluatedQualityGate evaluatedQualityGate = qGateComputer.refreshGateStatus(branchComponent, qualityGate, matrix, config);
    persistAndIndex(dbSession, matrix, branchComponent);

    return Optional.of(new QGChangeEvent(project, branch, lastAnalysis.get(), config, previousStatus, () -> Optional.of(evaluatedQualityGate)));
  }

  private MeasureMatrix loadMeasureMatrix(DbSession dbSession, Set<String> componentUuids, QualityGate qualityGate) {
    Collection<String> metricKeys = getKeysOfAllInvolvedMetrics(qualityGate);
    Map<String, MetricDto> metricsPerUuid = dbClient.metricDao().selectByKeys(dbSession, metricKeys).stream().collect(uniqueIndex(MetricDto::getUuid));
    List<LiveMeasureDto> measures = dbClient.liveMeasureDao().selectByComponentUuidsAndMetricUuids(dbSession, componentUuids, metricsPerUuid.keySet());
    return new MeasureMatrix(componentUuids, metricsPerUuid.values(), measures);
  }

  private void persistAndIndex(DbSession dbSession, MeasureMatrix matrix, ComponentDto branchComponent) {
    // persist the measures that have been created or updated
    matrix.getChanged().sorted(LiveMeasureComparator.INSTANCE).forEach(m -> dbClient.liveMeasureDao().insertOrUpdate(dbSession, m));
    projectIndexer.commitAndIndexComponents(dbSession, singleton(branchComponent), ProjectIndexer.Cause.MEASURE_CHANGE);
  }

  @CheckForNull
  private Metric.Level loadPreviousStatus(DbSession dbSession, ComponentDto branchComponent) {
    Optional<LiveMeasureDto> measure = dbClient.liveMeasureDao().selectMeasure(dbSession, branchComponent.uuid(), ALERT_STATUS_KEY);
    if (measure.isEmpty()) {
      return null;
    }

    try {
      return Metric.Level.valueOf(measure.get().getTextValue());
    } catch (IllegalArgumentException e) {
      Loggers.get(LiveMeasureComputerImpl.class).trace("Failed to parse value of metric '{}'", ALERT_STATUS_KEY, e);
      return null;
    }
  }

  private Set<String> getKeysOfAllInvolvedMetrics(QualityGate gate) {
    Set<String> metricKeys = new HashSet<>();
    for (Metric<?> metric : formulaFactory.getFormulaMetrics()) {
      metricKeys.add(metric.getKey());
    }
    metricKeys.addAll(qGateComputer.getMetricsRelatedTo(gate));
    return metricKeys;
  }

  private BranchDto loadBranch(DbSession dbSession, ComponentDto branchComponent) {
    return dbClient.branchDao().selectByUuid(dbSession, branchComponent.uuid())
      .orElseThrow(() -> new IllegalStateException("Branch not found: " + branchComponent.uuid()));
  }

  private ProjectDto loadProject(DbSession dbSession, String uuid) {
    return dbClient.projectDao().selectByUuid(dbSession, uuid)
      .orElseThrow(() -> new IllegalStateException("Project not found: " + uuid));
  }
}
