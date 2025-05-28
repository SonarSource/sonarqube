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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.setting.ProjectConfigurationLoader;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

/**
 * This class breaks apart the various steps required to update an
 * existing quality gate's measures with new ones:
 * <ul>
 *   <li>
 *     Get related metric keys
 *     <ul><li>After this step is where you would inject additional keys</li></ul>
 *   </li>
 *   <li>
 *     Build a "measure matrix", a special hash table that contains the original measures mapped to the related metric keys
 *     <ul><li>This matrix is then updated with new measure values</li></ul>
 *   </li>
 *   <li>
 *     Load the previous quality gate status
 *   </li>
 *   <li>
 *     Update the quality gate's measures, persisting them to the database
 *   </li>
 * </ul>
 */
public class LiveMeasureUpdaterWorkflow {
  private final Configuration config;
  private final QualityGate qualityGate;
  private final DbClient dbClient;
  private final DbSession dbSession;
  private final LiveQualityGateComputer qualityGateComputer;
  private final Dtos dtos;

  private LiveMeasureUpdaterWorkflow(
    Dtos dtos,
    DbClient dbClient,
    DbSession dbSession,
    Configuration config,
    QualityGate qualityGate,
    LiveQualityGateComputer qualityGateComputer) {
    this.dtos = dtos;
    this.config = config;
    this.qualityGate = qualityGate;
    this.dbClient = dbClient;
    this.dbSession = dbSession;
    this.qualityGateComputer = qualityGateComputer;
  }

  public static LiveMeasureUpdaterWorkflow build(
    DbClient dbClient,
    DbSession dbSession,
    ComponentDto branchComponentDto,
    ProjectConfigurationLoader projectConfigurationLoader,
    LiveQualityGateComputer qualityGateComputer) {
    BranchDto branchDto = loadBranch(dbClient, dbSession, branchComponentDto);
    ProjectDto projectDto = loadProject(dbClient, dbSession, branchDto.getProjectUuid());
    Configuration config = projectConfigurationLoader.loadBranchConfiguration(dbSession, branchDto);
    QualityGate qualityGate = qualityGateComputer.loadQualityGate(dbSession, projectDto, branchDto);

    return new LiveMeasureUpdaterWorkflow(
      new Dtos(branchComponentDto, branchDto, projectDto),
      dbClient,
      dbSession,
      config,
      qualityGate,
      qualityGateComputer);
  }

  private static BranchDto loadBranch(
    DbClient dbClient,
    DbSession dbSession,
    ComponentDto branchComponent) {
    return dbClient.branchDao().selectByUuid(dbSession, branchComponent.uuid())
      .orElseThrow(() -> new IllegalStateException("Branch not found: " + branchComponent.uuid()));
  }

  private static ProjectDto loadProject(
    DbClient dbClient,
    DbSession dbSession,
    String uuid) {
    return dbClient.projectDao().selectByUuid(dbSession, uuid)
      .orElseThrow(() -> new IllegalStateException("Project not found: " + uuid));
  }

  public BranchDto getBranchDto() {
    return dtos.branchDto;
  }

  public ProjectDto getProjectDto() {
    return dtos.projectDto;
  }

  public Configuration getConfig() {
    return config;
  }

  public EvaluatedQualityGate updateQualityGateMeasures(MeasureMatrix matrix) {
    var result = qualityGateComputer.refreshGateStatus(
      dtos.branchComponentDto,
      qualityGate,
      matrix,
      config);

    persistUpdatedMeasures(matrix);

    return result;
  }

  public Set<String> getKeysOfAllInvolvedMetrics(MeasureUpdateFormulaFactory formulaFactory) {
    Set<String> metricKeys = new HashSet<>();
    for (Metric<?> metric : formulaFactory.getFormulaMetrics()) {
      metricKeys.add(metric.getKey());
    }
    metricKeys.addAll(qualityGateComputer.getMetricsRelatedTo(qualityGate));

    return metricKeys;
  }

  public MeasureMatrix buildMeasureMatrix(
    Collection<String> metricKeys,
    Set<String> branchUuids) {
    Map<String, MetricDto> metricPerKey = dbClient.metricDao().selectByKeys(dbSession, metricKeys).stream().collect(Collectors.toMap(MetricDto::getKey, Function.identity()));
    List<MeasureDto> measures = dbClient.measureDao()
      .selectByComponentUuidsAndMetricKeys(dbSession, branchUuids, metricPerKey.keySet());
    return new MeasureMatrix(branchUuids, metricPerKey.values(), measures);
  }

  private void persistUpdatedMeasures(MeasureMatrix matrix) {
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
          m.getValue());
      }));
    measureDtoPerComponent.values().forEach(m -> dbClient.measureDao().insertOrUpdate(dbSession, m));
  }

  @CheckForNull
  public Metric.Level loadPreviousStatus() {
    return dbClient.measureDao().selectByComponentUuid(dbSession, dtos.branchDto.getUuid())
      .map(m -> m.getString(ALERT_STATUS_KEY))
      .map(m -> {
        try {
          return Metric.Level.valueOf(m);
        } catch (IllegalArgumentException e) {
          LoggerFactory.getLogger(LiveMeasureUpdaterWorkflow.class).trace("Failed to parse value of metric '{}'", ALERT_STATUS_KEY, e);
          return null;
        }
      })
      .orElse(null);
  }

  private record Dtos(ComponentDto branchComponentDto, BranchDto branchDto, ProjectDto projectDto) {
  }
}
