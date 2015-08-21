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

package org.sonar.server.computation.sqale;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class SqaleMeasuresVisitor extends PathAwareVisitorAdapter<SqaleMeasuresVisitor.DevelopmentCost> {
  private static final Logger LOG = Loggers.get(SqaleMeasuresVisitor.class);

  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final SqaleRatingSettings sqaleRatingSettings;

  private final Metric developmentCostMetric;
  private final Metric technicalDebtMetric;
  private final Metric debtRatioMetric;
  private final Metric sqaleRatingMetric;

  public SqaleMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, SqaleRatingSettings sqaleRatingSettings) {
    super(CrawlerDepthLimit.LEAVES, POST_ORDER, new SimpleStackElementFactory<DevelopmentCost>() {
      @Override
      public DevelopmentCost createForAny(Component component) {
        return new DevelopmentCost();
      }

      /** Counter is not used at ProjectView level, saves on instantiating useless objects */
      @Override
      public DevelopmentCost createForProjectView(Component projectView) {
        return null;
      }
    });
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.sqaleRatingSettings = sqaleRatingSettings;

    this.developmentCostMetric = this.metricRepository.getByKey(CoreMetrics.DEVELOPMENT_COST_KEY);
    this.technicalDebtMetric = this.metricRepository.getByKey(CoreMetrics.TECHNICAL_DEBT_KEY);
    this.debtRatioMetric = this.metricRepository.getByKey(CoreMetrics.SQALE_DEBT_RATIO_KEY);
    this.sqaleRatingMetric = this.metricRepository.getByKey(CoreMetrics.SQALE_RATING_KEY);
  }

  @Override
  public void visitProject(Component project, Path<DevelopmentCost> path) {
    computeAndSaveMeasures(project, path);
  }

  @Override
  public void visitDirectory(Component directory, Path<DevelopmentCost> path) {
    computeAndSaveMeasures(directory, path);
  }

  @Override
  public void visitModule(Component module, Path<DevelopmentCost> path) {
    computeAndSaveMeasures(module, path);
  }

  @Override
  public void visitFile(Component file, Path<DevelopmentCost> path) {
    if (!file.getFileAttributes().isUnitTest()) {
      long developmentCosts = computeDevelopmentCost(file);
      path.current().add(developmentCosts);
      computeAndSaveMeasures(file, path);
    }
  }

  @Override
  public void visitView(Component view, Path<DevelopmentCost> path) {
    computeAndSaveMeasures(view, path);
  }

  @Override
  public void visitSubView(Component subView, Path<DevelopmentCost> path) {
    computeAndSaveMeasures(subView, path);
  }

  @Override
  public void visitProjectView(Component projectView, Path<DevelopmentCost> path) {
    Optional<Measure> developmentCostMeasure = measureRepository.getRawMeasure(projectView, developmentCostMetric);
    if (developmentCostMeasure.isPresent()) {
      try {
        path.parent().add(Long.valueOf(developmentCostMeasure.get().getStringValue()));
      } catch (NumberFormatException e) {
        LOG.trace("Failed to parse value of metric {} for component {}", developmentCostMetric.getName(), projectView.getKey());
      }
    }
  }

  private void computeAndSaveMeasures(Component component, Path<DevelopmentCost> path) {
    saveDevelopmentCostMeasure(component, path.current());

    double density = computeDensity(component, path.current());
    saveDebtRatioMeasure(component, density);
    saveSqaleRatingMeasure(component, density);

    increaseParentDevelopmentCost(path);
  }

  private void saveDevelopmentCostMeasure(Component component, DevelopmentCost developmentCost) {
    // the value of this measure is stored as a string because it can exceed the size limit of number storage on some DB
    measureRepository.add(component, developmentCostMetric, newMeasureBuilder().create(Long.toString(developmentCost.getValue())));
  }

  private double computeDensity(Component component, DevelopmentCost developmentCost) {
    double debt = getLongValue(measureRepository.getRawMeasure(component, technicalDebtMetric));
    if (Double.doubleToRawLongBits(developmentCost.getValue()) != 0L) {
      return debt / (double) developmentCost.getValue();
    }
    return 0d;
  }

  private void saveDebtRatioMeasure(Component component, double density) {
    measureRepository.add(component, debtRatioMetric, newMeasureBuilder().create(100.0 * density));
  }

  private void saveSqaleRatingMeasure(Component component, double density) {
    SqaleRatingGrid ratingGrid = new SqaleRatingGrid(sqaleRatingSettings.getRatingGrid());
    int rating = ratingGrid.getRatingForDensity(density);
    String ratingLetter = toRatingLetter(rating);
    measureRepository.add(component, sqaleRatingMetric, newMeasureBuilder().create(rating, ratingLetter));
  }

  private void increaseParentDevelopmentCost(Path<DevelopmentCost> path) {
    if (!path.isRoot()) {
      // increase parent's developmentCost with our own
      path.parent().add(path.current().getValue());
    }
  }

  private long computeDevelopmentCost(Component file) {
    String languageKey = file.getFileAttributes().getLanguageKey();
    String sizeMetricKey = sqaleRatingSettings.getSizeMetricKey(languageKey);
    Metric sizeMetric = metricRepository.getByKey(sizeMetricKey);
    return getLongValue(measureRepository.getRawMeasure(file, sizeMetric)) * sqaleRatingSettings.getDevCost(languageKey);
  }

  private static long getLongValue(Optional<Measure> measure) {
    if (!measure.isPresent()) {
      return 0L;
    }
    return getLongValue(measure.get());
  }

  private static long getLongValue(Measure measure) {
    switch (measure.getValueType()) {
      case INT:
        return measure.getIntValue();
      case LONG:
        return measure.getLongValue();
      case DOUBLE:
        return (long) measure.getDoubleValue();
      default:
        return 0L;
    }
  }

  private static String toRatingLetter(int rating) {
    return SqaleRatingGrid.SqaleRating.createForIndex(rating).name();
  }

  /**
   * A wrapper class around a long which can be increased and represents the development cost of a Component
   */
  public static final class DevelopmentCost {
    private long value = 0;

    public void add(long developmentCosts) {
      this.value += developmentCosts;
    }

    public long getValue() {
      return value;
    }
  }
}
