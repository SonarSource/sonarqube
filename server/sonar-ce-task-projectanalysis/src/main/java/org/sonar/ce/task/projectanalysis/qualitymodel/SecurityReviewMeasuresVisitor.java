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

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitor;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.RatingMeasures;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.FILE;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;

public class SecurityReviewMeasuresVisitor extends PathAwareVisitorAdapter<SecurityReviewCounter> {

  private final ComponentIssuesRepository componentIssuesRepository;
  private final MeasureRepository measureRepository;
  private final Metric securityReviewRatingMetric;
  private final Metric securityHotspotsReviewedMetric;
  private final Metric securityHotspotsReviewedStatusMetric;
  private final Metric securityHotspotsToReviewStatusMetric;

  public SecurityReviewMeasuresVisitor(ComponentIssuesRepository componentIssuesRepository, MeasureRepository measureRepository, MetricRepository metricRepository) {
    super(FILE, POST_ORDER, SecurityReviewMeasuresVisitor.CounterFactory.INSTANCE);
    this.componentIssuesRepository = componentIssuesRepository;
    this.measureRepository = measureRepository;
    this.securityReviewRatingMetric = metricRepository.getByKey(SECURITY_REVIEW_RATING_KEY);
    this.securityHotspotsReviewedMetric = metricRepository.getByKey(SECURITY_HOTSPOTS_REVIEWED_KEY);
    this.securityHotspotsReviewedStatusMetric = metricRepository.getByKey(SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY);
    this.securityHotspotsToReviewStatusMetric = metricRepository.getByKey(SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY);
  }

  @Override
  public void visitProject(Component project, Path<SecurityReviewCounter> path) {
    computeMeasure(project, path);
  }

  @Override
  public void visitDirectory(Component directory, PathAwareVisitor.Path<SecurityReviewCounter> path) {
    computeMeasure(directory, path);
  }

  @Override
  public void visitFile(Component file, PathAwareVisitor.Path<SecurityReviewCounter> path) {
    computeMeasure(file, path);
  }

  private void computeMeasure(Component component, PathAwareVisitor.Path<SecurityReviewCounter> path) {
    componentIssuesRepository.getIssues(component)
      .stream()
      .filter(issue -> issue.type().equals(SECURITY_HOTSPOT))
      .forEach(issue -> path.current().processHotspot(issue));

    measureRepository.add(component, securityHotspotsReviewedStatusMetric, newMeasureBuilder().create(path.current().getHotspotsReviewed()));
    measureRepository.add(component, securityHotspotsToReviewStatusMetric, newMeasureBuilder().create(path.current().getHotspotsToReview()));
    Optional<Double> percent = computePercent(path.current().getHotspotsToReview(), path.current().getHotspotsReviewed());
    measureRepository.add(component, securityReviewRatingMetric, RatingMeasures.get(computeRating(percent.orElse(null))));
    percent.ifPresent(p -> measureRepository.add(component, securityHotspotsReviewedMetric, newMeasureBuilder().create(p, securityHotspotsReviewedMetric.getDecimalScale())));

    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  private static final class CounterFactory extends PathAwareVisitorAdapter.SimpleStackElementFactory<SecurityReviewCounter> {
    public static final SecurityReviewMeasuresVisitor.CounterFactory INSTANCE = new SecurityReviewMeasuresVisitor.CounterFactory();

    private CounterFactory() {
      // prevents instantiation
    }

    @Override
    public SecurityReviewCounter createForAny(Component component) {
      return new SecurityReviewCounter();
    }
  }

}
