/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.BestValueOptimization;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.LiveMeasureDto;

import static org.sonar.api.measures.CoreMetrics.ACCEPTED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.REOPENED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.measures.CoreMetrics.WONT_FIX_ISSUES_KEY;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistLiveMeasuresStep implements ComputationStep {

  /**
   * List of metrics that should not be persisted on file measure.
   */
  private static final Set<String> NOT_TO_PERSIST_ON_FILE_METRIC_KEYS = Set.of(FILE_COMPLEXITY_DISTRIBUTION_KEY, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY);
  private static final Set<String> NOT_TO_PERSIST_ON_UNCHANGED_FILES = Set.of(
    BLOCKER_VIOLATIONS_KEY, BUGS_KEY, CLASS_COMPLEXITY_KEY, CLASSES_KEY, CODE_SMELLS_KEY, COGNITIVE_COMPLEXITY_KEY, COMMENT_LINES_KEY, COMMENT_LINES_DENSITY_KEY,
    COMPLEXITY_KEY, COMPLEXITY_IN_CLASSES_KEY, COMPLEXITY_IN_FUNCTIONS_KEY, CONFIRMED_ISSUES_KEY, CRITICAL_VIOLATIONS_KEY, DEVELOPMENT_COST_KEY,
    EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY, FALSE_POSITIVE_ISSUES_KEY, FILE_COMPLEXITY_KEY, FILE_COMPLEXITY_DISTRIBUTION_KEY, FILES_KEY, FUNCTION_COMPLEXITY_KEY,
    FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, FUNCTIONS_KEY, GENERATED_LINES_KEY, GENERATED_NCLOC_KEY, INFO_VIOLATIONS_KEY, LINES_KEY,
    MAJOR_VIOLATIONS_KEY, MINOR_VIOLATIONS_KEY, NCLOC_KEY, NCLOC_DATA_KEY, NCLOC_LANGUAGE_DISTRIBUTION_KEY, OPEN_ISSUES_KEY, RELIABILITY_RATING_KEY,
    RELIABILITY_REMEDIATION_EFFORT_KEY, REOPENED_ISSUES_KEY, SECURITY_HOTSPOTS_KEY, SECURITY_HOTSPOTS_REVIEWED_KEY, SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY,
    SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY, SECURITY_RATING_KEY, SECURITY_REMEDIATION_EFFORT_KEY, SECURITY_REVIEW_RATING_KEY, SQALE_DEBT_RATIO_KEY, TECHNICAL_DEBT_KEY,
    SQALE_RATING_KEY, STATEMENTS_KEY, VIOLATIONS_KEY, VULNERABILITIES_KEY, ACCEPTED_ISSUES_KEY
  );
  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final MeasureToMeasureDto measureToMeasureDto;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final Optional<FileStatuses> fileStatuses;

  public PersistLiveMeasuresStep(DbClient dbClient, MetricRepository metricRepository, MeasureToMeasureDto measureToMeasureDto,
    TreeRootHolder treeRootHolder, MeasureRepository measureRepository, Optional<FileStatuses> fileStatuses) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.measureToMeasureDto = measureToMeasureDto;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.fileStatuses = fileStatuses;
  }

  @Override
  public String getDescription() {
    return "Persist live measures";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    boolean supportUpsert = dbClient.getDatabase().getDialect().supportsUpsert();
    try (DbSession dbSession = dbClient.openSession(true)) {
      Component root = treeRootHolder.getRoot();
      MeasureVisitor visitor = new MeasureVisitor(dbSession, supportUpsert);
      new DepthTraversalTypeAwareCrawler(visitor).visit(root);
      dbSession.commit();
      context.getStatistics()
        .add("insertsOrUpdates", visitor.insertsOrUpdates);
    }
  }

  private class MeasureVisitor extends TypeAwareVisitorAdapter {
    private final DbSession dbSession;
    private final boolean supportUpsert;
    private int insertsOrUpdates = 0;

    private MeasureVisitor(DbSession dbSession, boolean supportUpsert) {
      super(CrawlerDepthLimit.LEAVES, PRE_ORDER);
      this.supportUpsert = supportUpsert;
      this.dbSession = dbSession;
    }

    @Override
    public void visitAny(Component component) {
      List<String> metricUuids = new ArrayList<>();
      List<String> keptMetricUuids = new ArrayList<>();
      Map<String, Measure> measures = measureRepository.getRawMeasures(component);
      List<LiveMeasureDto> dtos = new ArrayList<>();
      for (Map.Entry<String, Measure> measuresByMetricKey : measures.entrySet()) {
        String metricKey = measuresByMetricKey.getKey();
        if (NOT_TO_PERSIST_ON_FILE_METRIC_KEYS.contains(metricKey) ) {
          continue;
        }
        Metric metric = metricRepository.getByKey(metricKey);
        Predicate<Measure> notBestValueOptimized = BestValueOptimization.from(metric, component).negate();
        Measure m = measuresByMetricKey.getValue();
        if (!NonEmptyMeasure.INSTANCE.test(m) || !notBestValueOptimized.test(m)) {
          continue;
        }
        metricUuids.add(metric.getUuid());
        if (shouldSkipMetricOnUnchangedFile(component, metricKey)) {
          keptMetricUuids.add(metric.getUuid());
          continue;
        }

        LiveMeasureDto lm = measureToMeasureDto.toLiveMeasureDto(m, metric, component);
        dtos.add(lm);
      }

      List<String> excludedMetricUuids = supportUpsert ? metricUuids : keptMetricUuids;
      deleteNonexistentMeasures(dbSession, component.getUuid(), excludedMetricUuids);
      dtos.forEach(dto -> insertMeasureOptimally(dbSession, dto));

      dbSession.commit();
      insertsOrUpdates += dtos.size();
    }

    private void deleteNonexistentMeasures(DbSession dbSession, String componentUuid, List<String> excludedMetricUuids) {
      // The measures that no longer exist on the component must be deleted, for example
      // when the coverage on a file goes to the "best value" 100%.
      // The measures on deleted components are deleted by the step PurgeDatastoresStep
      dbClient.liveMeasureDao().deleteByComponentUuidExcludingMetricUuids(dbSession, componentUuid, excludedMetricUuids);
    }

    private void insertMeasureOptimally(DbSession dbSession, LiveMeasureDto dto) {
      if (supportUpsert) {
        dbClient.liveMeasureDao().upsert(dbSession, dto);
      } else {
        dbClient.liveMeasureDao().insert(dbSession, dto);
      }
    }

    private boolean shouldSkipMetricOnUnchangedFile(Component component, String metricKey) {
      return component.getType() == Component.Type.FILE && fileStatuses.isPresent() &&
        fileStatuses.get().isDataUnchanged(component) && NOT_TO_PERSIST_ON_UNCHANGED_FILES.contains(metricKey);
    }
  }

  private enum NonEmptyMeasure implements Predicate<Measure> {
    INSTANCE;

    @Override
    public boolean test(@Nonnull Measure input) {
      return input.getValueType() != Measure.ValueType.NO_VALUE || input.getData() != null;
    }
  }
}
