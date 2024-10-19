/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import java.util.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.RatingMeasures;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.measure.Rating;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;

/**
 * Compute measures related to maintainability for projects and descendants :
 * {@link CoreMetrics#DEVELOPMENT_COST_KEY}
 * {@link CoreMetrics#SQALE_DEBT_RATIO_KEY}
 * {@link CoreMetrics#SQALE_RATING_KEY}
 * {@link CoreMetrics#EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY}
 * {@link SoftwareQualitiesMetrics#EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY}
 */
public class MaintainabilityMeasuresVisitor extends PathAwareVisitorAdapter<MaintainabilityMeasuresVisitor.Counter> {
  private final MeasureRepository measureRepository;
  private final RatingSettings ratingSettings;

  private final Metric nclocMetric;
  private final Metric developmentCostMetric;

  // Maintainability metrics based on RuleType
  private final Metric maintainabilityRemediationEffortMetric;
  private final Metric debtRatioMetric;
  private final Metric maintainabilityRatingMetric;
  private final Metric effortToMaintainabilityRatingAMetric;

  // Maintainability metrics based on Software Quality
  private final Metric softwareQualityMaintainabilityRemediationEffortMetric;
  private final Metric softwareQualityDebtRatioMetric;
  private final Metric softwareQualityEffortToMaintainabilityRatingAMetric;
  private final Metric softwareQualityMaintainabilityRatingMetric;

  public MaintainabilityMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, RatingSettings ratingSettings) {
    super(CrawlerDepthLimit.FILE, Order.POST_ORDER, CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.ratingSettings = ratingSettings;

    // Input metrics
    this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
    this.maintainabilityRemediationEffortMetric = metricRepository.getByKey(TECHNICAL_DEBT_KEY);
    this.softwareQualityMaintainabilityRemediationEffortMetric = metricRepository.getByKey(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY);

    // Output metrics
    this.developmentCostMetric = metricRepository.getByKey(DEVELOPMENT_COST_KEY);

    this.debtRatioMetric = metricRepository.getByKey(SQALE_DEBT_RATIO_KEY);
    this.maintainabilityRatingMetric = metricRepository.getByKey(SQALE_RATING_KEY);
    this.effortToMaintainabilityRatingAMetric = metricRepository.getByKey(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY);

    this.softwareQualityDebtRatioMetric = metricRepository.getByKey(SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY);
    this.softwareQualityMaintainabilityRatingMetric = metricRepository.getByKey(SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY);
    this.softwareQualityEffortToMaintainabilityRatingAMetric = metricRepository.getByKey(EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY);
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
  public void visitFile(Component file, Path<Counter> path) {
    path.current().addDevCosts(computeDevelopmentCost(file));
    computeAndSaveMeasures(file, path);
  }

  private long computeDevelopmentCost(Component file) {
    Optional<Measure> measure = measureRepository.getRawMeasure(file, nclocMetric);
    long ncloc = measure.map(Measure::getIntValue).orElse(0);
    return ncloc * ratingSettings.getDevCost();
  }

  private void computeAndSaveMeasures(Component component, Path<Counter> path) {
    addDevelopmentCostMeasure(component, path.current());

    double density = computeDensity(maintainabilityRemediationEffortMetric, component, path.current());
    addDebtRatioMeasure(debtRatioMetric, component, density);
    addMaintainabilityRatingMeasure(component, density);
    addEffortToMaintainabilityRatingAMeasure(maintainabilityRemediationEffortMetric, effortToMaintainabilityRatingAMetric, component, path);

    double densityBasedOnSoftwareQuality = computeDensity(softwareQualityMaintainabilityRemediationEffortMetric, component, path.current());
    addDebtRatioMeasure(softwareQualityDebtRatioMetric, component, densityBasedOnSoftwareQuality);
    addSoftwareQualityMaintainabilityRatingMeasure(component, densityBasedOnSoftwareQuality);
    addEffortToMaintainabilityRatingAMeasure(softwareQualityMaintainabilityRemediationEffortMetric, softwareQualityEffortToMaintainabilityRatingAMetric, component, path);

    addToParent(path);
  }

  private double computeDensity(Metric remediationEffortMetric, Component component, Counter developmentCost) {
    Optional<Measure> measure = measureRepository.getRawMeasure(component, remediationEffortMetric);
    double maintainabilityRemediationEffort = measure.isPresent() ? measure.get().getLongValue() : 0L;
    if (Double.doubleToRawLongBits(developmentCost.devCosts) != 0L) {
      return maintainabilityRemediationEffort / developmentCost.devCosts;
    }
    return 0D;
  }

  private void addDevelopmentCostMeasure(Component component, Counter developmentCost) {
    // the value of this measure is stored as a string because it can exceed the size limit of number storage on some DB
    measureRepository.add(component, developmentCostMetric, newMeasureBuilder().create(Long.toString(developmentCost.devCosts)));
  }

  private void addDebtRatioMeasure(Metric debtRatioMetric, Component component, double density) {
    measureRepository.add(component, debtRatioMetric, newMeasureBuilder().create(100.0 * density, debtRatioMetric.getDecimalScale()));
  }

  private void addMaintainabilityRatingMeasure(Component component, double density) {
    Rating rating = ratingSettings.getDebtRatingGrid().getRatingForDensity(density);
    measureRepository.add(component, maintainabilityRatingMetric, RatingMeasures.get(rating));
  }

  private void addSoftwareQualityMaintainabilityRatingMeasure(Component component, double density) {
    Rating rating = ratingSettings.getDebtRatingGrid().getRatingForDensity(density);
    measureRepository.add(component, softwareQualityMaintainabilityRatingMetric, RatingMeasures.get(rating));
  }

  private void addEffortToMaintainabilityRatingAMeasure(Metric maintainabilityRemediationEffortMetric, Metric effortToMaintainabilityRatingAMetric,
    Component component, Path<Counter> path) {
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

    private Counter() {
      // prevents instantiation
    }

    void add(Counter otherCounter) {
      addDevCosts(otherCounter.devCosts);
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
