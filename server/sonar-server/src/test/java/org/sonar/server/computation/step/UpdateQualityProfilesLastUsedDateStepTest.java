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

package org.sonar.server.computation.step;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric.MetricType;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.qualityprofile.QPMeasureData;
import org.sonar.server.computation.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;

public class UpdateQualityProfilesLastUsedDateStepTest {
  static final long ANALYSIS_DATE = 1_123_456_789L;
  private static final Component PROJECT = ReportComponent.DUMB_PROJECT;
  private QualityProfileDto sonarWayJava = newQualityProfileDto().setKey("sonar-way-java");
  private QualityProfileDto sonarWayPhp = newQualityProfileDto().setKey("sonar-way-php");
  private QualityProfileDto myQualityProfile = newQualityProfileDto().setKey("my-qp");

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  QualityProfileDbTester qualityProfileDb = new QualityProfileDbTester(db);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  UpdateQualityProfilesLastUsedDateStep underTest;

  @Before
  public void setUp() {
    underTest = new UpdateQualityProfilesLastUsedDateStep(dbClient, analysisMetadataHolder, treeRootHolder, metricRepository, measureRepository);
    analysisMetadataHolder.setAnalysisDate(ANALYSIS_DATE);
    treeRootHolder.setRoot(PROJECT);
    Metric<String> metric = CoreMetrics.QUALITY_PROFILES;
    metricRepository.add(new MetricImpl(1, metric.getKey(), metric.getName(), MetricType.STRING));

    qualityProfileDb.insertQualityProfiles(sonarWayJava, sonarWayPhp, myQualityProfile);
  }

  @Test
  public void project_without_quality_profiles() {
    underTest.execute();

    assertQualityProfileIsTheSame(sonarWayJava);
    assertQualityProfileIsTheSame(sonarWayPhp);
    assertQualityProfileIsTheSame(myQualityProfile);
  }

  @Test
  public void analysis_quality_profiles_are_updated() {
    measureRepository.addRawMeasure(1, QUALITY_PROFILES_KEY, Measure.newMeasureBuilder().create(
      toJson(sonarWayJava.getKey(), myQualityProfile.getKey())));

    underTest.execute();

    assertQualityProfileIsTheSame(sonarWayPhp);
    assertQualityProfileIsUpdated(sonarWayJava);
    assertQualityProfileIsUpdated(myQualityProfile);
  }

  @Test
  public void description() {
    assertThat(underTest.getDescription()).isEqualTo("Update quality profiles");
  }

  private void assertQualityProfileIsUpdated(QualityProfileDto qp) {
    assertThat(selectLastUser(qp.getKey())).withFailMessage("Quality profile '%s' hasn't been updated. Value: %d", qp.getKey(), qp.getLastUsed()).isEqualTo(ANALYSIS_DATE);
  }

  private void assertQualityProfileIsTheSame(QualityProfileDto qp) {
    assertThat(selectLastUser(qp.getKey())).isEqualTo(qp.getLastUsed());
  }

  @CheckForNull
  private Long selectLastUser(String qualityProfileKey) {
    return dbClient.qualityProfileDao().selectByKey(dbSession, qualityProfileKey).getLastUsed();
  }

  private static String toJson(String... keys) {
    return QPMeasureData.toJson(new QPMeasureData(
      Arrays.stream(keys)
        .map(key -> new QualityProfile(key, key, key, new Date()))
        .collect(Collectors.toList())));
  }
}
