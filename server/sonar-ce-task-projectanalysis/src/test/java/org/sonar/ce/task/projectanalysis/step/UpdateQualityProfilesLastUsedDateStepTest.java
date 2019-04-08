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

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;

public class UpdateQualityProfilesLastUsedDateStepTest {
  static final long ANALYSIS_DATE = 1_123_456_789L;
  private static final Component PROJECT = ReportComponent.DUMB_PROJECT;
  private QProfileDto sonarWayJava = newProfile("sonar-way-java");
  private QProfileDto sonarWayPhp = newProfile("sonar-way-php");
  private QProfileDto myQualityProfile = newProfile("my-qp");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule().setAnalysisDate(ANALYSIS_DATE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(QUALITY_PROFILES);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private QualityProfileDbTester qualityProfileDb = new QualityProfileDbTester(db);

  UpdateQualityProfilesLastUsedDateStep underTest = new UpdateQualityProfilesLastUsedDateStep(dbClient, analysisMetadataHolder, treeRootHolder, metricRepository,
    measureRepository);

  @Test
  public void doest_not_update_profiles_when_no_measure() {
    qualityProfileDb.insert(sonarWayJava, sonarWayPhp, myQualityProfile);

    underTest.execute(new TestComputationStepContext());

    assertQualityProfileIsTheSame(sonarWayJava);
    assertQualityProfileIsTheSame(sonarWayPhp);
    assertQualityProfileIsTheSame(myQualityProfile);
  }

  @Test
  public void update_profiles_defined_in_quality_profiles_measure() {
    qualityProfileDb.insert(sonarWayJava, sonarWayPhp, myQualityProfile);

    measureRepository.addRawMeasure(1, QUALITY_PROFILES_KEY, Measure.newMeasureBuilder().create(
      toJson(sonarWayJava.getKee(), myQualityProfile.getKee())));

    underTest.execute(new TestComputationStepContext());

    assertQualityProfileIsTheSame(sonarWayPhp);
    assertQualityProfileIsUpdated(sonarWayJava);
    assertQualityProfileIsUpdated(myQualityProfile);
  }

  @Test
  public void ancestor_profiles_are_updated() {
    // Parent profiles should be updated
    QProfileDto rootProfile = newProfile("root");
    QProfileDto parentProfile = newProfile("parent").setParentKee(rootProfile.getKee());
    // Current profile => should be updated
    QProfileDto currentProfile = newProfile("current").setParentKee(parentProfile.getKee());
    // Child of current profile => should not be updated
    QProfileDto childProfile = newProfile("child").setParentKee(currentProfile.getKee());
    qualityProfileDb.insert(rootProfile, parentProfile, currentProfile, childProfile);

    measureRepository.addRawMeasure(1, QUALITY_PROFILES_KEY, Measure.newMeasureBuilder().create(toJson(currentProfile.getKee())));

    underTest.execute(new TestComputationStepContext());

    assertQualityProfileIsUpdated(rootProfile);
    assertQualityProfileIsUpdated(parentProfile);
    assertQualityProfileIsUpdated(currentProfile);
    assertQualityProfileIsTheSame(childProfile);
  }

  @Test
  public void fail_when_profile_is_linked_to_unknown_parent() {
    QProfileDto currentProfile = newProfile("current").setParentKee("unknown");
    qualityProfileDb.insert(currentProfile);

    measureRepository.addRawMeasure(1, QUALITY_PROFILES_KEY, Measure.newMeasureBuilder().create(toJson(currentProfile.getKee())));

    expectedException.expect(RowNotFoundException.class);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void test_description() {
    assertThat(underTest.getDescription()).isEqualTo("Update last usage date of quality profiles");
  }

  private static QProfileDto newProfile(String key) {
    return newQualityProfileDto().setKee(key)
      // profile has been used before the analysis
      .setLastUsed(ANALYSIS_DATE - 10_000);
  }

  private void assertQualityProfileIsUpdated(QProfileDto qp) {
    assertThat(selectLastUser(qp.getKee())).withFailMessage("Quality profile '%s' hasn't been updated. Value: %d", qp.getKee(), qp.getLastUsed()).isEqualTo(ANALYSIS_DATE);
  }

  private void assertQualityProfileIsTheSame(QProfileDto qp) {
    assertThat(selectLastUser(qp.getKee())).isEqualTo(qp.getLastUsed());
  }

  @CheckForNull
  private Long selectLastUser(String qualityProfileKey) {
    return dbClient.qualityProfileDao().selectByUuid(dbSession, qualityProfileKey).getLastUsed();
  }

  private static String toJson(String... keys) {
    return QPMeasureData.toJson(new QPMeasureData(
      Arrays.stream(keys)
        .map(key -> new QualityProfile(key, key, key, new Date()))
        .collect(Collectors.toList())));
  }
}
