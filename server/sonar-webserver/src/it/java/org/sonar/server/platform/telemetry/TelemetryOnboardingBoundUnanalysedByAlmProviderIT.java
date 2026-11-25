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
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class TelemetryOnboardingBoundUnanalysedByAlmProviderIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final TelemetryOnboardingBoundUnanalysedByAlmProvider underTest = new TelemetryOnboardingBoundUnanalysedByAlmProvider(db.getDbClient());
  private final TelemetryOnboardingBoundProjectsByAlmProvider boundProjectsByAlm = new TelemetryOnboardingBoundProjectsByAlmProvider(db.getDbClient());

  @Test
  void getValues_whenNoProjects_shouldReturnEveryAlmAtZero() {
    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "github", 0,
      "gitlab", 0,
      "azure_devops", 0,
      "bitbucket", 0,
      "bitbucket_cloud", 0));
  }

  @Test
  void getValues_whenOnlyUnboundProjects_shouldReturnEveryAlmAtZero() {
    db.components().insertPrivateProject();
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values.values()).containsOnly(0);
    assertThat(values).hasSize(5);
  }

  @Test
  void getValues_whenMixedBindingsAndAnalyses_shouldCountOnlyBoundProjectsNeverAnalysed() {
    AlmSettingDto githubSetting = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto gitlabSetting = db.almSettings().insertGitlabAlmSetting();
    AlmSettingDto azureSetting = db.almSettings().insertAzureAlmSetting();
    ProjectData githubAnalysed = db.components().insertPrivateProject();
    ProjectData githubNever1 = db.components().insertPrivateProject();
    ProjectData githubNever2 = db.components().insertPrivateProject();
    ProjectData gitlabNever = db.components().insertPrivateProject();
    ProjectData azureAnalysed = db.components().insertPrivateProject();
    ProjectData unboundNever = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubSetting, githubAnalysed.getProjectDto());
    db.almSettings().insertGitHubProjectAlmSetting(githubSetting, githubNever1.getProjectDto());
    db.almSettings().insertGitHubProjectAlmSetting(githubSetting, githubNever2.getProjectDto());
    db.almSettings().insertGitlabProjectAlmSetting(gitlabSetting, gitlabNever.getProjectDto());
    db.almSettings().insertAzureProjectAlmSetting(azureSetting, azureAnalysed.getProjectDto());
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(githubAnalysed.getMainBranchComponent()).setLast(true));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(azureAnalysed.getMainBranchComponent()).setLast(true));
    db.getDbClient().snapshotDao().insert(db.getSession(),
      newAnalysis(unboundNever.getMainBranchComponent()).setLast(true));
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "github", 2,
      "gitlab", 1,
      "azure_devops", 0,
      "bitbucket", 0,
      "bitbucket_cloud", 0));
  }

  @Test
  void getValues_whenBoundProjectOnlyAnalysedOnNonMainBranch_shouldCountItAsAnalysed() {
    AlmSettingDto githubSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectData project = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubSetting, project.getProjectDto());
    var branch = db.components().insertProjectBranch(project.getProjectDto());
    db.components().insertSnapshot(branch, s -> s.setLast(true));
    db.getSession().commit();

    assertThat(underTest.getValues()).containsEntry("github", 0);
  }

  @Test
  void getValues_shouldReconcileWithBoundProjectsAndAnalysedProjectsOfBoundProjectsByAlm() {
    AlmSettingDto githubSetting = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto bitbucketCloudSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    for (int i = 0; i < 4; i++) {
      ProjectData project = db.components().insertPrivateProject();
      db.almSettings().insertGitHubProjectAlmSetting(githubSetting, project.getProjectDto());
      if (i < 3) {
        db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(project.getMainBranchComponent()).setLast(true));
      }
    }
    ProjectData bitbucketCloudProject = db.components().insertPrivateProject();
    db.almSettings().insertBitbucketCloudProjectAlmSetting(bitbucketCloudSetting, bitbucketCloudProject.getProjectDto());
    db.getSession().commit();

    Map<String, Integer> bound = boundProjectsByAlm.getValues();
    Map<String, Integer> unanalysed = underTest.getValues();

    assertThat(bound.get("github") - unanalysed.get("github")).isEqualTo(3);
    assertThat(bound.get("bitbucket_cloud") - unanalysed.get("bitbucket_cloud")).isZero();
  }
}
