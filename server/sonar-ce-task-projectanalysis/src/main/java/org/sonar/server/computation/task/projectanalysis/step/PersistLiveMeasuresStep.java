/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.LiveMeasureDao;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.measure.BestValueOptimization;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistLiveMeasuresStep implements ComputationStep {

  /**
   * List of metrics that should not be persisted on file measure.
   */
  private static final Set<String> NOT_TO_PERSIST_ON_FILE_METRIC_KEYS = unmodifiableSet(new HashSet<>(asList(
    FILE_COMPLEXITY_DISTRIBUTION_KEY,
    FUNCTION_COMPLEXITY_DISTRIBUTION_KEY,
    CLASS_COMPLEXITY_DISTRIBUTION_KEY)));

  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final MeasureToMeasureDto measureToMeasureDto;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;

  public PersistLiveMeasuresStep(DbClient dbClient, MetricRepository metricRepository, MeasureToMeasureDto measureToMeasureDto,
    TreeRootHolder treeRootHolder, MeasureRepository measureRepository) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.measureToMeasureDto = measureToMeasureDto;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
  }

  @Override
  public String getDescription() {
    return "Persist live measures";
  }

  @Override
  public void execute() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String marker = Uuids.create();
      Component root = treeRootHolder.getRoot();
      new DepthTraversalTypeAwareCrawler(new MeasureVisitor(dbSession, marker)).visit(root);
      dbClient.liveMeasureDao().deleteByProjectUuidExcludingMarker(dbSession, root.getUuid(), marker);
      dbSession.commit();
    }
  }

  private class MeasureVisitor extends TypeAwareVisitorAdapter {
    private final DbSession dbSession;
    private final String marker;

    private MeasureVisitor(DbSession dbSession, String marker) {
      super(CrawlerDepthLimit.LEAVES, PRE_ORDER);
      this.dbSession = dbSession;
      this.marker = marker;
    }

    @Override
    public void visitAny(Component component) {
      int count = 0;
      LiveMeasureDao dao = dbClient.liveMeasureDao();
      Multimap<String, Measure> measures = measureRepository.getRawMeasures(component);
      for (Map.Entry<String, Collection<Measure>> measuresByMetricKey : measures.asMap().entrySet()) {
        String metricKey = measuresByMetricKey.getKey();
        if (NOT_TO_PERSIST_ON_FILE_METRIC_KEYS.contains(metricKey) && component.getType() == Component.Type.FILE) {
          continue;
        }
        Metric metric = metricRepository.getByKey(metricKey);
        Predicate<Measure> notBestValueOptimized = BestValueOptimization.from(metric, component).negate();
        Iterator<LiveMeasureDto> liveMeasures = measuresByMetricKey.getValue().stream()
          .filter(NonEmptyMeasure.INSTANCE)
          .filter(notBestValueOptimized)
          .map(measure -> measureToMeasureDto.toLiveMeasureDto(measure, metric, component))
          .iterator();
        while (liveMeasures.hasNext()) {
          dao.insertOrUpdate(dbSession, liveMeasures.next(), marker);
          count++;
          if (count % 100 == 0) {
            // use short transactions to avoid potential deadlocks on MySQL
            // https://jira.sonarsource.com/browse/SONAR-10117?focusedCommentId=153555&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-153555
            dbSession.commit();
          }
        }
      }
    }
  }

  private enum NonEmptyMeasure implements Predicate<Measure> {
    INSTANCE;

    @Override
    public boolean test(@Nonnull Measure input) {
      return input.getValueType() != Measure.ValueType.NO_VALUE || input.hasVariation() || input.getData() != null;
    }
  }

}
