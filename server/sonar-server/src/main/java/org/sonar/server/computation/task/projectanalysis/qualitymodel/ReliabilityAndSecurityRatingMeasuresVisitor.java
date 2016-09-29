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
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
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
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * Compute following measures :
 * {@link CoreMetrics#RELIABILITY_RATING_KEY}
 * {@link CoreMetrics#SECURITY_RATING_KEY}
 */
public class ReliabilityAndSecurityRatingMeasuresVisitor extends PathAwareVisitorAdapter<ReliabilityAndSecurityRatingMeasuresVisitor.Counter> {

  private final MeasureRepository measureRepository;
  private final ComponentIssuesRepository componentIssuesRepository;

  // Output metrics
  private final Metric reliabilityRatingMetric;
  private final Metric securityRatingMetric;

  public ReliabilityAndSecurityRatingMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ComponentIssuesRepository componentIssuesRepository) {
    super(CrawlerDepthLimit.LEAVES, POST_ORDER, ReliabilityAndSecurityRatingMeasuresVisitor.CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.componentIssuesRepository = componentIssuesRepository;

    // Output metrics
    this.reliabilityRatingMetric = metricRepository.getByKey(RELIABILITY_RATING_KEY);
    this.securityRatingMetric = metricRepository.getByKey(SECURITY_RATING_KEY);
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
    Optional<Measure> reliabilityRatingMeasure = measureRepository.getRawMeasure(projectView, reliabilityRatingMetric);
    if (reliabilityRatingMeasure.isPresent()) {
      path.parent().reliabilityRating.increment(RatingGrid.Rating.valueOf(reliabilityRatingMeasure.get().getData()));
    }
    Optional<Measure> securityRatingMeasure = measureRepository.getRawMeasure(projectView, securityRatingMetric);
    if (securityRatingMeasure.isPresent()) {
      path.parent().securityRating.increment(RatingGrid.Rating.valueOf(securityRatingMeasure.get().getData()));
    }
  }

  private void computeAndSaveMeasures(Component component, Path<Counter> path) {
    processIssues(component, path);
    addReliabilityRatingMeasure(component, path);
    addSecurityRatingMeasure(component, path);
    addToParent(path);
  }

  private void processIssues(Component component, Path<Counter> path) {
    for (Issue issue : componentIssuesRepository.getIssues(component)) {
      if (issue.resolution() == null) {
        path.current().addIssue(issue);
      }
    }
  }

  private void addReliabilityRatingMeasure(Component component, Path<Counter> path) {
    RatingGrid.Rating rating = path.current().reliabilityRating.getValue();
    measureRepository.add(component, reliabilityRatingMetric, newMeasureBuilder().create(rating.getIndex(), rating.name()));
  }

  private void addSecurityRatingMeasure(Component component, Path<Counter> path) {
    RatingGrid.Rating rating = path.current().securityRating.getValue();
    measureRepository.add(component, securityRatingMetric, newMeasureBuilder().create(rating.getIndex(), rating.name()));
  }

  private static void addToParent(Path<Counter> path) {
    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  static final class Counter {
    private RatingVariationValue reliabilityRating = new RatingVariationValue();
    private RatingVariationValue securityRating = new RatingVariationValue();

    private Counter() {
      // prevents instantiation
    }

    void add(Counter otherCounter) {
      reliabilityRating.increment(otherCounter.reliabilityRating);
      securityRating.increment(otherCounter.securityRating);
    }

    void addIssue(Issue issue) {
      RatingGrid.Rating rating = getRatingFromSeverity(issue.severity());
      if (issue.type().equals(BUG)) {
        reliabilityRating.increment(rating);
      } else if (issue.type().equals(VULNERABILITY)) {
        securityRating.increment(rating);
      }
    }

    private static RatingGrid.Rating getRatingFromSeverity(String severity) {
      switch (severity) {
        case BLOCKER:
          return RatingGrid.Rating.E;
        case CRITICAL:
          return RatingGrid.Rating.D;
        case MAJOR:
          return RatingGrid.Rating.C;
        case MINOR:
          return RatingGrid.Rating.B;
        default:
          return RatingGrid.Rating.A;
      }
    }
  }

  private static final class CounterFactory extends PathAwareVisitorAdapter.SimpleStackElementFactory<ReliabilityAndSecurityRatingMeasuresVisitor.Counter> {
    public static final ReliabilityAndSecurityRatingMeasuresVisitor.CounterFactory INSTANCE = new ReliabilityAndSecurityRatingMeasuresVisitor.CounterFactory();

    private CounterFactory() {
      // prevents instantiation
    }

    @Override
    public ReliabilityAndSecurityRatingMeasuresVisitor.Counter createForAny(Component component) {
      return new ReliabilityAndSecurityRatingMeasuresVisitor.Counter();
    }

    /**
     * Counter is not used at ProjectView level, saves on instantiating useless objects
     */
    @Override
    public ReliabilityAndSecurityRatingMeasuresVisitor.Counter createForProjectView(Component projectView) {
      return null;
    }
  }
}
