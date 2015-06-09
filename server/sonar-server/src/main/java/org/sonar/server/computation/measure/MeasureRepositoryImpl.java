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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Metric;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.db.DbClient;

import static java.util.Objects.requireNonNull;

public class MeasureRepositoryImpl implements MeasureRepository {
  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MeasureDtoToMeasure measureDtoToMeasure;
  private final BatchMeasureToMeasure batchMeasureToMeasure;
  private final Map<Integer, Map<String, Measure>> measures = new HashMap<>();

  public MeasureRepositoryImpl(DbClient dbClient, BatchReportReader reportReader, MeasureDtoToMeasure measureDtoToMeasure, BatchMeasureToMeasure batchMeasureToMeasure) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.measureDtoToMeasure = measureDtoToMeasure;
    this.batchMeasureToMeasure = batchMeasureToMeasure;
  }

  @Override
  public Optional<Measure> findPrevious(Component component, Metric<?> metric) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return measureDtoToMeasure.toMeasure(dbClient.measureDao().findByComponentKeyAndMetricKey(dbSession, component.getKey(), metric.getKey()), metric);
    }
  }

  @Override
  public Optional<Measure> findCurrent(final Component component, final Metric<?> metric) {
    Optional<Measure> local = findLocal(component, metric);
    if (local.isPresent()) {
      return local;
    }
    return findInBatch(component, metric);
  }

  private Optional<Measure> findInBatch(Component component, final Metric<?> metric) {
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
  public void add(Component component, Metric<?> metric, Measure measure) {
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
  public Map<String, Measure> getCurrentMeasures(Component component) {
    Map<String, Measure> res = measures.get(component.getRef());
    if (res == null) {
      return Collections.emptyMap();
    }
    return ImmutableMap.copyOf(res);
  }

  private Optional<Measure> findLocal(Component component, Metric<?> metric) {
    Map<String, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(measuresPerMetric.get(metric.getKey()));
  }

  private void addLocal(Component component, Metric<?> metric, Measure measure) {
    Map<String, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      measuresPerMetric = new HashMap<>();
      measures.put(component.getRef(), measuresPerMetric);
    }
    measuresPerMetric.put(metric.getKey(), measure);
  }

}
