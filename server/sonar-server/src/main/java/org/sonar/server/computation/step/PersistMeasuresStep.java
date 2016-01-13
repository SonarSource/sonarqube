/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.measure.BestValueOptimization;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureToMeasureDto;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistMeasuresStep implements ComputationStep {

  /**
   * List of metrics that should not be persisted on file measure (Waiting for SONAR-6688 to be implemented)
   */
  private static final List<String> NOT_TO_PERSIST_ON_FILE_METRIC_KEYS = ImmutableList.of(
    FILE_COMPLEXITY_DISTRIBUTION_KEY,
    FUNCTION_COMPLEXITY_DISTRIBUTION_KEY,
    CLASS_COMPLEXITY_DISTRIBUTION_KEY);

  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final MeasureToMeasureDto measureToMeasureDto;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;

  public PersistMeasuresStep(DbClient dbClient, MetricRepository metricRepository, MeasureToMeasureDto measureToMeasureDto,
    TreeRootHolder treeRootHolder, MeasureRepository measureRepository) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.measureToMeasureDto = measureToMeasureDto;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
  }

  @Override
  public String getDescription() {
    return "Persist measures";
  }

  @Override
  public void execute() {
    DbSession dbSession = dbClient.openSession(true);
    try {
      new DepthTraversalTypeAwareCrawler(new MeasureVisitor(dbSession)).visit(treeRootHolder.getRoot());
      dbSession.commit();
    } finally {
      dbSession.close();
    }
  }

  private class MeasureVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;

    private MeasureVisitor(DbSession session) {
      super(CrawlerDepthLimit.LEAVES, PRE_ORDER);
      this.session = session;
    }

    @Override
    public void visitAny(Component component) {
      Multimap<String, Measure> measures = measureRepository.getRawMeasures(component);
      persistMeasures(component, measures);
    }

    private void persistMeasures(Component component, Multimap<String, Measure> batchReportMeasures) {
      for (Map.Entry<String, Collection<Measure>> measures : batchReportMeasures.asMap().entrySet()) {
        String metricKey = measures.getKey();
        if (NOT_TO_PERSIST_ON_FILE_METRIC_KEYS.contains(metricKey) && component.getType() == Component.Type.FILE) {
          continue;
        }

        Metric metric = metricRepository.getByKey(metricKey);
        Predicate<Measure> notBestValueOptimized = Predicates.not(BestValueOptimization.from(metric, component));
        for (Measure measure : from(measures.getValue()).filter(NonEmptyMeasure.INSTANCE).filter(notBestValueOptimized)) {
          MeasureDto measureDto = measureToMeasureDto.toMeasureDto(measure, metric, component);
          dbClient.measureDao().insert(session, measureDto);
        }
      }
    }

  }

  private enum NonEmptyMeasure implements Predicate<Measure> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Measure input) {
      return input.getValueType() != Measure.ValueType.NO_VALUE || input.hasVariations() || input.getData() != null;
    }
  }

}
