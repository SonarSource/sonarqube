/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.RatingMeasures;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.measure.Rating;
import org.sonar.server.security.SecurityReviewRating;

import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class SecurityReviewRatingVisitor extends TypeAwareVisitorAdapter {

  private final MeasureRepository measureRepository;
  private final Metric nclocMetric;
  private final Metric securityHostspotsMetric;
  private final Metric securityReviewRatingMetric;

  public SecurityReviewRatingVisitor(MeasureRepository measureRepository, MetricRepository metricRepository) {
    super(new CrawlerDepthLimit.Builder(PROJECT).withViewsMaxDepth(SUBVIEW), Order.POST_ORDER);
    this.measureRepository = measureRepository;
    this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
    this.securityHostspotsMetric = metricRepository.getByKey(SECURITY_HOTSPOTS_KEY);
    this.securityReviewRatingMetric = metricRepository.getByKey(SECURITY_REVIEW_RATING_KEY);
  }

  @Override
  public void visitProject(Component project) {
    computeMeasure(project);
  }

  @Override
  public void visitView(Component view) {
    computeMeasure(view);
  }

  @Override
  public void visitSubView(Component subView) {
    computeMeasure(subView);
  }

  private void computeMeasure(Component component) {
    Optional<Measure> nclocMeasure = measureRepository.getRawMeasure(component, nclocMetric);
    Optional<Measure> securityHostspotsMeasure = measureRepository.getRawMeasure(component, securityHostspotsMetric);
    if (!nclocMeasure.isPresent() || !securityHostspotsMeasure.isPresent()) {
      return;
    }
    int ncloc = nclocMeasure.get().getIntValue();
    int securityHotspots = securityHostspotsMeasure.get().getIntValue();
    Rating rating = SecurityReviewRating.compute(ncloc, securityHotspots);
    measureRepository.add(component, securityReviewRatingMetric, RatingMeasures.get(rating));
  }

}
