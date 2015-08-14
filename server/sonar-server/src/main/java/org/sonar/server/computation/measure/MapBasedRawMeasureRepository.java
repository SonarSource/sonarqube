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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.metric.Metric;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Map based implementation of MeasureRepository which supports only raw measures.
 *
 * Intended to be used as a delegate of other MeasureRepository implementations (hence the final keyword).
 */
public final class MapBasedRawMeasureRepository<T> implements MeasureRepository {
  private final Function<Component, T> componentToKey;
  private final Map<T, Map<MeasureKey, Measure>> measures = new HashMap<>();

  public MapBasedRawMeasureRepository(Function<Component, T> componentToKey) {
    this.componentToKey = requireNonNull(componentToKey);
  }

  /**
   * @throws UnsupportedOperationException all the time, not supported
   */
  @Override
  public Optional<Measure> getBaseMeasure(Component component, Metric metric) {
    throw new UnsupportedOperationException("This implementation of MeasureRepository supports only raw measures");
  }

  @Override
  public Optional<Measure> getRawMeasure(final Component component, final Metric metric) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);

    return find(component, metric, null, null);
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric, RuleDto rule) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(rule);

    return find(component, metric, rule, null);
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric, Characteristic characteristic) {
    // fail fast
    requireNonNull(component);
    requireNonNull(metric);
    requireNonNull(characteristic);

    return find(component, metric, null, characteristic);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    requireNonNull(component);
    checkValueTypeConsistency(metric, measure);

    Optional<Measure> existingMeasure = find(component, metric, measure);
    if (existingMeasure.isPresent()) {
      throw new UnsupportedOperationException(
        format(
          "a measure can be set only once for a specific Component (key=%s), Metric (key=%s)%s. Use update method",
          component.getKey(),
          metric.getKey(),
          buildRuleOrCharacteristicMsgPart(measure)));
    }
    add(component, metric, measure, OverridePolicy.OVERRIDE);
  }

  @Override
  public void update(Component component, Metric metric, Measure measure) {
    requireNonNull(component);
    checkValueTypeConsistency(metric, measure);

    Optional<Measure> existingMeasure = find(component, metric, measure);
    if (!existingMeasure.isPresent()) {
      throw new UnsupportedOperationException(
        format(
          "a measure can be updated only if one already exists for a specific Component (key=%s), Metric (key=%s)%s. Use add method",
          component.getKey(),
          metric.getKey(),
          buildRuleOrCharacteristicMsgPart(measure)));
    }
    add(component, metric, measure, OverridePolicy.OVERRIDE);
  }

  private static void checkValueTypeConsistency(Metric metric, Measure measure) {
    checkArgument(
      measure.getValueType() == Measure.ValueType.NO_VALUE || measure.getValueType() == metric.getType().getValueType(),
      format(
        "Measure's ValueType (%s) is not consistent with the Metric's ValueType (%s)",
        measure.getValueType(), metric.getType().getValueType()));
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
    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> rawMeasures = measures.get(componentKey);
    if (rawMeasures == null) {
      return ImmutableSetMultimap.of();
    }

    ImmutableSetMultimap.Builder<String, Measure> builder = ImmutableSetMultimap.builder();
    for (Map.Entry<MeasureKey, Measure> entry : rawMeasures.entrySet()) {
      builder.put(entry.getKey().getMetricKey(), entry.getValue());
    }
    return builder.build();
  }

  private Optional<Measure> find(Component component, Metric metric, @Nullable RuleDto rule, @Nullable Characteristic characteristic) {
    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(componentKey);
    if (measuresPerMetric == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(measuresPerMetric.get(new MeasureKey(metric.getKey(), rule, characteristic)));
  }

  private Optional<Measure> find(Component component, Metric metric, Measure measure) {
    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(componentKey);
    if (measuresPerMetric == null) {
      return Optional.absent();
    }
    return Optional.fromNullable(measuresPerMetric.get(new MeasureKey(metric.getKey(), measure.getRuleId(), measure.getCharacteristicId())));
  }

  public void add(Component component, Metric metric, Measure measure, OverridePolicy overridePolicy) {
    requireNonNull(component);
    requireNonNull(measure);
    requireNonNull(measure);
    requireNonNull(overridePolicy);

    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(componentKey);
    if (measuresPerMetric == null) {
      measuresPerMetric = new HashMap<>();
      measures.put(componentKey, measuresPerMetric);
    }
    MeasureKey key = new MeasureKey(metric.getKey(), measure.getRuleId(), measure.getCharacteristicId());
    if (!measuresPerMetric.containsKey(key) || overridePolicy == OverridePolicy.OVERRIDE) {
      measuresPerMetric.put(key, measure);
    }
  }

  public enum OverridePolicy {
    OVERRIDE, DO_NOT_OVERRIDE
  }
}
