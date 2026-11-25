/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryOnboardingProjectsByAnalysisModeProviderIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final TelemetryOnboardingProjectsByAnalysisModeProvider underTest = new TelemetryOnboardingProjectsByAnalysisModeProvider(db.getDbClient());
  private final TelemetryOnboardingCountsProvider countsProvider = new TelemetryOnboardingCountsProvider(db.getDbClient());

  @Test
  void getValues_whenNoProjects_shouldReturnBothKeysAtZero() {
    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 0, "ci_undetected", 0));
  }

  @Test
  void getValues_whenMixedProjects_shouldSplitAnalysedProjectsBetweenCiDetectedAndUndetected() {
    insertAnalysedProject("Github Actions");
    insertAnalysedProject("Jenkins");
    insertAnalysedProject("undetected");
    insertAnalysedProject(null);
    insertAnalysedProject("");
    db.components().insertPrivateProject();
    db.components().insertPrivateProject();
    db.components().insertPrivateProject();
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 2, "ci_undetected", 3));
  }

  @Test
  void getValues_shouldTreatUndetectedSentinelCaseInsensitively() {
    insertAnalysedProject("UNDETECTED");
    db.getSession().commit();

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 0, "ci_undetected", 1));
  }

  @Test
  void getValues_shouldOnlyLookAtLastAnalysisOfMainBranch() {
    ProjectData ciThenUndetected = db.components().insertPrivateProject();
    insertAnalysis(ciThenUndetected, false, "Github Actions");
    insertAnalysis(ciThenUndetected, true, "undetected");
    ProjectData ciOnlyOnBranch = db.components().insertPrivateProject();
    insertAnalysis(ciOnlyOnBranch, true, "undetected");
    SnapshotDto branchAnalysis = db.components().insertSnapshot(
      db.components().insertProjectBranch(ciOnlyOnBranch.getProjectDto()), s -> s.setLast(true));
    insertDetectedCi(branchAnalysis, "Github Actions");
    ProjectData undetectedThenCi = db.components().insertPrivateProject();
    insertAnalysis(undetectedThenCi, false, "undetected");
    insertAnalysis(undetectedThenCi, true, "Jenkins");
    db.getSession().commit();

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 1, "ci_undetected", 2));
  }

  @Test
  void getValues_whenProjectOnlyAnalysedOnNonMainBranch_shouldCountItAsCiUndetected() {
    ProjectData project = db.components().insertPrivateProject();
    SnapshotDto branchAnalysis = db.components().insertSnapshot(
      db.components().insertProjectBranch(project.getProjectDto()), s -> s.setLast(true));
    insertDetectedCi(branchAnalysis, "Github Actions");
    db.getSession().commit();

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 0, "ci_undetected", 1));
  }

  @Test
  void getValues_shouldIgnoreOtherAnalysisProperties() {
    ProjectData project = db.components().insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setLast(true));
    insertProperty(analysis, "sonar.analysis.detectedscm", "git");
    db.getSession().commit();

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 0, "ci_undetected", 1));
  }

  @Test
  void getValues_shouldAddUpToAnalysedProjectsOfOnboardingCounts() {
    insertAnalysedProject("Github Actions");
    insertAnalysedProject("undetected");
    insertAnalysedProject(null);
    db.components().insertPrivateProject();
    db.components().insertPrivateProject();
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();
    Map<String, Integer> counts = countsProvider.getValues();

    assertThat(values.get("ci_detected") + values.get("ci_undetected")).isEqualTo(counts.get("analysed_projects")).isEqualTo(3);
  }

  private void insertAnalysedProject(String detectedCi) {
    insertAnalysis(db.components().insertPrivateProject(), true, detectedCi);
  }

  private void insertAnalysis(ProjectData project, boolean last, String detectedCi) {
    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setLast(last));
    if (detectedCi != null) {
      insertDetectedCi(analysis, detectedCi);
    }
  }

  private void insertDetectedCi(SnapshotDto analysis, String detectedCi) {
    insertProperty(analysis, "sonar.analysis.detectedci", detectedCi);
  }

  private void insertProperty(SnapshotDto analysis, String key, String value) {
    db.getDbClient().analysisPropertiesDao().insert(db.getSession(), new AnalysisPropertyDto()
      .setUuid(Uuids.createFast())
      .setAnalysisUuid(analysis.getUuid())
      .setKey(key)
      .setValue(value));
    db.commit();
  }
}
