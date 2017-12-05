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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.formula.counter.RatingValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * Compute measures related to maintainability for projects and descendants :
 * {@link CoreMetrics#DEVELOPMENT_COST_KEY}
 * {@link CoreMetrics#SQALE_DEBT_RATIO_KEY}
 * {@link CoreMetrics#SQALE_RATING_KEY}
 * {@link CoreMetrics#EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY}
 */
public class MaintainabilityMeasuresVisitor extends PathAwareVisitorAdapter<MaintainabilityMeasuresVisitor.Counter> {
  private final MeasureRepository measureRepository;
  private final RatingSettings ratingSettings;

  private final Metric nclocMetric;
  private final Metric developmentCostMetric;

  private final Metric maintainabilityRemediationEffortMetric;
  private final Metric debtRatioMetric;
  private final Metric maintainabilityRatingMetric;
  private final Metric effortToMaintainabilityRatingAMetric;

  public MaintainabilityMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, RatingSettings ratingSettings) {
    super(CrawlerDepthLimit.FILE, POST_ORDER, CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.ratingSettings = ratingSettings;

    // Input metrics
    this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
    this.maintainabilityRemediationEffortMetric = metricRepository.getByKey(TECHNICAL_DEBT_KEY);

    // Output metrics
    this.developmentCostMetric = metricRepository.getByKey(DEVELOPMENT_COST_KEY);
    this.debtRatioMetric = metricRepository.getByKey(SQALE_DEBT_RATIO_KEY);
    this.maintainabilityRatingMetric = metricRepository.getByKey(SQALE_RATING_KEY);
    this.effortToMaintainabilityRatingAMetric = metricRepository.getByKey(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY);
  }

  @Override
  public void visitProject(Component project, Path<Counter> path) {
    computeAndSaveMeasures(project, path);
  }

  @Override
  public void visitDirectory(Component directory, Path<Counter> path) {
    computeAndSaveMeasures(directory, path);
  }

  @Override
  public void visitModule(Component module, Path<Counter> path) {
    computeAndSaveMeasures(module, path);
  }

  @Override
  public void visitFile(Component file, Path<Counter> path) {
    path.current().addDevCosts(computeDevelopmentCost(file));
    computeAndSaveMeasures(file, path);
  }

  private long computeDevelopmentCost(Component file) {
    Optional<Measure> measure = measureRepository.getRawMeasure(file, nclocMetric);
    long ncloc = measure.isPresent() ? measure.get().getIntValue() : 0;
    return ncloc * ratingSettings.getDevCost(file.getFileAttributes().getLanguageKey());
  }

  private void computeAndSaveMeasures(Component component, Path<Counter> path) {
    addDevelopmentCostMeasure(component, path.current());

    double density = computeDensity(component, path.current());
    addDebtRatioMeasure(component, density);
    addMaintainabilityRatingMeasure(component, density);
    addEffortToMaintainabilityRatingAMeasure(component, path);

    addToParent(path);
  }

  private double computeDensity(Component component, Counter developmentCost) {
    Optional<Measure> measure = measureRepository.getRawMeasure(component, maintainabilityRemediationEffortMetric);
    double maintainabilityRemediationEffort = measure.isPresent() ? measure.get().getLongValue() : 0L;
    if (Double.doubleToRawLongBits(developmentCost.devCosts) != 0L) {
      return maintainabilityRemediationEffort / (double) developmentCost.devCosts;
    }
    return 0d;
  }

  private void addDevelopmentCostMeasure(Component component, Counter developmentCost) {
    // the value of this measure is stored as a string because it can exceed the size limit of number storage on some DB
    measureRepository.add(component, developmentCostMetric, newMeasureBuilder().create(Long.toString(developmentCost.devCosts)));
  }

  private void addDebtRatioMeasure(Component component, double density) {
    measureRepository.add(component, debtRatioMetric, newMeasureBuilder().create(100.0 * density, debtRatioMetric.getDecimalScale()));
  }

  private void addMaintainabilityRatingMeasure(Component component, double density) {
    Rating rating = ratingSettings.getDebtRatingGrid().getRatingForDensity(density);
    measureRepository.add(component, maintainabilityRatingMetric, newMeasureBuilder().create(rating.getIndex(), rating.name()));
  }

  private void addEffortToMaintainabilityRatingAMeasure(Component component, Path<Counter> path) {
    long developmentCostValue = path.current().devCosts;
    Optional<Measure> effortMeasure = measureRepository.getRawMeasure(component, maintainabilityRemediationEffortMetric);
    long effort = effortMeasure.isPresent() ? effortMeasure.get().getLongValue() : 0L;
    long upperGradeCost = ((Double) (ratingSettings.getDebtRatingGrid().getGradeLowerBound(Rating.B) * developmentCostValue)).longValue();
    long effortToRatingA = upperGradeCost < effort ? (effort - upperGradeCost) : 0L;
    measureRepository.add(component, effortToMaintainabilityRatingAMetric, Measure.newMeasureBuilder().create(effortToRatingA));
  }

  private static void addToParent(Path<Counter> path) {
    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  public static final class Counter {
    private long devCosts = 0;
    private RatingValue reliabilityRating = new RatingValue();
    private RatingValue securityRating = new RatingValue();

    private Counter() {
      // prevents instantiation
    }

    void add(Counter otherCounter) {
      addDevCosts(otherCounter.devCosts);
      reliabilityRating.increment(otherCounter.reliabilityRating);
      securityRating.increment(otherCounter.securityRating);
    }

    void addDevCosts(long developmentCosts) {
      this.devCosts += developmentCosts;
    }
  }

  private static final class CounterFactory extends SimpleStackElementFactory<Counter> {
    public static final CounterFactory INSTANCE = new CounterFactory();

    private CounterFactory() {
      // prevents instantiation
    }

    @Override
    public Counter createForAny(Component component) {
      return new Counter();
    }
  }
}
