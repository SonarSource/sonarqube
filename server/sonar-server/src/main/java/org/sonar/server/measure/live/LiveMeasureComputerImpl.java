/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureComparator;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.measure.DebtRatingGrid;
import org.sonar.server.measure.Rating;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.settings.ProjectConfigurationLoader;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.groupingBy;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class LiveMeasureComputerImpl implements LiveMeasureComputer {

  private final DbClient dbClient;
  private final IssueMetricFormulaFactory formulaFactory;
  private final LiveQualityGateComputer qGateComputer;
  private final ProjectConfigurationLoader projectConfigurationLoader;
  private final ProjectIndexers projectIndexer;

  public LiveMeasureComputerImpl(DbClient dbClient, IssueMetricFormulaFactory formulaFactory,
    LiveQualityGateComputer qGateComputer, ProjectConfigurationLoader projectConfigurationLoader, ProjectIndexers projectIndexer) {
    this.dbClient = dbClient;
    this.formulaFactory = formulaFactory;
    this.qGateComputer = qGateComputer;
    this.projectConfigurationLoader = projectConfigurationLoader;
    this.projectIndexer = projectIndexer;
  }

  @Override
  public List<QGChangeEvent> refresh(DbSession dbSession, Collection<ComponentDto> components) {
    if (components.isEmpty()) {
      return emptyList();
    }

    List<QGChangeEvent> result = new ArrayList<>();
    Map<String, List<ComponentDto>> componentsByProjectUuid = components.stream().collect(groupingBy(ComponentDto::projectUuid));
    for (List<ComponentDto> groupedComponents : componentsByProjectUuid.values()) {
      Optional<QGChangeEvent> qgChangeEvent = refreshComponentsOnSameProject(dbSession, groupedComponents);
      qgChangeEvent.ifPresent(result::add);
    }
    return result;
  }

  private Optional<QGChangeEvent> refreshComponentsOnSameProject(DbSession dbSession, List<ComponentDto> touchedComponents) {
    // load all the components to be refreshed, including their ancestors
    List<ComponentDto> components = loadTreeOfComponents(dbSession, touchedComponents);
    ComponentDto project = findProject(components);
    OrganizationDto organization = loadOrganization(dbSession, project);
    BranchDto branch = loadBranch(dbSession, project);

    Optional<SnapshotDto> lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, project.uuid());
    if (!lastAnalysis.isPresent()) {
      return Optional.empty();
    }

    QualityGate qualityGate = qGateComputer.loadQualityGate(dbSession, organization, project, branch);
    Collection<String> metricKeys = getKeysOfAllInvolvedMetrics(qualityGate);

    List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    Map<Integer, MetricDto> metricsPerId = metrics.stream()
      .collect(uniqueIndex(MetricDto::getId));
    List<String> componentUuids = components.stream().map(ComponentDto::uuid).collect(toArrayList(components.size()));
    List<LiveMeasureDto> dbMeasures = dbClient.liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession, componentUuids, metricsPerId.keySet());
    // previous status must be load now as MeasureMatrix mutate the LiveMeasureDto which are passed to it
    Metric.Level previousStatus = loadPreviousStatus(metrics, dbMeasures);

    Configuration config = projectConfigurationLoader.loadProjectConfiguration(dbSession, project);
    DebtRatingGrid debtRatingGrid = new DebtRatingGrid(config);

    MeasureMatrix matrix = new MeasureMatrix(components, metricsPerId.values(), dbMeasures);
    FormulaContextImpl context = new FormulaContextImpl(matrix, debtRatingGrid);
    long beginningOfLeak = getBeginningOfLeakPeriod(lastAnalysis, branch);

    components.forEach(c -> {
      IssueCounter issueCounter = new IssueCounter(dbClient.issueDao().selectIssueGroupsByBaseComponent(dbSession, c, beginningOfLeak));
      for (IssueMetricFormula formula : formulaFactory.getFormulas()) {
        // use formulas when the leak period is defined, it's a PR/SLB, or the formula is not about the leak period
        if (shouldUseLeakFormulas(lastAnalysis.get(), branch) || !formula.isOnLeak()) {
          context.change(c, formula);
          try {
            formula.compute(context, issueCounter);
          } catch (RuntimeException e) {
            throw new IllegalStateException("Fail to compute " + formula.getMetric().getKey() + " on " + context.getComponent().getDbKey(), e);
          }
        }
      }
    });

    EvaluatedQualityGate evaluatedQualityGate = qGateComputer.refreshGateStatus(project, qualityGate, matrix);

    // persist the measures that have been created or updated
    matrix.getChanged().sorted(LiveMeasureComparator.INSTANCE)
      .forEach(m -> dbClient.liveMeasureDao().insertOrUpdate(dbSession, m));
    projectIndexer.commitAndIndex(dbSession, singleton(project), ProjectIndexer.Cause.MEASURE_CHANGE);

    return Optional.of(
      new QGChangeEvent(project, branch, lastAnalysis.get(), config, previousStatus, () -> Optional.of(evaluatedQualityGate)));
  }

  private static long getBeginningOfLeakPeriod(Optional<SnapshotDto> lastAnalysis, BranchDto branch) {
    if (isSLBorPR(branch)) {
      return 0L;
    } else {
      Optional<Long> beginningOfLeakPeriod = lastAnalysis.map(SnapshotDto::getPeriodDate);
      return beginningOfLeakPeriod.orElse(Long.MAX_VALUE);
    }
  }

  private static boolean isSLBorPR(BranchDto branch) {
    return branch.getBranchType() == BranchType.SHORT || branch.getBranchType() == BranchType.PULL_REQUEST;
  }

  private static boolean shouldUseLeakFormulas(SnapshotDto lastAnalysis, BranchDto branch) {
    return lastAnalysis.getPeriodDate() != null || isSLBorPR(branch);
  }

  @CheckForNull
  private static Metric.Level loadPreviousStatus(List<MetricDto> metrics, List<LiveMeasureDto> dbMeasures) {
    MetricDto alertStatusMetric = metrics.stream()
      .filter(m -> ALERT_STATUS_KEY.equals(m.getKey()))
      .findAny()
      .orElseThrow(() -> new IllegalStateException(String.format("Metric with key %s is not registered", ALERT_STATUS_KEY)));
    return dbMeasures.stream()
      .filter(m -> m.getMetricId() == alertStatusMetric.getId())
      .map(LiveMeasureDto::getTextValue)
      .filter(Objects::nonNull)
      .map(m -> {
        try {
          return Metric.Level.valueOf(m);
        } catch (IllegalArgumentException e) {
          Loggers.get(LiveMeasureComputerImpl.class)
            .trace("Failed to parse value of metric '{}'", m, e);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  private List<ComponentDto> loadTreeOfComponents(DbSession dbSession, List<ComponentDto> touchedComponents) {
    Set<String> componentUuids = new HashSet<>();
    for (ComponentDto component : touchedComponents) {
      componentUuids.add(component.uuid());
      // ancestors, excluding self
      componentUuids.addAll(component.getUuidPathAsList());
    }
    // Contrary to the formulas in Compute Engine,
    // measures do not aggregate values of descendant components.
    // As a consequence nodes do not need to be sorted. Formulas can be applied
    // on components in any order.
    return dbClient.componentDao().selectByUuids(dbSession, componentUuids);
  }

  private Set<String> getKeysOfAllInvolvedMetrics(QualityGate gate) {
    Set<String> metricKeys = new HashSet<>();
    for (Metric metric : formulaFactory.getFormulaMetrics()) {
      metricKeys.add(metric.getKey());
    }
    metricKeys.addAll(qGateComputer.getMetricsRelatedTo(gate));
    return metricKeys;
  }

  private static ComponentDto findProject(Collection<ComponentDto> components) {
    return components.stream().filter(ComponentDto::isRootProject).findFirst()
      .orElseThrow(() -> new IllegalStateException("No project found in " + components));
  }

  private BranchDto loadBranch(DbSession dbSession, ComponentDto project) {
    return dbClient.branchDao().selectByUuid(dbSession, project.uuid())
      .orElseThrow(() -> new IllegalStateException("Branch not found: " + project.uuid()));
  }

  private OrganizationDto loadOrganization(DbSession dbSession, ComponentDto project) {
    String organizationUuid = project.getOrganizationUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
      .orElseThrow(() -> new IllegalStateException("No organization with UUID " + organizationUuid));
  }

  private static class FormulaContextImpl implements IssueMetricFormula.Context {
    private final MeasureMatrix matrix;
    private final DebtRatingGrid debtRatingGrid;
    private ComponentDto currentComponent;
    private IssueMetricFormula currentFormula;

    private FormulaContextImpl(MeasureMatrix matrix, DebtRatingGrid debtRatingGrid) {
      this.matrix = matrix;
      this.debtRatingGrid = debtRatingGrid;
    }

    private void change(ComponentDto component, IssueMetricFormula formula) {
      this.currentComponent = component;
      this.currentFormula = formula;
    }

    @Override
    public ComponentDto getComponent() {
      return currentComponent;
    }

    @Override
    public DebtRatingGrid getDebtRatingGrid() {
      return debtRatingGrid;
    }

    @Override
    public Optional<Double> getValue(Metric metric) {
      Optional<LiveMeasureDto> measure = matrix.getMeasure(currentComponent, metric.getKey());
      return measure.map(LiveMeasureDto::getValue);
    }

    @Override
    public Optional<Double> getLeakValue(Metric metric) {
      Optional<LiveMeasureDto> measure = matrix.getMeasure(currentComponent, metric.getKey());
      return measure.map(LiveMeasureDto::getVariation);
    }

    @Override
    public void setValue(double value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(!currentFormula.isOnLeak(), "Formula of metric %s accepts only leak values", metricKey);
      matrix.setValue(currentComponent, metricKey, value);
    }

    @Override
    public void setLeakValue(double value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(currentFormula.isOnLeak(), "Formula of metric %s does not accept leak values", metricKey);
      matrix.setLeakValue(currentComponent, metricKey, value);
    }

    @Override
    public void setValue(Rating value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(!currentFormula.isOnLeak(), "Formula of metric %s accepts only leak values", metricKey);
      matrix.setValue(currentComponent, metricKey, value);
    }

    @Override
    public void setLeakValue(Rating value) {
      String metricKey = currentFormula.getMetric().getKey();
      checkState(currentFormula.isOnLeak(), "Formula of metric %s does not accept leak values", metricKey);
      matrix.setLeakValue(currentComponent, metricKey, value);
    }
  }
}
