/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.qualityprofile.QPMeasureData;
import org.sonar.server.computation.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class ComputeQProfileMeasureStepTest {

  private static final String QP_NAME_1 = "qp1";
  private static final String QP_NAME_2 = "qp1";
  private static final String LANGUAGE_KEY_1 = "language_key1";
  private static final String LANGUAGE_KEY_2 = "language_key2";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(QUALITY_PROFILES);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputeQProfileMeasureStep underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ComputeQProfileMeasureStep(treeRootHolder, measureRepository, metricRepository);
  }

  @Test
  public void add_quality_profile_measure_on_project() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(MODULE, 111).build()
          )
          .build()
      ).build();

    treeRootHolder.setRoot(project);

    QualityProfile qp = createQProfile(QP_NAME_1, LANGUAGE_KEY_1);
    addMeasure(111, qp);

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1).get(QUALITY_PROFILES_KEY)).extracting("data").containsOnly(toJson(qp));
  }

  @Test
  public void add_quality_profile_measure_from_multiple_modules() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(MODULE, 111).build()
          )
          .build(),
        DumbComponent.builder(MODULE, 12).build()
      ).build();

    treeRootHolder.setRoot(project);

    QualityProfile qp1 = createQProfile(QP_NAME_1, LANGUAGE_KEY_1);
    addMeasure(111, qp1);
    QualityProfile qp2 = createQProfile(QP_NAME_2, LANGUAGE_KEY_2);
    addMeasure(12, qp2);

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1).get(QUALITY_PROFILES_KEY)).extracting("data").containsOnly(toJson(qp1, qp2));
  }

  @Test
  public void nothing_to_add_when_no_measure() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1)
      .addChildren(
        DumbComponent.builder(MODULE, 11)
          .addChildren(
            DumbComponent.builder(MODULE, 111).build()
          )
          .build()
      ).build();

    treeRootHolder.setRoot(project);

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1)).isEmpty();
  }

  @Test
  public void nothing_to_add_when_measure_already_exists_on_project() throws Exception {
    DumbComponent project = DumbComponent.builder(PROJECT, 1).build();

    treeRootHolder.setRoot(project);

    QualityProfile qp = createQProfile(QP_NAME_1, LANGUAGE_KEY_1);
    addMeasure(1, qp);

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(1)).isEmpty();
  }

  private static QualityProfile createQProfile(String qpName, String languageKey) {
    return new QualityProfile(qpName + "-" + languageKey, qpName, languageKey, new Date());
  }

  private void addMeasure(int componentRef, QualityProfile... qps) {
    Measure qualityProfileMeasure = newMeasureBuilder().create(toJson(qps));
    measureRepository.addRawMeasure(componentRef, QUALITY_PROFILES_KEY, qualityProfileMeasure);
  }

  private static String toJson(QualityProfile... qps) {
    List<QualityProfile> qualityProfiles = Arrays.asList(qps);
    return QPMeasureData.toJson(new QPMeasureData(qualityProfiles));
  }
}
