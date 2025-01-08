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
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.formula.counter.RatingValue;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.issue.NewIssueClassifier;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.measure.Rating;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.LEAVES;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY;

/**
 * Compute following measures :
 * {@link CoreMetrics#NEW_RELIABILITY_RATING_KEY}
 * {@link CoreMetrics#NEW_SECURITY_RATING_KEY}
 * {@link SoftwareQualitiesMetrics#NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY}
 * {@link SoftwareQualitiesMetrics#NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY}
 */
public class NewReliabilityAndSecurityRatingMeasuresVisitor extends PathAwareVisitorAdapter<NewReliabilityAndSecurityRatingMeasuresVisitor.Counter> {

  private static final Map<String, Rating> RATING_BY_SEVERITY = Map.of(
    BLOCKER, E,
    CRITICAL, D,
    MAJOR, C,
    MINOR, B,
    INFO, A);

  private final MeasureRepository measureRepository;
  private final ComponentIssuesRepository componentIssuesRepository;
  private final Map<String, Metric> metricsByKey;
  private final NewIssueClassifier newIssueClassifier;

  public NewReliabilityAndSecurityRatingMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository,
    ComponentIssuesRepository componentIssuesRepository, NewIssueClassifier newIssueClassifier) {
    super(LEAVES, POST_ORDER, new CounterFactory(newIssueClassifier));
    this.measureRepository = measureRepository;
    this.componentIssuesRepository = componentIssuesRepository;

    // Output metrics
    this.metricsByKey = Map.of(
      NEW_RELIABILITY_RATING_KEY, metricRepository.getByKey(NEW_RELIABILITY_RATING_KEY),
      NEW_SECURITY_RATING_KEY, metricRepository.getByKey(NEW_SECURITY_RATING_KEY),
      NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, metricRepository.getByKey(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY),
      NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, metricRepository.getByKey(NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY));
    this.newIssueClassifier = newIssueClassifier;
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
    if (!newIssueClassifier.isEnabled()) {
      return;
    }
    initRatingsToA(path);
    processIssues(component, path);
    processIssuesForSoftwareQuality(component, path);
    path.current().newRatingValueByMetric.entrySet()
      .stream()
      .filter(entry -> entry.getValue().isSet())
      .forEach(
        entry -> measureRepository.add(
          component,
          metricsByKey.get(entry.getKey()),
          newMeasureBuilder().create(entry.getValue().getValue().getIndex())));
    addToParent(path);
  }

  private static void initRatingsToA(Path<Counter> path) {
    path.current().newRatingValueByMetric.values().forEach(entry -> entry.increment(A));
  }

  private void processIssues(Component component, Path<Counter> path) {
    componentIssuesRepository.getIssues(component)
      .stream()
      .filter(issue -> issue.resolution() == null)
      .filter(issue -> issue.type().equals(BUG) || issue.type().equals(VULNERABILITY))
      .forEach(issue -> path.current().processIssue(issue));
  }

  private void processIssuesForSoftwareQuality(Component component, Path<Counter> path) {
    componentIssuesRepository.getIssues(component)
      .stream()
      .filter(issue -> issue.resolution() == null)
      .forEach(issue -> path.current().processIssueForSoftwareQuality(issue));
  }

  private static void addToParent(Path<Counter> path) {
    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  static class Counter {
    private final Map<String, RatingValue> newRatingValueByMetric = Map.of(
      NEW_RELIABILITY_RATING_KEY, new RatingValue(),
      NEW_SECURITY_RATING_KEY, new RatingValue(),
      NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, new RatingValue(),
      NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, new RatingValue());
    private final NewIssueClassifier newIssueClassifier;
    private final Component component;

    public Counter(NewIssueClassifier newIssueClassifier, Component component) {
      this.newIssueClassifier = newIssueClassifier;
      this.component = component;
    }

    void add(Counter otherCounter) {
      newRatingValueByMetric.forEach((metric, rating) -> rating.increment(otherCounter.newRatingValueByMetric.get(metric)));
    }

    void processIssue(Issue issue) {
      if (newIssueClassifier.isNew(component, (DefaultIssue) issue)) {
        Rating rating = RATING_BY_SEVERITY.get(issue.severity());
        if (issue.type().equals(BUG)) {
          newRatingValueByMetric.get(NEW_RELIABILITY_RATING_KEY).increment(rating);
        } else if (issue.type().equals(VULNERABILITY)) {
          newRatingValueByMetric.get(NEW_SECURITY_RATING_KEY).increment(rating);
        }
      }
    }

    void processIssueForSoftwareQuality(Issue issue) {
      if (newIssueClassifier.isNew(component, (DefaultIssue) issue)) {
        processSoftwareQualityRating(issue, SoftwareQuality.RELIABILITY, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY);
        processSoftwareQualityRating(issue, SoftwareQuality.SECURITY, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY);
      }
    }

    private void processSoftwareQualityRating(Issue issue, SoftwareQuality softwareQuality, String metricKey) {
      Severity severity = issue.impacts().get(softwareQuality);
      if (severity != null) {
        Rating rating = Rating.RATING_BY_SOFTWARE_QUALITY_SEVERITY.get(severity);

        if (rating != null) {
          newRatingValueByMetric.get(metricKey).increment(rating);
        }
      }
    }
  }

  private static final class CounterFactory extends SimpleStackElementFactory<NewReliabilityAndSecurityRatingMeasuresVisitor.Counter> {
    private final NewIssueClassifier newIssueClassifier;

    private CounterFactory(NewIssueClassifier newIssueClassifier) {
      this.newIssueClassifier = newIssueClassifier;
    }

    @Override
    public Counter createForAny(Component component) {
      return new Counter(newIssueClassifier, component);
    }
  }
}
