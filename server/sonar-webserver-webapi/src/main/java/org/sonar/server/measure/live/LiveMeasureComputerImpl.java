/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.setting.ProjectConfigurationLoader;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.groupingBy;

public class LiveMeasureComputerImpl implements LiveMeasureComputer {

  private final DbClient dbClient;
  private final MeasureUpdateFormulaFactory formulaFactory;
  private final ComponentIndexFactory componentIndexFactory;
  private final LiveQualityGateComputer qGateComputer;
  private final ProjectConfigurationLoader projectConfigurationLoader;
  private final Indexers projectIndexer;
  private final LiveMeasureTreeUpdater treeUpdater;

  public LiveMeasureComputerImpl(DbClient dbClient, MeasureUpdateFormulaFactory formulaFactory, ComponentIndexFactory componentIndexFactory,
    LiveQualityGateComputer qGateComputer, ProjectConfigurationLoader projectConfigurationLoader, Indexers projectIndexer, LiveMeasureTreeUpdater treeUpdater) {
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

    LiveMeasureUpdaterWorkflow liveMeasureUpdaterWorkflow = LiveMeasureUpdaterWorkflow.build(
      dbClient,
      dbSession,
      branchComponent,
      projectConfigurationLoader,
      qGateComputer);

    Set<String> metricKeys = liveMeasureUpdaterWorkflow.getKeysOfAllInvolvedMetrics(formulaFactory);
    MeasureMatrix matrix = liveMeasureUpdaterWorkflow.buildMeasureMatrix(
      metricKeys,
      components.getAllUuids());

    treeUpdater.update(
      dbSession,
      lastAnalysis.get(),
      liveMeasureUpdaterWorkflow.getConfig(),
      components,
      liveMeasureUpdaterWorkflow.getBranchDto(),
      matrix);

    Metric.Level previousStatus = liveMeasureUpdaterWorkflow.loadPreviousStatus();
    EvaluatedQualityGate evaluatedQualityGate = liveMeasureUpdaterWorkflow.updateQualityGateMeasures(matrix);

    projectIndexer.commitAndIndexBranches(dbSession, singleton(liveMeasureUpdaterWorkflow.getBranchDto()), Indexers.BranchEvent.MEASURE_CHANGE);

    return Optional.of(new QGChangeEvent(
      liveMeasureUpdaterWorkflow.getProjectDto(),
      liveMeasureUpdaterWorkflow.getBranchDto(),
      lastAnalysis.get(),
      liveMeasureUpdaterWorkflow.getConfig(),
      previousStatus, () -> Optional.of(evaluatedQualityGate)));
  }
}
