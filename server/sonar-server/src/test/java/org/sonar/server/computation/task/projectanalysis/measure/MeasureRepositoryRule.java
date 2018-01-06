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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.rules.ExternalResource;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ComponentProvider;
import org.sonar.server.computation.task.projectanalysis.component.Developer;
import org.sonar.server.computation.task.projectanalysis.component.DumbDeveloper;
import org.sonar.server.computation.task.projectanalysis.component.NoComponentProvider;
import org.sonar.server.computation.task.projectanalysis.component.TreeComponentProvider;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderComponentProvider;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;

import static com.google.common.base.Preconditions.checkArgument;
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
  private Collection<Component> loadedAsRawComponents;
  private Collection<Metric> loadedAsRawMetrics;
  private final Predicate<Map.Entry<InternalKey, Measure>> isAddedMeasure = new Predicate<Map.Entry<InternalKey, Measure>>() {
    @Override
    public boolean apply(@Nonnull Map.Entry<InternalKey, Measure> input) {
      return !initialRawMeasures.containsKey(input.getKey())
        || !MeasureRepoEntry.deepEquals(input.getValue(), initialRawMeasures.get(input.getKey()));
    }
  };

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

  public MeasureRepositoryRule addBaseMeasure(Component component, Metric metric, Measure measure) {
    checkAndInitProvidersState();

    InternalKey internalKey = new InternalKey(component, metric);
    checkState(!baseMeasures.containsKey(internalKey),
      format("Can not add a BaseMeasure twice for a Component (ref=%s) and Metric (key=%s)", getRef(component), metric.getKey()));

    baseMeasures.put(internalKey, measure);

    return this;
  }

  public MeasureRepositoryRule addBaseMeasure(int componentRef, String metricKey, Measure measure) {
    checkAndInitProvidersState();

    InternalKey internalKey = new InternalKey(componentProvider.getByRef(componentRef), metricRepositoryRule.getByKey(metricKey));
    checkState(!baseMeasures.containsKey(internalKey), format("Can not add a BaseMeasure twice for a Component (ref=%s) and Metric (key=%s)", componentRef, metricKey));

    baseMeasures.put(internalKey, measure);

    return this;
  }

  public SetMultimap<String, Measure> getRawMeasures(int componentRef) {
    return getRawMeasures(componentProvider.getByRef(componentRef));
  }

  /**
   * Return measures that were added by the step (using {@link #add(Component, Metric, Measure)}).
   * It does not contain the one added in the test by {@link #addRawMeasure(int, String, Measure)}
   */
  public SetMultimap<String, Measure> getAddedRawMeasures(int componentRef) {
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

    Set<Measure> measures = getAddedRawMeasures(componentProvider.getByRef(componentRef)).get(metricKey);
    if (measures.isEmpty()) {
      return Optional.absent();
    }
    checkArgument(measures.size() == 1, String.format("There is more than one measure on metric '%s' for component '%s'", metricKey, componentRef));
    return Optional.of(measures.iterator().next());
  }

  /**
   * Return measures that were added by the step (using {@link #add(Component, Metric, Measure)}).
   * It does not contain the one added in the test by {@link #addRawMeasure(int, String, Measure)}
   */
  public SetMultimap<String, Measure> getAddedRawMeasures(Component component) {
    checkAndInitProvidersState();

    ImmutableSetMultimap.Builder<String, Measure> builder = ImmutableSetMultimap.builder();
    for (Map.Entry<InternalKey, Measure> entry : from(filterKeys(rawMeasures, hasComponentRef(component)).entrySet()).filter(isAddedMeasure)) {
      builder.put(entry.getKey().getMetricKey(), entry.getValue());
    }
    return builder.build();
  }

  public MeasureRepositoryRule addRawMeasure(int componentRef, String metricKey, Measure measure) {
    checkAndInitProvidersState();

    InternalKey internalKey = new InternalKey(componentProvider.getByRef(componentRef), metricRepositoryRule.getByKey(metricKey), measure.getDeveloper());
    checkState(!rawMeasures.containsKey(internalKey), format(
      "A measure can only be set once for Component (ref=%s), Metric (key=%s)",
      componentRef, metricKey));

    rawMeasures.put(internalKey, measure);
    initialRawMeasures.put(internalKey, measure);

    return this;
  }
  @Override
  public Optional<Measure> getBaseMeasure(Component component, Metric metric) {
    return Optional.fromNullable(baseMeasures.get(new InternalKey(component, metric)));
  }

  public Collection<Component> getComponentsLoadedAsRaw() {
    return loadedAsRawComponents;
  }
  
  public Collection<Metric> getMetricsLoadedAsRaw() {
    return loadedAsRawMetrics;
  }

  @Override
  public Optional<Measure> getRawMeasure(Component component, Metric metric) {
    return Optional.fromNullable(rawMeasures.get(new InternalKey(component, metric)));
  }

  public Optional<Measure> getRawMeasure(Component component, Metric metric, DumbDeveloper developer) {
    return Optional.fromNullable(rawMeasures.get(new InternalKey(component, metric, developer)));
  }

  public Optional<Measure> getRawRuleMeasure(Component component, Metric metric, int ruleId) {
    return Optional.fromNullable(rawMeasures.get(new InternalKey(component, metric, null)));
  }

  @Override
  public Set<Measure> getRawMeasures(Component component, Metric metric) {
    return from(filterKeys(rawMeasures, hasComponentRef(component)).entrySet()).filter(new MatchMetric(metric)).transform(ToMeasure.INSTANCE).toSet();
  }

  @Override
  public SetMultimap<String, Measure> getRawMeasures(Component component) {
    ImmutableSetMultimap.Builder<String, Measure> builder = ImmutableSetMultimap.builder();
    for (Map.Entry<InternalKey, Measure> entry : filterKeys(rawMeasures, hasComponentRef(component)).entrySet()) {
      builder.put(entry.getKey().getMetricKey(), entry.getValue());
    }
    return builder.build();
  }

  private HasComponentRefPredicate hasComponentRef(Component component) {
    return new HasComponentRefPredicate(component);
  }

  @Override
  public void add(Component component, Metric metric, Measure measure) {
    String ref = getRef(component);
    InternalKey internalKey = new InternalKey(ref, metric.getKey(), measure.getDeveloper());
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
    InternalKey internalKey = new InternalKey(componentRef, metric.getKey(), measure.getDeveloper());
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
    @Nullable
    private final Developer developer;

    public InternalKey(Component component, Metric metric) {
      this(getRef(component), metric.getKey(), null);
    }

    public InternalKey(Component component, Metric metric, @Nullable Developer developer) {
      this(getRef(component), metric.getKey(), developer);
    }

    private InternalKey(String componentRef, String metricKey, @Nullable Developer developer) {
      this.componentRef = componentRef;
      this.metricKey = metricKey;
      this.developer = developer;
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
        Objects.equals(metricKey, that.metricKey) &&
        Objects.equals(developer, that.developer);
    }

    @Override
    public int hashCode() {
      return Objects.hash(componentRef, metricKey, developer);
    }

    @Override
    public String toString() {
      return "InternalKey{" +
        "component=" + componentRef +
        ", metric='" + metricKey + '\'' +
        ", developer=" + developer +
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
    return component.getType().isReportType() ? String.valueOf(component.getReportAttributes().getRef()) : component.getKey();
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
