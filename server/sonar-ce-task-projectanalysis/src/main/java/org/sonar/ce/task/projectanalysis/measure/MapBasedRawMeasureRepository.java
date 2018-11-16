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
package org.sonar.ce.task.projectanalysis.measure;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.metric.Metric;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Map based implementation of MeasureRepository which supports only raw measures.
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

    return find(component, metric);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    requireNonNull(component);
    checkValueTypeConsistency(metric, measure);

    Optional<Measure> existingMeasure = find(component, metric, measure);
    if (existingMeasure.isPresent()) {
      throw new UnsupportedOperationException(
        format(
          "a measure can be set only once for a specific Component (key=%s), Metric (key=%s). Use update method",
          component.getDbKey(),
          metric.getKey()));
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
          "a measure can be updated only if one already exists for a specific Component (key=%s), Metric (key=%s). Use add method",
          component.getDbKey(),
          metric.getKey()));
    }
    add(component, metric, measure, OverridePolicy.OVERRIDE);
  }

  private static void checkValueTypeConsistency(Metric metric, Measure measure) {
    checkArgument(
      measure.getValueType() == Measure.ValueType.NO_VALUE || measure.getValueType() == metric.getType().getValueType(),
      "Measure's ValueType (%s) is not consistent with the Metric's ValueType (%s)",
      measure.getValueType(), metric.getType().getValueType());
  }

  @Override
  public Set<Measure> getRawMeasures(Component component, Metric metric) {
    requireNonNull(metric);
    requireNonNull(component);
    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> rawMeasures = measures.get(componentKey);
    if (rawMeasures == null) {
      return Collections.emptySet();
    }
    return rawMeasures.entrySet().stream()
      .filter(new MatchMetric(metric))
      .map(ToMeasure.INSTANCE)
      .collect(Collectors.toSet());
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

  private Optional<Measure> find(Component component, Metric metric) {
    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(componentKey);
    if (measuresPerMetric == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(measuresPerMetric.get(new MeasureKey(metric.getKey(), null)));
  }

  private Optional<Measure> find(Component component, Metric metric, Measure measure) {
    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> measuresPerMetric = measures.get(componentKey);
    if (measuresPerMetric == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(measuresPerMetric.get(new MeasureKey(metric.getKey(), measure.getDeveloper())));
  }

  public void add(Component component, Metric metric, Measure measure, OverridePolicy overridePolicy) {
    requireNonNull(component);
    requireNonNull(measure);
    requireNonNull(measure);
    requireNonNull(overridePolicy);

    T componentKey = componentToKey.apply(component);
    Map<MeasureKey, Measure> measuresPerMetric = measures.computeIfAbsent(componentKey, key -> new HashMap<>());
    MeasureKey key = new MeasureKey(metric.getKey(), measure.getDeveloper());
    if (!measuresPerMetric.containsKey(key) || overridePolicy == OverridePolicy.OVERRIDE) {
      measuresPerMetric.put(key, measure);
    }
  }

  public enum OverridePolicy {
    OVERRIDE, DO_NOT_OVERRIDE
  }

  private static class MatchMetric implements Predicate<Map.Entry<MeasureKey, Measure>> {
    private final Metric metric;

    public MatchMetric(Metric metric) {
      this.metric = metric;
    }

    @Override
    public boolean test(@Nonnull Map.Entry<MeasureKey, Measure> input) {
      return input.getKey().getMetricKey().equals(metric.getKey());
    }
  }

  private enum ToMeasure implements Function<Map.Entry<MeasureKey, Measure>, Measure> {
    INSTANCE;

    @Nullable
    @Override
    public Measure apply(@Nonnull Map.Entry<MeasureKey, Measure> input) {
      return input.getValue();
    }
  }
}
