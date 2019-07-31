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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentProvider;
import org.sonar.ce.task.projectanalysis.component.NoComponentProvider;
import org.sonar.ce.task.projectanalysis.component.TreeComponentProvider;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderComponentProvider;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Maps.filterKeys;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * An implementation of MeasureRepository as a JUnit rule which provides add methods for raw measures and extra add
 * methods that takes component ref and metric keys thanks to the integration with various Component and Metric
 * providers.
 */
public class MeasureRepositoryRule extends ExternalResource implements MeasureRepository {
  private final ComponentProvider componentProvider;
  @CheckForNull
  private final MetricRepositoryRule metricRepositoryRule;
  private final Map<InternalKey, Measure> baseMeasures = new HashMap<>();
  private final Map<InternalKey, Measure> rawMeasures = new HashMap<>();
  private final Map<InternalKey, Measure> initialRawMeasures = new HashMap<>();
  private final Predicate<Map.Entry<InternalKey, Measure>> isAddedMeasure = input -> !initialRawMeasures.containsKey(input.getKey())
    || !MeasureRepoEntry.deepEquals(input.getValue(), initialRawMeasures.get(input.getKey()));

  private MeasureRepositoryRule(ComponentProvider componentProvider, @Nullable MetricRepositoryRule metricRepositoryRule) {
    this.componentProvider = componentProvider;
    this.metricRepositoryRule = metricRepositoryRule;
  }

  @Override
  protected void after() {
    componentProvider.reset();
    baseMeasures.clear();
    rawMeasures.clear();
  }

  public static MeasureRepositoryRule create() {
    return new MeasureRepositoryRule(NoComponentProvider.INSTANCE, null);
  }

  public static MeasureRepositoryRule create(TreeRootHolder treeRootHolder, MetricRepositoryRule metricRepositoryRule) {
    return new MeasureRepositoryRule(new TreeRootHolderComponentProvider(treeRootHolder), requireNonNull(metricRepositoryRule));
  }

  public static MeasureRepositoryRule create(Component treeRoot, MetricRepositoryRule metricRepositoryRule) {
    return new MeasureRepositoryRule(new TreeComponentProvider(treeRoot), requireNonNull(metricRepositoryRule));
  }

  public MeasureRepositoryRule addBaseMeasure(int componentRef, String metricKey, Measure measure) {
    checkAndInitProvidersState();

    InternalKey internalKey = new InternalKey(componentProvider.getByRef(componentRef), metricRepositoryRule.getByKey(metricKey));
    checkState(!baseMeasures.containsKey(internalKey), format("Can not add a BaseMeasure twice for a Component (ref=%s) and Metric (key=%s)", componentRef, metricKey));

    baseMeasures.put(internalKey, measure);

    return this;
  }

  public Map<String, Measure> getRawMeasures(int componentRef) {
    return getRawMeasures(componentProvider.getByRef(componentRef));
  }

  /**
   * Return measures that were added by the step (using {@link #add(Component, Metric, Measure)}).
   * It does not contain the one added in the test by {@link #addRawMeasure(int, String, Measure)}
   */
  public Map<String, Measure> getAddedRawMeasures(int componentRef) {
    checkAndInitProvidersState();

    return getAddedRawMeasures(componentProvider.getByRef(componentRef));
  }

  /**
   * Return a measure that were added by the step (using {@link #add(Component, Metric, Measure)}).
   * It does not contain the one added in the test by {@link #addRawMeasure(int, String, Measure)}
   */
  public Optional<Measure> getAddedRawMeasure(Component component, String metricKey) {
    return getAddedRawMeasure(component.getReportAttributes().getRef(), metricKey);
  }

  /**
   * Return a measure that were added by the step (using {@link #add(Component, Metric, Measure)}).
   * It does not contain the one added in the test by {@link #addRawMeasure(int, String, Measure)}
   */
  public Optional<Measure> getAddedRawMeasure(int componentRef, String metricKey) {
    checkAndInitProvidersState();

    Measure measure = getAddedRawMeasures(componentProvider.getByRef(componentRef)).get(metricKey);
    return Optional.ofNullable(measure);
  }

  /**
   * Return measures that were added by the step (using {@link #add(Component, Metric, Measure)}).
   * It does not contain the one added in the test by {@link #addRawMeasure(int, String, Measure)}
   */
  public Map<String, Measure> getAddedRawMeasures(Component component) {
    checkAndInitProvidersState();

    Map<String, Measure> builder = new HashMap<>();
    for (Map.Entry<InternalKey, Measure> entry : from(filterKeys(rawMeasures, hasComponentRef(component)).entrySet()).filter(isAddedMeasure)) {
      builder.put(entry.getKey().getMetricKey(), entry.getValue());
    }
    return builder;
  }

