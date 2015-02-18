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

package org.sonar.batch.debt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Decorator that computes Sqale Rating metric
 */
public class SqaleRatingDecorator implements Decorator {

  private final SqaleRatingSettings sqaleRatingSettings;
  private final Metric[] metrics;
  private final FileSystem fs;

  public SqaleRatingDecorator(SqaleRatingSettings sqaleRatingSettings, Metric[] metrics, FileSystem fs) {
    this.sqaleRatingSettings = sqaleRatingSettings;
    this.metrics = Arrays.copyOf(metrics, metrics.length);
    this.fs = fs;
  }

  @VisibleForTesting
  SqaleRatingDecorator() {
    this.sqaleRatingSettings = null;
    this.metrics = null;
    this.fs = null;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public List<Metric> dependsOnMetrics() {
    return Lists.<Metric>newArrayList(CoreMetrics.TECHNICAL_DEBT,
      // ncloc and complexity are the two possible metrics to be used to calculate the development cost
      CoreMetrics.NCLOC, CoreMetrics.COMPLEXITY);
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return Lists.<Metric>newArrayList(CoreMetrics.SQALE_RATING, CoreMetrics.DEVELOPMENT_COST, CoreMetrics.SQALE_DEBT_RATIO);
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isPersistable(resource) && !ResourceUtils.isUnitTestFile(resource)) {
      Long developmentCost = getDevelopmentCost(context);
      context.saveMeasure(new Measure(CoreMetrics.DEVELOPMENT_COST, Long.toString(developmentCost)));

      long debt = getMeasureValue(context, CoreMetrics.TECHNICAL_DEBT);
      double density = computeDensity(debt, developmentCost);
      context.saveMeasure(CoreMetrics.SQALE_DEBT_RATIO, 100.0 * density);

      SqaleRatingGrid ratingGrid = new SqaleRatingGrid(sqaleRatingSettings.getRatingGrid());
      context.saveMeasure(createRatingMeasure(ratingGrid.getRatingForDensity(density)));
    }
  }

  private Measure createRatingMeasure(int rating) {
    return new Measure(CoreMetrics.SQALE_RATING).setIntValue(rating).setData(toRatingLetter(rating));
  }

  static String toRatingLetter(@Nullable Integer rating) {
    if (rating != null) {
      return SqaleRatingGrid.SqaleRating.createForIndex(rating).name();
    }
    return null;
  }

  private long getDevelopmentCost(DecoratorContext context) {
    InputFile file = fs.inputFile(fs.predicates().hasRelativePath(context.getResource().getKey()));
    if (file != null) {
      String language = file.language();
      return getMeasureValue(context, sqaleRatingSettings.getSizeMetric(language, metrics)) * sqaleRatingSettings.getDevCost(language);
    } else {
      Collection<Measure> childrenMeasures = context.getChildrenMeasures(CoreMetrics.DEVELOPMENT_COST);
      Double sum = sum(childrenMeasures);
      return sum.longValue();
    }
  }

  private static Double sum(@Nullable Collection<Measure> measures) {
    if (measures == null) {
      return 0d;
    }
    double sum = 0d;
    for (Measure measure : measures) {
      String data = measure.getData();
      if (data != null) {
        sum += Double.parseDouble(data);
      }
    }
    return sum;
  }

  private long getMeasureValue(DecoratorContext context, Metric metric) {
    Measure measure = context.getMeasure(metric);
    if (measure != null) {
      return measure.getValue().longValue();
    }
    return 0L;
  }

  protected double computeDensity(double debt, double developmentCost) {
    if (Double.doubleToRawLongBits(developmentCost) != 0L) {
      return debt / developmentCost;
    }
    return 0d;
  }

}
