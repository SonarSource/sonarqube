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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.setting.ProjectConfigurationLoader;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.groupingBy;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

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
    OrganizationDto organization = loadOrganization(dbSession, branchComponent);
    Optional<SnapshotDto> lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, branchComponent.uuid());
    if (lastAnalysis.isEmpty()) {
      return Optional.empty();
    }

    BranchDto branch = loadBranch(dbSession, branchComponent);
    ProjectDto project = loadProject(dbSession, branch.getProjectUuid());
    Configuration config = projectConfigurationLoader.loadBranchConfiguration(dbSession, branch);
    QualityGate qualityGate = qGateComputer.loadQualityGate(dbSession, organization, project, branch);
    MeasureMatrix matrix = loadMeasureMatrix(dbSession, components.getAllUuids(), qualityGate);

    treeUpdater.update(dbSession, lastAnalysis.get(), config, components, branch, matrix);

    Metric.Level previousStatus = loadPreviousStatus(dbSession, branchComponent);
    EvaluatedQualityGate evaluatedQualityGate = qGateComputer.refreshGateStatus(branchComponent, qualityGate, matrix, config);
    persistAndIndex(dbSession, matrix, branch);

    return Optional.of(new QGChangeEvent(project, branch, lastAnalysis.get(), config, previousStatus, () -> Optional.of(evaluatedQualityGate)));
  }

  private OrganizationDto loadOrganization(DbSession dbSession, ComponentDto project) {
    String organizationUuid = project.getOrganizationUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
            .orElseThrow(() -> new IllegalStateException("No organization with UUID " + organizationUuid));
  }

  private MeasureMatrix loadMeasureMatrix(DbSession dbSession, Set<String> componentUuids, QualityGate qualityGate) {
    Collection<String> metricKeys = getKeysOfAllInvolvedMetrics(qualityGate);
    Map<String, MetricDto> metricPerKey =
      dbClient.metricDao().selectByKeys(dbSession, metricKeys).stream().collect(Collectors.toMap(MetricDto::getKey, Function.identity()));
    List<MeasureDto> measures = dbClient.measureDao()
      .selectByComponentUuidsAndMetricKeys(dbSession, componentUuids, metricPerKey.keySet());
    return new MeasureMatrix(componentUuids, metricPerKey.values(), measures);
  }

  private void persistAndIndex(DbSession dbSession, MeasureMatrix matrix, BranchDto branch) {
    // persist the measures that have been created or updated
    Map<String, MeasureDto> measureDtoPerComponent = new HashMap<>();
    matrix.getChanged().sorted(MeasureMatrix.Measure.COMPARATOR)
      .filter(m -> m.getValue() != null)
      .forEach(m -> measureDtoPerComponent.compute(m.getComponentUuid(), (componentUuid, measureDto) -> {
      if (measureDto == null) {
        measureDto = new MeasureDto()
          .setComponentUuid(componentUuid)
          .setBranchUuid(m.getBranchUuid());
      }
      return measureDto.addValue(
        m.getMetricKey(),
        m.getValue()
      );
    }));
    measureDtoPerComponent.values().forEach(m -> dbClient.measureDao().insertOrUpdate(dbSession, m));
    projectIndexer.commitAndIndexBranches(dbSession, singleton(branch), Indexers.BranchEvent.MEASURE_CHANGE);
  }

  @CheckForNull
  private Metric.Level loadPreviousStatus(DbSession dbSession, ComponentDto branchComponent) {
    return dbClient.measureDao().selectByComponentUuid(dbSession, branchComponent.uuid())
      .map(m -> m.getString(ALERT_STATUS_KEY))
      .map(m -> {
        try {
          return Metric.Level.valueOf(m);
        } catch (IllegalArgumentException e) {
          LoggerFactory.getLogger(LiveMeasureComputerImpl.class).trace("Failed to parse value of metric '{}'", ALERT_STATUS_KEY, e);
          return null;
        }
      })
      .orElse(null);
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