  public MeasureRepositoryRule addRawMeasure(int componentRef, String metricKey, Measure measure) {
    checkAndInitProvidersState();

    InternalKey internalKey = new InternalKey(componentProvider.getByRef(componentRef), metricRepositoryRule.getByKey(metricKey));
    checkState(!rawMeasures.containsKey(internalKey), format(
      "A measure can only be set once for Component (ref=%s), Metric (key=%s)",
      componentRef, metricKey));

    rawMeasures.put(internalKey, measure);
    initialRawMeasures.put(internalKey, measure);

    return this;
  }

  @Override
  public Optional<Measure> getBaseMeasure(Component component, Metric metric) {
    return Optional.ofNullable(baseMeasures.get(new InternalKey(component, metric)));
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric) {
    return Optional.ofNullable(rawMeasures.get(new InternalKey(component, metric)));
  }

  @Override
  public Map<String, Measure> getRawMeasures(Component component) {
    return filterKeys(rawMeasures, hasComponentRef(component)).entrySet().stream()
      .collect(Collectors.toMap(k -> k.getKey().getMetricKey(), e -> e.getValue()));
  }

  private HasComponentRefPredicate hasComponentRef(Component component) {
    return new HasComponentRefPredicate(component);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    String ref = getRef(component);
    InternalKey internalKey = new InternalKey(ref, metric.getKey());
    if (rawMeasures.containsKey(internalKey)) {
      throw new UnsupportedOperationException(format(
        "A measure can only be set once for Component (ref=%s), Metric (key=%s)",
        ref, metric.getKey()));
    }
    rawMeasures.put(internalKey, measure);
  }

  @Override
  public void update(Component component, Metric metric, Measure measure) {
    String componentRef = getRef(component);
    InternalKey internalKey = new InternalKey(componentRef, metric.getKey());
    if (!rawMeasures.containsKey(internalKey)) {
      throw new UnsupportedOperationException(format(
        "A measure can only be updated if it has been added first for Component (ref=%s), Metric (key=%s)",
        componentRef, metric.getKey()));
    }
    rawMeasures.put(internalKey, measure);
  }

  private void checkAndInitProvidersState() {
    checkState(metricRepositoryRule != null, "Can not add a measure by metric key if MeasureRepositoryRule has not been created for a MetricRepository");
    componentProvider.ensureInitialized();
  }

  public boolean isEmpty() {
    return rawMeasures.isEmpty();
  }

  private static final class InternalKey {
    private final String componentRef;
    private final String metricKey;

    public InternalKey(Component component, Metric metric) {
      this(getRef(component), metric.getKey());
    }

    private InternalKey(String componentRef, String metricKey) {
      this.componentRef = componentRef;
      this.metricKey = metricKey;
    }

    public String getComponentRef() {
      return componentRef;
    }

    public String getMetricKey() {
      return metricKey;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InternalKey that = (InternalKey) o;
      return Objects.equals(componentRef, that.componentRef) &&
        Objects.equals(metricKey, that.metricKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(componentRef, metricKey);
    }

    @Override
    public String toString() {
      return "InternalKey{" +
        "component=" + componentRef +
        ", metric='" + metricKey + '\'' +
        '}';
    }
  }

  private static class HasComponentRefPredicate implements Predicate<InternalKey> {

    private final String componentRef;

    public HasComponentRefPredicate(Component component) {
      this.componentRef = getRef(component);
    }

    @Override
    public boolean apply(@Nonnull InternalKey input) {
      return input.getComponentRef().equals(this.componentRef);
    }
  }

  private static String getRef(Component component) {
    return component.getType().isReportType() ? String.valueOf(component.getReportAttributes().getRef()) : component.getDbKey();
  }

  private static class MatchMetric implements Predicate<Map.Entry<InternalKey, Measure>> {
    private final Metric metric;

    public MatchMetric(Metric metric) {
      this.metric = metric;
    }

    @Override
    public boolean apply(@Nonnull Map.Entry<InternalKey, Measure> input) {
      return input.getKey().getMetricKey().equals(metric.getKey());
    }
  }

  private enum ToMeasure implements Function<Map.Entry<InternalKey, Measure>, Measure> {
    INSTANCE;

    @Nullable
    @Override
    public Measure apply(@Nonnull Map.Entry<InternalKey, Measure> input) {
      return input.getValue();
    }
  }

}
