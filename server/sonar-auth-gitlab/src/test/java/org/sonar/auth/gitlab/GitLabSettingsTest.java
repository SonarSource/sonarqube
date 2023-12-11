/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.auth.gitlab;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_APPLICATION_ID;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_TOKEN;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SECRET;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SYNC_USER_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;

public class GitLabSettingsTest {

  private MapSettings settings;
  private GitLabSettings config;

  @Before
  public void prepare() {
    settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, GitLabSettings.definitions()));
    config = new GitLabSettings(settings.asConfig());
  }

  @Test
  public void test_settings() {
    assertThat(config.url()).isEqualTo("https://gitlab.com");
    assertThat(config.apiUrl()).isEqualTo("https://gitlab.com/api/v4");

    settings.setProperty(GITLAB_AUTH_URL, "https://onpremise.gitlab.com/");
    assertThat(config.url()).isEqualTo("https://onpremise.gitlab.com");
    assertThat(config.apiUrl()).isEqualTo("https://onpremise.gitlab.com/api/v4");

    settings.setProperty(GITLAB_AUTH_URL, "https://onpremise.gitlab.com");
    assertThat(config.url()).isEqualTo("https://onpremise.gitlab.com");
    assertThat(config.apiUrl()).isEqualTo("https://onpremise.gitlab.com/api/v4");

    assertThat(config.isEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_ENABLED, "true");
    assertThat(config.isEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_APPLICATION_ID, "1234");
    assertThat(config.isEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_SECRET, "5678");
    assertThat(config.isEnabled()).isTrue();

    assertThat(config.applicationId()).isEqualTo("1234");
    assertThat(config.secret()).isEqualTo("5678");

    assertThat(config.allowUsersToSignUp()).isTrue();
    settings.setProperty(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "false");
    assertThat(config.allowUsersToSignUp()).isFalse();

    assertThat(config.syncUserGroups()).isFalse();
    settings.setProperty(GITLAB_AUTH_SYNC_USER_GROUPS, true);
    assertThat(config.syncUserGroups()).isTrue();

    settings.setProperty(GITLAB_AUTH_PROVISIONING_TOKEN, "token");
    assertThat(config.provisioningToken()).isEqualTo("token");

    settings.setProperty(GITLAB_AUTH_PROVISIONING_GROUPS, new String[] {"Group1", "Group2"});
    assertThat(config.provisioningGroups()).containsExactlyInAnyOrder("Group1", "Group2");

    assertThat(config.isProvisioningEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
    assertThat(config.isProvisioningEnabled()).isTrue();
  }

  @Test
  public void isProvisioningEnabled_whenNotSet_returnsFalse() {
    enableGithubAuthentication();
    assertThat(config.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_ifProvisioningDisabled_returnsFalse() {
    enableGithubAuthentication();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, false);
    assertThat(config.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_ifProvisioningEnabledButGithubAuthDisabled_returnsFalse() {
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
    assertThat(config.isProvisioningEnabled()).isFalse();
  }

  @Test
  public void isProvisioningEnabled_ifProvisioningEnabledAndGithubAuthEnabled_returnsTrue() {
    enableGithubAuthentication();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
    assertThat(config.isProvisioningEnabled()).isTrue();
  }

  private void enableGithubAuthentication() {
    settings.setProperty(GITLAB_AUTH_ENABLED, true);
    settings.setProperty(GITLAB_AUTH_APPLICATION_ID, "on");
    settings.setProperty(GITLAB_AUTH_SECRET, "on");
  }

}

