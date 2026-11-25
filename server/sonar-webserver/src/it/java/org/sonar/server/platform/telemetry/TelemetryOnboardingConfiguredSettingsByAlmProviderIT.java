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

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryOnboardingConfiguredSettingsByAlmProviderIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final TelemetryOnboardingConfiguredSettingsByAlmProvider underTest = new TelemetryOnboardingConfiguredSettingsByAlmProvider(db.getDbClient());
  private final TelemetryOnboardingCountsProvider countsProvider = new TelemetryOnboardingCountsProvider(db.getDbClient());

  @Test
  void getValues_whenNoAlmSettings_shouldReturnEveryAlmAtZero() {
    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "github", 0,
      "gitlab", 0,
      "azure_devops", 0,
      "bitbucket", 0,
      "bitbucket_cloud", 0));
  }

  @Test
  void getValues_whenSeveralSettingsPerPlatform_shouldCountEachConfiguration() {
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitlabAlmSetting();
    db.almSettings().insertAzureAlmSetting();
    db.almSettings().insertBitbucketAlmSetting();
    db.almSettings().insertBitbucketCloudAlmSetting();
    db.almSettings().insertBitbucketCloudAlmSetting();
    db.getSession().commit();

    Map<String, Integer> values = underTest.getValues();

    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "github", 2,
      "gitlab", 1,
      "azure_devops", 1,
      "bitbucket", 1,
      "bitbucket_cloud", 2));
  }

  @Test
  void getValues_shouldAddUpToConfiguredAlmOfOnboardingCounts() {
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitlabAlmSetting();
    db.almSettings().insertAzureAlmSetting();
    db.getSession().commit();

    int sum = underTest.getValues().values().stream().mapToInt(Integer::intValue).sum();

    assertThat(sum).isEqualTo(countsProvider.getValues().get("configured_alm")).isEqualTo(4);
  }
}
