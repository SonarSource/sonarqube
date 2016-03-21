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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.PastMeasureDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureKey;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Set variations on all numeric measures found in the repository.
 * This step MUST be executed after all steps that create some measures
 * <p/>
 * Note that measures on developer are not handle yet.
 */
public class ComputeMeasureVariationsStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  private final Function<PastMeasureDto, MeasureKey> pastMeasureToMeasureKey = new Function<PastMeasureDto, MeasureKey>() {
    @Nullable
    @Override
    public MeasureKey apply(@Nonnull PastMeasureDto input) {
      Metric metric = metricRepository.getById(input.getMetricId());
      return new MeasureKey(metric.getKey(), null);
    }
  };

  public ComputeMeasureVariationsStep(DbClient dbClient, TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder, MetricRepository metricRepository,
    MeasureRepository measureRepository) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<Metric> metrics = from(metricRepository.getAll()).filter(NumericMetric.INSTANCE).toList();
      new DepthTraversalTypeAwareCrawler(new VariationMeasuresVisitor(dbSession, metrics))
        .visit(treeRootHolder.getRoot());
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private class VariationMeasuresVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;
    private final Set<Integer> metricIds;
    private final Map<String, Metric> metricByKeys;

    public VariationMeasuresVisitor(DbSession session, Iterable<Metric> metrics) {
      // measures on files are currently purged, so past measures are not available on files
      super(CrawlerDepthLimit.reportMaxDepth(DIRECTORY).withViewsMaxDepth(SUBVIEW), PRE_ORDER);
      this.session = session;
      this.metricIds = from(metrics).transform(MetricDtoToMetricId.INSTANCE).toSet();
      this.metricByKeys = from(metrics).uniqueIndex(MetricToKey.INSTANCE);
    }

    @Override
    public void visitAny(Component component) {
      MeasuresWithVariationRepository measuresWithVariationRepository = computeMeasuresWithVariations(component);
      processMeasuresWithVariation(component, measuresWithVariationRepository);
    }

    private MeasuresWithVariationRepository computeMeasuresWithVariations(Component component) {
      MeasuresWithVariationRepository measuresWithVariationRepository = new MeasuresWithVariationRepository();
      for (Period period : periodsHolder.getPeriods()) {
        List<PastMeasureDto> pastMeasures = dbClient.measureDao()
          .selectByComponentUuidAndProjectSnapshotIdAndMetricIds(session, component.getUuid(), period.getSnapshotId(), metricIds);
        setVariationMeasures(component, pastMeasures, period.getIndex(), measuresWithVariationRepository);
      }
      return measuresWithVariationRepository;
    }

    private void processMeasuresWithVariation(Component component, MeasuresWithVariationRepository measuresWithVariationRepository) {
      for (MeasureWithVariations measureWithVariations : measuresWithVariationRepository.measures()) {
        Metric metric = measureWithVariations.getMetric();
        Measure measure = Measure.updatedMeasureBuilder(measureWithVariations.getMeasure())
          .setVariations(new MeasureVariations(
            measureWithVariations.getVariation(1),
            measureWithVariations.getVariation(2),
            measureWithVariations.getVariation(3),
            measureWithVariations.getVariation(4),
            measureWithVariations.getVariation(5)))
          .create();
        measureRepository.update(component, metric, measure);
      }
    }

    private void setVariationMeasures(Component component, List<PastMeasureDto> pastMeasures, int period, MeasuresWithVariationRepository measuresWithVariationRepository) {
      Map<MeasureKey, PastMeasureDto> pastMeasuresByMeasureKey = from(pastMeasures).uniqueIndex(pastMeasureToMeasureKey);
      for (Map.Entry<String, Measure> entry : from(measureRepository.getRawMeasures(component).entries()).filter(NotDeveloperMeasure.INSTANCE)) {
        String metricKey = entry.getKey();
        Measure measure = entry.getValue();
        PastMeasureDto pastMeasure = pastMeasuresByMeasureKey.get(new MeasureKey(metricKey, null));
        if (pastMeasure != null && pastMeasure.hasValue()) {
          Metric metric = metricByKeys.get(metricKey);
          measuresWithVariationRepository.add(metric, measure, period, computeVariation(measure, pastMeasure.getValue()));
        }
      }
    }

    private double computeVariation(Measure measure, Double pastValue) {
      switch (measure.getValueType()) {
        case INT:
          return measure.getIntValue() - pastValue;
        case LONG:
          return measure.getLongValue() - pastValue;
        case DOUBLE:
          return measure.getDoubleValue() - pastValue;
        case BOOLEAN:
          return (measure.getBooleanValue() ? 1d : 0d) - pastValue;
        default:
          throw new IllegalArgumentException("Unsupported Measure.ValueType " + measure.getValueType());
      }
    }
  }

  private static final class MeasuresWithVariationRepository {

    private final Map<MeasureKey, MeasureWithVariations> measuresWithVariations = new HashMap<>();

    public void add(Metric metric, final Measure measure, int variationIndex, double variationValue) {
      checkArgument(measure.getDeveloper() == null, "%s does not support computing variations of Measures for Developer", getClass().getSimpleName());
      MeasureKey measureKey = new MeasureKey(metric.getKey(), null);
      MeasureWithVariations measureWithVariations = measuresWithVariations.get(measureKey);
      if (measureWithVariations == null) {
        measureWithVariations = new MeasureWithVariations(metric, measure);
        measuresWithVariations.put(measureKey, measureWithVariations);
      }
      measureWithVariations.setVariation(variationIndex, variationValue);
    }

    public Collection<MeasureWithVariations> measures() {
      return measuresWithVariations.values();
    }
  }

  private static final class MeasureWithVariations {
    private final Metric metric;
    private final Measure measure;
    private final Double[] variations = new Double[5];

    public MeasureWithVariations(Metric metric, Measure measure) {
      this.metric = metric;
      this.measure = measure;
    }

    public Measure getMeasure() {
      return measure;
    }

    public Metric getMetric() {
      return metric;
    }

    public void setVariation(int index, @Nullable Double value) {
      variations[index - 1] = value;
    }

    @CheckForNull
    public Double getVariation(int index) {
      return variations[index - 1];
    }
  }

  private enum MetricDtoToMetricId implements Function<Metric, Integer> {
    INSTANCE;

    @Nullable
    @Override
    public Integer apply(@Nonnull Metric metric) {
      return metric.getId();
    }
  }

  private enum MetricToKey implements Function<Metric, String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply(@Nonnull Metric metric) {
      return metric.getKey();
    }
  }

  private enum NumericMetric implements Predicate<Metric> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Metric metric) {
      Measure.ValueType valueType = metric.getType().getValueType();
      return Measure.ValueType.INT.equals(valueType)
        || Measure.ValueType.LONG.equals(valueType)
        || Measure.ValueType.DOUBLE.equals(valueType)
        || Measure.ValueType.BOOLEAN.equals(valueType);
    }
  }

  private enum NotDeveloperMeasure implements Predicate<Map.Entry<String, Measure>> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Map.Entry<String, Measure> input) {
      return input.getValue().getDeveloper() == null;
    }
  }

  @Override
  public String getDescription() {
    return "Compute measure variations";
  }
}
