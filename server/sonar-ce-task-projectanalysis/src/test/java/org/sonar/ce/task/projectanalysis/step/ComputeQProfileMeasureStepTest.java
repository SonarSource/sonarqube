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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;

public class ComputeQProfileMeasureStepTest {

  private static final String QP_NAME_1 = "qp1";
  private static final String QP_NAME_2 = "qp1";
  private static final String LANGUAGE_KEY_1 = "java";
  private static final String LANGUAGE_KEY_2 = "php";

  private static final String PROJECT_KEY = "PROJECT KEY";
  private static final int PROJECT_REF = 1;
  private static final int FOLDER_1_REF = 1111;
  private static final int FOLDER_2_REF = 1112;
  private static final int FILE_1_1_REF = 11111;
  private static final int FILE_1_2_REF = 11112;
  private static final int FILE_2_1_REF = 11121;
  private static final int FILE_2_2_REF = 11122;

  private static final Component MULTI_MODULE_PROJECT = ReportComponent.builder(PROJECT, PROJECT_REF).setKey(PROJECT_KEY)
    .addChildren(ReportComponent.builder(DIRECTORY, FOLDER_1_REF)
        .addChildren(
          ReportComponent.builder(FILE, FILE_1_1_REF).setFileAttributes(new FileAttributes(false, "java", 1)).build(),
          ReportComponent.builder(FILE, FILE_1_2_REF).setFileAttributes(new FileAttributes(false, "java", 1)).build())
        .build(),
      ReportComponent.builder(DIRECTORY, FOLDER_2_REF)
        .addChildren(
          ReportComponent.builder(FILE, FILE_2_1_REF).setFileAttributes(new FileAttributes(false, null, 1)).build(),
          ReportComponent.builder(FILE, FILE_2_2_REF).setFileAttributes(new FileAttributes(false, "php", 1)).build())
        .build())
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(QUALITY_PROFILES);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private ComputeQProfileMeasureStep underTest = new ComputeQProfileMeasureStep(treeRootHolder, measureRepository, metricRepository, analysisMetadataHolder);

  @Test
  public void add_quality_profile_measure_on_project() {
    treeRootHolder.setRoot(MULTI_MODULE_PROJECT);
    QualityProfile qpJava = createQProfile(QP_NAME_1, LANGUAGE_KEY_1);
    QualityProfile qpPhp = createQProfile(QP_NAME_2, LANGUAGE_KEY_2);
    analysisMetadataHolder.setQProfilesByLanguage(ImmutableMap.of(LANGUAGE_KEY_1, qpJava, LANGUAGE_KEY_2, qpPhp));

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasures(PROJECT_REF).get(QUALITY_PROFILES_KEY))
      .extracting("data").containsOnly(toJson(qpJava, qpPhp));
  }

  @Test
  public void nothing_to_add_when_no_files() {
    ReportComponent project = ReportComponent.builder(PROJECT, PROJECT_REF).build();
    treeRootHolder.setRoot(project);

    underTest.execute(new TestComputationStepContext());

    assertThat(measureRepository.getAddedRawMeasures(PROJECT_REF)).isEmpty();
  }

  @Test
  public void fail_if_report_inconsistent() {
    treeRootHolder.setRoot(MULTI_MODULE_PROJECT);
    QualityProfile qpJava = createQProfile(QP_NAME_1, LANGUAGE_KEY_1);
    analysisMetadataHolder.setQProfilesByLanguage(ImmutableMap.of(LANGUAGE_KEY_1, qpJava));

    try {
      underTest.execute(new TestComputationStepContext());
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).hasCause(new IllegalStateException("Report contains a file with language 'php' but no matching quality profile"));
    }

  }

  private static QualityProfile createQProfile(String qpName, String languageKey) {
    return new QualityProfile(qpName + "-" + languageKey, qpName, languageKey, new Date());
  }

  private static String toJson(QualityProfile... qps) {
    List<QualityProfile> qualityProfiles = Arrays.asList(qps);
    return QPMeasureData.toJson(new QPMeasureData(qualityProfiles));
  }

}
