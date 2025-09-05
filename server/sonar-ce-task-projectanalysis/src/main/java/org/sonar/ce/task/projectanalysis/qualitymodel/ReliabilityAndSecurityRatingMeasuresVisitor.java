/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Map;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.formula.counter.RatingValue;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.RatingMeasures;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.server.measure.Rating;

import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.core.rule.RuleType.VULNERABILITY;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.FILE;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.server.measure.Rating.RATING_BY_SEVERITY;

/**
 * Compute following measures for projects and descendants:
 * {@link CoreMetrics#RELIABILITY_RATING_KEY}
 * {@link CoreMetrics#SECURITY_RATING_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_RELIABILITY_RATING_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_SECURITY_RATING_KEY}
 */
public class ReliabilityAndSecurityRatingMeasuresVisitor extends PathAwareVisitorAdapter<ReliabilityAndSecurityRatingMeasuresVisitor.Counter> {

  private final MeasureRepository measureRepository;
  private final ComponentIssuesRepository componentIssuesRepository;
  private final Map<String, Metric> metricsByKey;

  public ReliabilityAndSecurityRatingMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository,
    ComponentIssuesRepository componentIssuesRepository) {
    super(FILE, Order.POST_ORDER, CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.componentIssuesRepository = componentIssuesRepository;

    // Output metrics
    Metric reliabilityRatingMetric = metricRepository.getByKey(RELIABILITY_RATING_KEY);
    Metric securityRatingMetric = metricRepository.getByKey(SECURITY_RATING_KEY);
    Metric softwareQualityReliabilityRatingMetric = metricRepository.getByKey(SOFTWARE_QUALITY_RELIABILITY_RATING_KEY);
    Metric softwareQualitySecurityRatingMetric = metricRepository.getByKey(SOFTWARE_QUALITY_SECURITY_RATING_KEY);

    this.metricsByKey = Map.of(
      RELIABILITY_RATING_KEY, reliabilityRatingMetric,
      SECURITY_RATING_KEY, securityRatingMetric,
      SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, softwareQualityReliabilityRatingMetric,
      SOFTWARE_QUALITY_SECURITY_RATING_KEY, softwareQualitySecurityRatingMetric)
    ;
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
    computeAndSaveMeasures(file, path);
  }

  private void computeAndSaveMeasures(Component component, Path<Counter> path) {
    processIssues(component, path);
    path.current().ratingValueByMetric.forEach((key, value) -> {
      Rating rating = value.getValue();
      measureRepository.add(component, metricsByKey.get(key), RatingMeasures.get(rating));
    });
    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  private void processIssues(Component component, Path<Counter> path) {
    componentIssuesRepository.getNotSandboxedIssues(component)
      .stream()
      .filter(issue -> issue.resolution() == null)
      .forEach(issue -> processIssue(path, issue));
  }

  private static void processIssue(Path<Counter> path, DefaultIssue issue) {
    Rating rating = RATING_BY_SEVERITY.get(issue.severity());
    if (issue.type().equals(BUG)) {
      path.current().ratingValueByMetric.get(RELIABILITY_RATING_KEY).increment(rating);
    } else if (issue.type().equals(VULNERABILITY)) {
      path.current().ratingValueByMetric.get(SECURITY_RATING_KEY).increment(rating);
    }

    processSoftwareQualityRating(path, issue, SoftwareQuality.RELIABILITY, SOFTWARE_QUALITY_RELIABILITY_RATING_KEY);
    processSoftwareQualityRating(path, issue, SoftwareQuality.SECURITY, SOFTWARE_QUALITY_SECURITY_RATING_KEY);
  }

  private static void processSoftwareQualityRating(Path<Counter> path, DefaultIssue issue, SoftwareQuality softwareQuality,
    String metricKey) {
    Severity severity = issue.impacts().get(softwareQuality);
    if (severity != null) {
      Rating rating = Rating.RATING_BY_SOFTWARE_QUALITY_SEVERITY.get(severity);

      if (rating != null) {
        path.current().ratingValueByMetric.get(metricKey).increment(rating);
      }
    }
  }

  static final class Counter {
    private final Map<String, RatingValue> ratingValueByMetric = Map.of(
      RELIABILITY_RATING_KEY, new RatingValue(),
      SECURITY_RATING_KEY, new RatingValue(),
      SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, new RatingValue(),
      SOFTWARE_QUALITY_SECURITY_RATING_KEY, new RatingValue());

    private Counter() {
      // prevents instantiation
    }

    void add(Counter otherCounter) {
      ratingValueByMetric.forEach((key, value) -> value.increment(otherCounter.ratingValueByMetric.get(key)));
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
