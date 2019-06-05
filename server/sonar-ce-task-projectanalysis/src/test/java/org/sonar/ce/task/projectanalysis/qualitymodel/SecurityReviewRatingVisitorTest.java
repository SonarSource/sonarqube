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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.measure.Rating;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class SecurityReviewRatingVisitorTest {

  private static final int PROJECT_REF = 1;
  private static final Component PROJECT = builder(Component.Type.PROJECT, PROJECT_REF).setKey("project").build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(SECURITY_HOTSPOTS)
    .add(SECURITY_REVIEW_RATING);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private VisitorsCrawler underTest = new VisitorsCrawler(singletonList(new SecurityReviewRatingVisitor(measureRepository, metricRepository)));

  @Test
  public void compute_security_review_rating_on_project() {
    treeRootHolder.setRoot(PROJECT);
    measureRepository.addRawMeasure(PROJECT_REF, NCLOC_KEY, newMeasureBuilder().create(1000));
    measureRepository.addRawMeasure(PROJECT_REF, SECURITY_HOTSPOTS_KEY, newMeasureBuilder().create(12));

    underTest.visit(PROJECT);

    Measure measure = measureRepository.getAddedRawMeasure(PROJECT_REF, SECURITY_REVIEW_RATING_KEY).get();
    assertThat(measure.getIntValue()).isEqualTo(Rating.C.getIndex());
    assertThat(measure.getData()).isEqualTo(Rating.C.name());
  }

  @Test
  public void compute_nothing_when_no_ncloc() {
    treeRootHolder.setRoot(PROJECT);
    measureRepository.addRawMeasure(PROJECT_REF, SECURITY_HOTSPOTS_KEY, newMeasureBuilder().create(2));

    underTest.visit(PROJECT);

    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, SECURITY_REVIEW_RATING_KEY)).isEmpty();
  }

  @Test
  public void compute_nothing_when_no_security_hotspot() {
    treeRootHolder.setRoot(PROJECT);
    measureRepository.addRawMeasure(PROJECT_REF, NCLOC_KEY, newMeasureBuilder().create(1000));

    underTest.visit(PROJECT);

    assertThat(measureRepository.getAddedRawMeasure(PROJECT_REF, SECURITY_REVIEW_RATING_KEY)).isEmpty();
  }
}
