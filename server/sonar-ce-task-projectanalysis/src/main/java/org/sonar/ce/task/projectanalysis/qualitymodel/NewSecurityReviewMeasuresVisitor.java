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
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.issue.NewIssueClassifier;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.FILE;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;

public class NewSecurityReviewMeasuresVisitor extends PathAwareVisitorAdapter<SecurityReviewCounter> {

  private final ComponentIssuesRepository componentIssuesRepository;
  private final MeasureRepository measureRepository;
  private final Metric newSecurityReviewRatingMetric;
  private final Metric newSecurityHotspotsReviewedMetric;
  private final Metric newSecurityHotspotsReviewedStatusMetric;
  private final Metric newSecurityHotspotsToReviewStatusMetric;
  private final NewIssueClassifier newIssueClassifier;

  public NewSecurityReviewMeasuresVisitor(ComponentIssuesRepository componentIssuesRepository, MeasureRepository measureRepository, MetricRepository metricRepository,
    NewIssueClassifier newIssueClassifier) {
    super(FILE, POST_ORDER, NewSecurityReviewMeasuresVisitor.CounterFactory.INSTANCE);
    this.componentIssuesRepository = componentIssuesRepository;
    this.measureRepository = measureRepository;
    this.newSecurityReviewRatingMetric = metricRepository.getByKey(NEW_SECURITY_REVIEW_RATING_KEY);
    this.newSecurityHotspotsReviewedMetric = metricRepository.getByKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY);
    this.newSecurityHotspotsReviewedStatusMetric = metricRepository.getByKey(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY);
    this.newSecurityHotspotsToReviewStatusMetric = metricRepository.getByKey(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY);
    this.newIssueClassifier = newIssueClassifier;
  }

  @Override
  public void visitProject(Component project, Path<SecurityReviewCounter> path) {
    if (!newIssueClassifier.isEnabled()) {
      return;
    }
    computeMeasure(project, path);

    // The following measures are only computed on projects level as they are required to compute the others measures on applications
    measureRepository.add(project, newSecurityHotspotsReviewedStatusMetric, Measure.newMeasureBuilder().create(path.current().getHotspotsReviewed()));
    measureRepository.add(project, newSecurityHotspotsToReviewStatusMetric, Measure.newMeasureBuilder().create(path.current().getHotspotsToReview()));
  }

  @Override
  public void visitDirectory(Component directory, Path<SecurityReviewCounter> path) {
    computeMeasure(directory, path);
  }

  @Override
  public void visitFile(Component file, Path<SecurityReviewCounter> path) {
    computeMeasure(file, path);
  }

  private void computeMeasure(Component component, Path<SecurityReviewCounter> path) {
    componentIssuesRepository.getNotSandboxedIssues(component)
      .stream()
      .filter(issue -> issue.type().equals(SECURITY_HOTSPOT))
      .filter(issue -> newIssueClassifier.isNew(component, issue))
      .forEach(issue -> path.current().processHotspot(issue));

    Optional<Double> percent = computePercent(path.current().getHotspotsToReview(), path.current().getHotspotsReviewed());
    measureRepository.add(component, newSecurityReviewRatingMetric, Measure.newMeasureBuilder().create(computeRating(percent.orElse(null)).getIndex()));
    percent.ifPresent(p -> measureRepository.add(component, newSecurityHotspotsReviewedMetric, Measure.newMeasureBuilder().create(p)));

    if (!path.isRoot()) {
      path.parent().add(path.current());
    }
  }

  private static final class CounterFactory extends SimpleStackElementFactory<SecurityReviewCounter> {
    public static final NewSecurityReviewMeasuresVisitor.CounterFactory INSTANCE = new NewSecurityReviewMeasuresVisitor.CounterFactory();

    private CounterFactory() {
      // prevents instantiation
    }

    @Override
    public SecurityReviewCounter createForAny(Component component) {
      return new SecurityReviewCounter();
    }
  }

}
