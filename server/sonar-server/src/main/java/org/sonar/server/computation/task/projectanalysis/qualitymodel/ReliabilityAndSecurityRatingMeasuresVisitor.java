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
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.formula.counter.RatingVariationValue;
import org.sonar.server.computation.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;

import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
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
import static org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit.LEAVES;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.E;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating.valueOf;

/**
 * Compute following measures :
 * {@link CoreMetrics#RELIABILITY_RATING_KEY}
 * {@link CoreMetrics#SECURITY_RATING_KEY}
 * {@link CoreMetrics#NEW_RELIABILITY_RATING_KEY}
 * {@link CoreMetrics#NEW_SECURITY_RATING_KEY}
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
  private final PeriodsHolder periodsHolder;

  // Output metrics
  private final Metric reliabilityRatingMetric;
  private final Metric securityRatingMetric;
  private final Metric newReliabilityRatingMetric;
  private final Metric newSecurityRatingMetric;

  private final Map<String, Metric> metricsByKey;

  public ReliabilityAndSecurityRatingMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ComponentIssuesRepository componentIssuesRepository,
    PeriodsHolder periodsHolder) {
    super(LEAVES, POST_ORDER, CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.componentIssuesRepository = componentIssuesRepository;
    this.periodsHolder = periodsHolder;

    // Output metrics
    this.reliabilityRatingMetric = metricRepository.getByKey(RELIABILITY_RATING_KEY);
    this.securityRatingMetric = metricRepository.getByKey(SECURITY_RATING_KEY);
    this.newReliabilityRatingMetric = metricRepository.getByKey(NEW_RELIABILITY_RATING_KEY);
    this.newSecurityRatingMetric = metricRepository.getByKey(NEW_SECURITY_RATING_KEY);

    this.metricsByKey = ImmutableMap.of(
      RELIABILITY_RATING_KEY, reliabilityRatingMetric,
      SECURITY_RATING_KEY, securityRatingMetric,
      NEW_RELIABILITY_RATING_KEY, newReliabilityRatingMetric,
      NEW_SECURITY_RATING_KEY, newSecurityRatingMetric);
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

  @Override
  public void visitView(Component view, Path<Counter> path) {
    computeAndSaveMeasures(view, path);
  }

  @Override
  public void visitSubView(Component subView, Path<Counter> path) {
    computeAndSaveMeasures(subView, path);
  }

  @Override
  public void visitProjectView(Component projectView, Path<Counter> path) {
    path.parent().ratingValueByMetric.entrySet().forEach(entry -> {
      Optional<Measure> ratingMeasure = measureRepository.getRawMeasure(projectView, metricsByKey.get(entry.getKey()));
      if (ratingMeasure.isPresent()) {
        entry.getValue().increment(valueOf(ratingMeasure.get().getData()));
      }
    });
  }

  private void computeAndSaveMeasures(Component component, Path<Counter> path) {
    processIssues(component, path);
    path.current().ratingValueByMetric.entrySet().forEach(
      entry -> measureRepository.add(component, metricsByKey.get(entry.getKey()), createRatingMeasure(entry.getValue().getValue())));
    path.current().newRatingValueByMetric.entrySet().forEach(
      entry -> entry.getValue().toMeasureVariations()
        .ifPresent(measureVariations -> measureRepository.add(
          component,
          metricsByKey.get(entry.getKey()),
          newMeasureBuilder().setVariations(measureVariations).createNoValue())));
    addToParent(path);
  }

  private void processIssues(Component component, Path<Counter> path) {
    componentIssuesRepository.getIssues(component)
      .stream()
      .filter(issue -> issue.resolution() == null)
      .filter(issue -> issue.type().equals(BUG) || issue.type().equals(VULNERABILITY))
      .forEach(issue -> {
        path.current().processIssue(issue);
        periodsHolder.getPeriods().forEach(period -> path.current().processIssue(issue, period));
      });
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
    private Map<String, RatingVariationValue.Array> newRatingValueByMetric = ImmutableMap.of(
      NEW_RELIABILITY_RATING_KEY, new RatingVariationValue.Array(),
      NEW_SECURITY_RATING_KEY, new RatingVariationValue.Array());

    private Counter() {
      // prevents instantiation
    }

    void add(Counter otherCounter) {
      ratingValueByMetric.entrySet().forEach(e -> e.getValue().increment(otherCounter.ratingValueByMetric.get(e.getKey())));
      newRatingValueByMetric.entrySet().forEach(e -> e.getValue().incrementAll(otherCounter.newRatingValueByMetric.get(e.getKey())));
    }

    void processIssue(Issue issue) {
      Rating rating = RATING_BY_SEVERITY.get(issue.severity());
      if (issue.type().equals(BUG)) {
        ratingValueByMetric.get(RELIABILITY_RATING_KEY).increment(rating);
      } else if (issue.type().equals(VULNERABILITY)) {
        ratingValueByMetric.get(SECURITY_RATING_KEY).increment(rating);
      }
    }

    void processIssue(Issue issue, Period period) {
      if (isOnPeriod((DefaultIssue) issue, period)) {
        Rating rating = RATING_BY_SEVERITY.get(issue.severity());
        if (issue.type().equals(BUG)) {
          newRatingValueByMetric.get(NEW_RELIABILITY_RATING_KEY).increment(period, rating);
        } else if (issue.type().equals(VULNERABILITY)) {
          newRatingValueByMetric.get(NEW_SECURITY_RATING_KEY).increment(period, rating);
        }
      }
    }

    private static boolean isOnPeriod(DefaultIssue issue, Period period) {
      // Add one second to not take into account issues created during current analysis
      return issue.creationDate().getTime() >= period.getSnapshotDate() + 1000L;
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

    /**
     * Counter is not used at ProjectView level, saves on instantiating useless objects
     */
    @Override
    public Counter createForProjectView(Component projectView) {
      return null;
    }
  }
}
