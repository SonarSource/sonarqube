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
package org.sonar.server.computation.measure;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.db.DbClient;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

public class MeasureRepositoryImpl implements MeasureRepository {
  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MeasureDtoToMeasure measureDtoToMeasure = new MeasureDtoToMeasure();
  private final BatchMeasureToMeasure batchMeasureToMeasure;
  private final Function<BatchReport.Measure, Measure> batchMeasureToMeasureFunction;
  private final Map<Integer, Map<String, Measure>> measures = new HashMap<>();

  public MeasureRepositoryImpl(DbClient dbClient, BatchReportReader reportReader,
    final MetricRepository metricRepository, final RuleCache ruleCache) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.batchMeasureToMeasure = new BatchMeasureToMeasure(ruleCache);

    this.batchMeasureToMeasureFunction = new Function<BatchReport.Measure, Measure>() {
      @Nullable
      @Override
      public Measure apply(@Nonnull BatchReport.Measure input) {
        return batchMeasureToMeasure.toMeasure(input, metricRepository.getByKey(input.getMetricKey())).get();
      }
    };
  }

  @Override
  public Optional<Measure> getBaseMeasure(Component component, Metric metric) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);

    try (DbSession dbSession = dbClient.openSession(false)) {
      MeasureDto measureDto = dbClient.measureDao().findByComponentKeyAndMetricKey(dbSession, component.getKey(), metric.getKey());
      return measureDtoToMeasure.toMeasure(measureDto, metric);
    }
  }

  @Override
  public Optional<Measure> getRawMeasure(final Component component, final Metric metric) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);

    Optional<Measure> local = findLocal(component, metric);
    if (local.isPresent()) {
      return local;
    }
    return findInBatch(component, metric);
  }

  private Optional<Measure> findInBatch(Component component, final Metric metric) {
    BatchReport.Measure batchMeasure = Iterables.find(
      reportReader.readComponentMeasures(component.getRef()),
      new Predicate<BatchReport.Measure>() {
        @Override
        public boolean apply(@Nonnull BatchReport.Measure input) {
          return input.getMetricKey().equals(metric.getKey());
        }
      }
      , null);

    return batchMeasureToMeasure.toMeasure(batchMeasure, metric);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(measure);

    Optional<Measure> existingMeasure = findLocal(component, metric);
    if (existingMeasure.isPresent()) {
      throw new UnsupportedOperationException(
        String.format(
          "a measure can be set only once for a specific Component (ref=%s) and Metric (key=%s)",
          component.getRef(),
          metric.getKey()
          ));
    }
    addLocal(component, metric, measure);
  }

  @Override
  public SetMultimap<String, Measure> getRawMeasures(Component component) {
    Map<String, Measure> rawMeasures = measures.get(component.getRef());
    ListMultimap<String, BatchReport.Measure> batchMeasures = from(reportReader.readComponentMeasures(component.getRef()))
      .index(BatchMeasureToMetricKey.INSTANCE);

    if (rawMeasures == null && batchMeasures.isEmpty()) {
      return ImmutableSetMultimap.of();
    }

    ListMultimap<String, Measure> rawMeasuresFromBatch = Multimaps.transformValues(batchMeasures, batchMeasureToMeasureFunction);
    if (rawMeasures == null) {
      return ImmutableSetMultimap.copyOf(rawMeasuresFromBatch);
    }

    ImmutableSetMultimap.Builder<String, Measure> builder = ImmutableSetMultimap.builder();
    builder.putAll(rawMeasuresFromBatch);
    for (Map.Entry<String, Measure> entry : rawMeasures.entrySet()) {
      builder.put(entry.getKey(), entry.getValue());
    }
    return builder.build();
  }

  private Optional<Measure> findLocal(Component component, Metric metric) {
    Map<String, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(measuresPerMetric.get(metric.getKey()));
  }

  private void addLocal(Component component, Metric metric, Measure measure) {
    Map<String, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      measuresPerMetric = new HashMap<>();
      measures.put(component.getRef(), measuresPerMetric);
    }
    measuresPerMetric.put(metric.getKey(), measure);
  }

  private enum BatchMeasureToMetricKey implements Function<BatchReport.Measure, String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply(@Nonnull BatchReport.Measure input) {
      return input.getMetricKey();
    }
  }
}
