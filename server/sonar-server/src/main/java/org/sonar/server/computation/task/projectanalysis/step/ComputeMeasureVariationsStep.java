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

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.PastMeasureDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureKey;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Set variations on all numeric measures found in the repository.
 * This step MUST be executed after all steps that create some measures
 * <p/>
 * Note that measures on developer are not handle yet.
 */
public class ComputeMeasureVariationsStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final PeriodHolder periodHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public ComputeMeasureVariationsStep(DbClient dbClient, TreeRootHolder treeRootHolder, PeriodHolder periodHolder, MetricRepository metricRepository,
                                      MeasureRepository measureRepository) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.periodHolder = periodHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<Metric> metrics = StreamSupport.stream(metricRepository.getAll().spliterator(), false).filter(isNumeric()).collect(MoreCollectors.toList());
      new DepthTraversalTypeAwareCrawler(new VariationMeasuresVisitor(dbSession, metrics))
        .visit(treeRootHolder.getRoot());
    }
  }

  private class VariationMeasuresVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;
    private final Set<Integer> metricIds;
    private final List<Metric> metrics;

    VariationMeasuresVisitor(DbSession session, List<Metric> metrics) {
      // measures on files are currently purged, so past measures are not available on files
      super(CrawlerDepthLimit.reportMaxDepth(DIRECTORY).withViewsMaxDepth(SUBVIEW), PRE_ORDER);
      this.session = session;
      this.metricIds = metrics.stream().map(Metric::getId).collect(MoreCollectors.toSet());
      this.metrics = metrics;
    }

    @Override
    public void visitAny(Component component) {
      MeasuresWithVariationRepository measuresWithVariationRepository = computeMeasuresWithVariations(component);
      processMeasuresWithVariation(component, measuresWithVariationRepository);
    }

    private MeasuresWithVariationRepository computeMeasuresWithVariations(Component component) {
      MeasuresWithVariationRepository measuresWithVariationRepository = new MeasuresWithVariationRepository();
      if (periodHolder.hasPeriod()) {
        Period period = periodHolder.getPeriod();
        List<PastMeasureDto> pastMeasures = dbClient.measureDao()
          .selectPastMeasures(session, component.getUuid(), period.getAnalysisUuid(), metricIds);
        setVariationMeasures(component, pastMeasures, measuresWithVariationRepository);
      }
      return measuresWithVariationRepository;
    }

    private void setVariationMeasures(Component component, List<PastMeasureDto> pastMeasures, MeasuresWithVariationRepository measuresWithVariationRepository) {
      Map<MeasureKey, PastMeasureDto> pastMeasuresByMeasureKey = pastMeasures
        .stream()
        .collect(uniqueIndex(m -> new MeasureKey(metricRepository.getById((long) m.getMetricId()).getKey(), null), identity()));
      for (Metric metric : metrics) {
        Optional<Measure> measure = measureRepository.getRawMeasure(component, metric);
        if (measure.isPresent() && !measure.get().hasVariation()) {
          PastMeasureDto pastMeasure = pastMeasuresByMeasureKey.get(new MeasureKey(metric.getKey(), null));
          double pastValue = (pastMeasure != null && pastMeasure.hasValue()) ? pastMeasure.getValue() : 0d;
          measuresWithVariationRepository.add(metric, measure.get(), computeVariation(measure.get(), pastValue));
        }
      }
    }

    private double computeVariation(Measure measure, double pastValue) {
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
          throw new IllegalArgumentException(format("Unsupported Measure.ValueType on measure '%s'", measure));
      }
    }

    private void processMeasuresWithVariation(Component component, MeasuresWithVariationRepository measuresWithVariationRepository) {
      for (MeasureWithVariation measureWithVariation : measuresWithVariationRepository.measures()) {
        Metric metric = measureWithVariation.getMetric();
        Measure measure = Measure.updatedMeasureBuilder(measureWithVariation.getMeasure())
          .setVariation(measureWithVariation.getVariation())
          .create();
        measureRepository.update(component, metric, measure);
      }
    }
  }

  private static final class MeasuresWithVariationRepository {

    private final Map<MeasureKey, MeasureWithVariation> measuresWithVariations = new HashMap<>();

    public void add(Metric metric, final Measure measure, double variationValue) {
      checkArgument(measure.getDeveloper() == null, "%s does not support computing variations of Measures for Developer", getClass().getSimpleName());
      MeasureKey measureKey = new MeasureKey(metric.getKey(), null);
      MeasureWithVariation measureWithVariation = measuresWithVariations.computeIfAbsent(measureKey, k -> new MeasureWithVariation(metric, measure));
      measureWithVariation.setVariation(variationValue);
    }

    public Collection<MeasureWithVariation> measures() {
      return measuresWithVariations.values();
    }
  }

  private static final class MeasureWithVariation {
    private final Metric metric;
    private final Measure measure;
    private Double variation;

    MeasureWithVariation(Metric metric, Measure measure) {
      this.metric = metric;
      this.measure = measure;
    }

    public Measure getMeasure() {
      return measure;
    }

    public Metric getMetric() {
      return metric;
    }

    public void setVariation(@Nullable Double value) {
      this.variation = value;
    }

    @CheckForNull
    public Double getVariation() {
      return variation;
    }
  }

  private static Predicate<Metric> isNumeric() {
    return metric -> {
      Measure.ValueType valueType = metric.getType().getValueType();
      return Measure.ValueType.INT.equals(valueType)
        || Measure.ValueType.LONG.equals(valueType)
        || Measure.ValueType.DOUBLE.equals(valueType)
        || Measure.ValueType.BOOLEAN.equals(valueType);
    };
  }

  @Override
  public String getDescription() {
    return "Compute measure variations";
  }
}
