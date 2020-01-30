/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import org.sonar.api.ce.measure.Issue;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitor;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.RatingMeasures;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.FILE;
import static org.sonar.core.issue.DefaultIssue.STATUS_TO_REVIEW;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;

public class SecurityReviewMeasuresVisitor extends PathAwareVisitorAdapter<SecurityReviewMeasuresVisitor.Counter> {

  private final ComponentIssuesRepository componentIssuesRepository;
  private final MeasureRepository measureRepository;
  private final Metric securityReviewRatingMetric;
  private final Metric securityHotspotsReviewedMetric;

  public SecurityReviewMeasuresVisitor(ComponentIssuesRepository componentIssuesRepository, MeasureRepository measureRepository, MetricRepository metricRepository) {
    super(FILE, POST_ORDER, SecurityReviewMeasuresVisitor.CounterFactory.INSTANCE);
    this.componentIssuesRepository = componentIssuesRepository;
    this.measureRepository = measureRepository;
    this.securityReviewRatingMetric = metricRepository.getByKey(SECURITY_REVIEW_RATING_KEY);
    this.securityHotspotsReviewedMetric = metricRepository.getByKey(SECURITY_HOTSPOTS_REVIEWED_KEY);
  }

  @Override
  public void visitProject(Component project, Path<SecurityReviewMeasuresVisitor.Counter> path) {
    computeMeasure(project, path);
  }

  @Override
  public void visitDirectory(Component directory, PathAwareVisitor.Path<SecurityReviewMeasuresVisitor.Counter> path) {
    computeMeasure(directory, path);
  }

  @Override
  public void visitFile(Component file, PathAwareVisitor.Path<SecurityReviewMeasuresVisitor.Counter> path) {
    computeMeasure(file, path);
  }

  private void computeMeasure(Component component, PathAwareVisitor.Path<SecurityReviewMeasuresVisitor.Counter> path) {
    componentIssuesRepository.getIssues(component)
      .stream()
      .filter(issue -> issue.type().equals(SECURITY_HOTSPOT))
      .forEach(issue -> path.current().processHotspot(issue));

    Double percent = computePercent(path.current().hotspotsToReview, path.current().hotspotsReviewed);
    measureRepository.add(component, securityHotspotsReviewedMetric, Measure.newMeasureBuilder().create(percent, securityHotspotsReviewedMetric.getDecimalScale()));
    measureRepository.add(component, securityReviewRatingMetric, RatingMeasures.get(computeRating(percent)));

    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  static final class Counter {
    private long hotspotsReviewed;
    private long hotspotsToReview;

    private Counter() {
      // prevents instantiation
    }

    void processHotspot(Issue issue) {
      if (issue.status().equals(STATUS_REVIEWED)) {
        hotspotsReviewed++;
      } else if (issue.status().equals(STATUS_TO_REVIEW)) {
        hotspotsToReview++;
      }
    }

    void add(Counter otherCounter) {
      hotspotsReviewed += otherCounter.hotspotsReviewed;
      hotspotsToReview += otherCounter.hotspotsToReview;
    }
  }

  private static final class CounterFactory extends PathAwareVisitorAdapter.SimpleStackElementFactory<SecurityReviewMeasuresVisitor.Counter> {
    public static final SecurityReviewMeasuresVisitor.CounterFactory INSTANCE = new SecurityReviewMeasuresVisitor.CounterFactory();

    private CounterFactory() {
      // prevents instantiation
    }

    @Override
    public SecurityReviewMeasuresVisitor.Counter createForAny(Component component) {
      return new SecurityReviewMeasuresVisitor.Counter();
    }
  }

}
