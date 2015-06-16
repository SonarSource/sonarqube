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
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.db.DbClient;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

public class MeasureRepositoryImpl implements MeasureRepository {
  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MeasureDtoToMeasure measureDtoToMeasure = new MeasureDtoToMeasure();
  private final BatchMeasureToMeasure batchMeasureToMeasure;
  private final Function<BatchReport.Measure, Measure> batchMeasureToMeasureFunction;
  private final Map<Integer, Map<MeasureKey, Measure>> measures = new HashMap<>();

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

    Optional<Measure> local = findLocal(component, metric, null, null);
    if (local.isPresent()) {
      return local;
    }
    return findInBatch(component, metric);
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric, RuleDto rule) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(rule);

    return findLocal(component, metric, rule, null);
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric, Characteristic characteristic) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(characteristic);

    return findLocal(component, metric, null, characteristic);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(measure);

    Optional<Measure> existingMeasure = findLocal(component, metric, measure);
    if (existingMeasure.isPresent()) {
      throw new UnsupportedOperationException(
        String.format(
          "a measure can be set only once for a specific Component (ref=%s), Metric (key=%s)%s. Use update method",
          component.getRef(),
          metric.getKey(),
          buildRuleOrCharacteristicMsgPart(measure)
          ));
    }
    addLocal(component, metric, measure);
  }

  @Override
  public void update(Component component, Metric metric, Measure measure) {
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(measure);

    Optional<Measure> existingMeasure = findLocal(component, metric, measure);
    if (!existingMeasure.isPresent()) {
      throw new UnsupportedOperationException(
        String.format(
          "a measure can be updated only if one already exists for a specific Component (ref=%s), Metric (key=%s)%s. Use add method",
          component.getRef(),
          metric.getKey(),
          buildRuleOrCharacteristicMsgPart(measure)
          ));
    }
    addLocal(component, metric, measure);
  }

  private static String buildRuleOrCharacteristicMsgPart(Measure measure) {
    if (measure.getRuleId() != null) {
      return " and rule (id=" + measure.getRuleId() + ")";
    }
    if (measure.getCharacteristicId() != null) {
      return " and Characteristic (id=" + measure.getCharacteristicId() + ")";
    }
    return "";
  }

  @Override
  public SetMultimap<String, Measure> getRawMeasures(Component component) {
    Map<MeasureKey, Measure> rawMeasures = measures.get(component.getRef());
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
    for (Map.Entry<MeasureKey, Measure> entry : rawMeasures.entrySet()) {
      builder.put(entry.getKey().metricKey, entry.getValue());
    }
    return builder.build();
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

    Optional<Measure> res = batchMeasureToMeasure.toMeasure(batchMeasure, metric);
    if (res.isPresent()) {
      addLocal(component, metric, res.get());
    }
    return res;
  }

  private Optional<Measure> findLocal(Component component, Metric metric,
    @Nullable RuleDto rule, @Nullable Characteristic characteristic) {
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(measuresPerMetric.get(new MeasureKey(metric.getKey(), rule, characteristic)));
  }

  private Optional<Measure> findLocal(Component component, Metric metric, Measure measure) {
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(measuresPerMetric.get(new MeasureKey(metric.getKey(), measure.getRuleId(), measure.getCharacteristicId())));
  }

  private void addLocal(Component component, Metric metric, Measure measure) {
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(component.getRef());
    if (measuresPerMetric == null) {
      measuresPerMetric = new HashMap<>();
      measures.put(component.getRef(), measuresPerMetric);
    }
    measuresPerMetric.put(new MeasureKey(metric.getKey(), measure.getRuleId(), measure.getCharacteristicId()), measure);
  }

  private enum BatchMeasureToMetricKey implements Function<BatchReport.Measure, String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply(@Nonnull BatchReport.Measure input) {
      return input.getMetricKey();
    }
  }

  @Immutable
  private static final class MeasureKey {
    private static final int DEFAULT_INT_VALUE = -6253;

    private final String metricKey;
    private final int ruleId;
    private final int characteristicId;

    public MeasureKey(String metricKey, @Nullable Integer ruleId, @Nullable Integer characteristicId) {
      // defensive code in case we badly chose the default value, we want to know it right away!
      checkArgument(ruleId == null || ruleId != DEFAULT_INT_VALUE, "Unsupported rule id");
      checkArgument(characteristicId == null || characteristicId != DEFAULT_INT_VALUE, "Unsupported characteristic id");

      this.metricKey = requireNonNull(metricKey, "MetricKey can not be null");
      this.ruleId = ruleId == null ? DEFAULT_INT_VALUE : ruleId;
      this.characteristicId = characteristicId == null ? DEFAULT_INT_VALUE : characteristicId;
    }

    public MeasureKey(String key, @Nullable RuleDto rule, @Nullable Characteristic characteristic) {
      this(key, rule == null ? null : rule.getId(), characteristic == null ? null : characteristic.getId());
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MeasureKey that = (MeasureKey) o;
      return metricKey.equals(that.metricKey)
        && ruleId == that.ruleId
        && characteristicId == that.characteristicId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(metricKey, ruleId, characteristicId);
    }

    @Override
    public String toString() {
      return com.google.common.base.Objects.toStringHelper(this)
        .add("metricKey", metricKey)
        .add("ruleId", ruleId)
        .add("characteristicId", characteristicId)
        .toString();
    }
  }
}
