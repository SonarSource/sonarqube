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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.formula.counter.RatingVariationValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;

import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * Compute following measures :
 * {@link CoreMetrics#DEVELOPMENT_COST_KEY}
 * {@link CoreMetrics#SQALE_DEBT_RATIO_KEY}
 * {@link CoreMetrics#SQALE_RATING_KEY}
 * {@link CoreMetrics#EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY}
 */
public class QualityModelMeasuresVisitor extends PathAwareVisitorAdapter<QualityModelMeasuresVisitor.QualityModelCounter> {
  private static final Logger LOG = Loggers.get(QualityModelMeasuresVisitor.class);

  private final MeasureRepository measureRepository;
  private final RatingSettings ratingSettings;
  private final RatingGrid ratingGrid;

  private final Metric nclocMetric;
  private final Metric developmentCostMetric;

  private final Metric maintainabilityRemediationEffortMetric;
  private final Metric debtRatioMetric;
  private final Metric maintainabilityRatingMetric;
  private final Metric effortToMaintainabilityRatingAMetric;

  public QualityModelMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, RatingSettings ratingSettings) {
    super(CrawlerDepthLimit.LEAVES, POST_ORDER, QualityMododelCounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.ratingSettings = ratingSettings;
    this.ratingGrid = ratingSettings.getRatingGrid();

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
  public void visitProject(Component project, Path<QualityModelCounter> path) {
    computeAndSaveMeasures(project, path);
  }

  @Override
  public void visitDirectory(Component directory, Path<QualityModelCounter> path) {
    computeAndSaveMeasures(directory, path);
  }

  @Override
  public void visitModule(Component module, Path<QualityModelCounter> path) {
    computeAndSaveMeasures(module, path);
  }

  @Override
  public void visitFile(Component file, Path<QualityModelCounter> path) {
    path.current().addDevCosts(computeDevelopmentCost(file));
    computeAndSaveMeasures(file, path);
  }

  private long computeDevelopmentCost(Component file) {
    Optional<Measure> measure = measureRepository.getRawMeasure(file, nclocMetric);
    long ncloc = measure.isPresent() ? measure.get().getIntValue() : 0;
    return ncloc * ratingSettings.getDevCost(file.getFileAttributes().getLanguageKey());
  }

  @Override
  public void visitView(Component view, Path<QualityModelCounter> path) {
    computeAndSaveMeasures(view, path);
  }

  @Override
  public void visitSubView(Component subView, Path<QualityModelCounter> path) {
    computeAndSaveMeasures(subView, path);
  }

  @Override
  public void visitProjectView(Component projectView, Path<QualityModelCounter> path) {
    Optional<Measure> developmentCostMeasure = measureRepository.getRawMeasure(projectView, developmentCostMetric);
    if (developmentCostMeasure.isPresent()) {
      try {
        path.parent().addDevCosts(Long.valueOf(developmentCostMeasure.get().getStringValue()));
      } catch (NumberFormatException e) {
        LOG.trace("Failed to parse value of metric {} for component {}", developmentCostMetric.getName(), projectView.getKey());
      }
    }
  }

  private void computeAndSaveMeasures(Component component, Path<QualityModelCounter> path) {
    addDevelopmentCostMeasure(component, path.current());

    double density = computeDensity(component, path.current());
    addDebtRatioMeasure(component, density);
    addMaintainabilityRatingMeasure(component, density);
    addEffortToMaintainabilityRatingAMeasure(component, path);

    addToParent(path);
  }

  private double computeDensity(Component component, QualityModelCounter developmentCost) {
    Optional<Measure> measure = measureRepository.getRawMeasure(component, maintainabilityRemediationEffortMetric);
    double maintainabilityRemediationEffort = measure.isPresent() ? measure.get().getLongValue() : 0L;
    if (Double.doubleToRawLongBits(developmentCost.devCosts) != 0L) {
      return maintainabilityRemediationEffort / (double) developmentCost.devCosts;
    }
    return 0d;
  }

  private void addDevelopmentCostMeasure(Component component, QualityModelCounter developmentCost) {
    // the value of this measure is stored as a string because it can exceed the size limit of number storage on some DB
    measureRepository.add(component, developmentCostMetric, newMeasureBuilder().create(Long.toString(developmentCost.devCosts)));
  }

  private void addDebtRatioMeasure(Component component, double density) {
    measureRepository.add(component, debtRatioMetric, newMeasureBuilder().create(100.0 * density, debtRatioMetric.getDecimalScale()));
  }

  private void addMaintainabilityRatingMeasure(Component component, double density) {
    Rating rating = ratingGrid.getRatingForDensity(density);
    measureRepository.add(component, maintainabilityRatingMetric, newMeasureBuilder().create(rating.getIndex(), rating.name()));
  }

  private void addEffortToMaintainabilityRatingAMeasure(Component component, Path<QualityModelCounter> path) {
    long developmentCostValue = path.current().devCosts;
    Optional<Measure> effortMeasure = measureRepository.getRawMeasure(component, maintainabilityRemediationEffortMetric);
    long effort = effortMeasure.isPresent() ? effortMeasure.get().getLongValue() : 0L;
    long upperGradeCost = ((Double) (ratingGrid.getGradeLowerBound(Rating.B) * developmentCostValue)).longValue();
    long effortToRatingA = upperGradeCost < effort ? (effort - upperGradeCost) : 0L;
    measureRepository.add(component, effortToMaintainabilityRatingAMetric, Measure.newMeasureBuilder().create(effortToRatingA));
  }

  private static void addToParent(Path<QualityModelCounter> path) {
    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  public static final class QualityModelCounter {
    private long devCosts = 0;
    private RatingVariationValue reliabilityRating = new RatingVariationValue();
    private RatingVariationValue securityRating = new RatingVariationValue();

    private QualityModelCounter() {
      // prevents instantiation
    }

    public void add(QualityModelCounter otherCounter) {
      addDevCosts(otherCounter.devCosts);
      reliabilityRating.increment(otherCounter.reliabilityRating);
      securityRating.increment(otherCounter.securityRating);
    }

    public void addDevCosts(long developmentCosts) {
      this.devCosts += developmentCosts;
    }
  }

  private static final class QualityMododelCounterFactory extends SimpleStackElementFactory<QualityModelCounter> {
    public static final QualityMododelCounterFactory INSTANCE = new QualityMododelCounterFactory();

    private QualityMododelCounterFactory() {
      // prevents instantiation
    }

    @Override
    public QualityModelCounter createForAny(Component component) {
      return new QualityModelCounter();
    }

    /** Counter is not used at ProjectView level, saves on instantiating useless objects */
    @Override
    public QualityModelCounter createForProjectView(Component projectView) {
      return null;
    }
  }
}
