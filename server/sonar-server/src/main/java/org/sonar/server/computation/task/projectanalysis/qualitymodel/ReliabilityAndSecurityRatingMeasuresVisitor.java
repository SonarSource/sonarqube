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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.formula.counter.RatingVariationValue;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit.FILE;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.E;

/**
 * Compute following measures for projects and descendants:
 * {@link CoreMetrics#RELIABILITY_RATING_KEY}
 * {@link CoreMetrics#SECURITY_RATING_KEY}
 */
public class ReliabilityAndSecurityRatingMeasuresVisitor extends PathAwareVisitorAdapter<ReliabilityAndSecurityRatingMeasuresVisitor.Counter> {

  private static final Map<String, Rating> RATING_BY_SEVERITY = ImmutableMap.of(
    BLOCKER, E,
    CRITICAL, D,
    MAJOR, C,
    MINOR, B,
    INFO, A);

  private final MeasureRepository measureRepository;
  private final ComponentIssuesRepository componentIssuesRepository;

  // Output metrics
  private final Metric reliabilityRatingMetric;
  private final Metric securityRatingMetric;

  private final Map<String, Metric> metricsByKey;

  public ReliabilityAndSecurityRatingMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ComponentIssuesRepository componentIssuesRepository) {
    super(FILE, POST_ORDER, CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.componentIssuesRepository = componentIssuesRepository;

    // Output metrics
    this.reliabilityRatingMetric = metricRepository.getByKey(RELIABILITY_RATING_KEY);
    this.securityRatingMetric = metricRepository.getByKey(SECURITY_RATING_KEY);

    this.metricsByKey = ImmutableMap.of(
      RELIABILITY_RATING_KEY, reliabilityRatingMetric,
      SECURITY_RATING_KEY, securityRatingMetric);
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
    computeAndSaveMeasures(file, path);
  }

  private void computeAndSaveMeasures(Component component, Path<Counter> path) {
    processIssues(component, path);
    path.current().ratingValueByMetric.entrySet().forEach(
      entry -> measureRepository.add(component, metricsByKey.get(entry.getKey()), createRatingMeasure(entry.getValue().getValue())));
    addToParent(path);
  }

  private void processIssues(Component component, Path<Counter> path) {
    componentIssuesRepository.getIssues(component)
      .stream()
      .filter(issue -> issue.resolution() == null)
      .filter(issue -> issue.type().equals(BUG) || issue.type().equals(VULNERABILITY))
      .forEach(path.current()::processIssue);
  }

  private static void addToParent(Path<Counter> path) {
    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  private static Measure createRatingMeasure(Rating rating) {
    return newMeasureBuilder().create(rating.getIndex(), rating.name());
  }

  static final class Counter {
    private Map<String, RatingVariationValue> ratingValueByMetric = ImmutableMap.of(
      RELIABILITY_RATING_KEY, new RatingVariationValue(),
      SECURITY_RATING_KEY, new RatingVariationValue());

    private Counter() {
      // prevents instantiation
    }

    void add(Counter otherCounter) {
      ratingValueByMetric.entrySet().forEach(e -> e.getValue().increment(otherCounter.ratingValueByMetric.get(e.getKey())));
    }

    void processIssue(Issue issue) {
      Rating rating = RATING_BY_SEVERITY.get(issue.severity());
      if (issue.type().equals(BUG)) {
        ratingValueByMetric.get(RELIABILITY_RATING_KEY).increment(rating);
      } else if (issue.type().equals(VULNERABILITY)) {
        ratingValueByMetric.get(SECURITY_RATING_KEY).increment(rating);
      }
    }
  }

  private static final class CounterFactory extends PathAwareVisitorAdapter.SimpleStackElementFactory<ReliabilityAndSecurityRatingMeasuresVisitor.Counter> {
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
