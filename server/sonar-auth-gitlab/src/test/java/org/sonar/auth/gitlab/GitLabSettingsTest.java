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
package org.sonar.auth.gitlab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOWED_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_ALL_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_APPLICATION_ID;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_TOKEN;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SECRET;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SYNC_USER_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_USER_CONSENT_FOR_PERMISSION_PROVISIONING_REQUIRED;
import static org.sonar.auth.gitlab.GitLabSettings.isGitlabCloudUrl;
import static org.sonar.db.ce.CeTaskTypes.GITLAB_PROJECT_PERMISSIONS_PROVISIONING;

class GitLabSettingsTest {

  private MapSettings settings;
  private GitLabSettings config;


  @BeforeEach
  void prepare() {
    settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, GitLabSettings.definitions()));
    config = new GitLabSettings(settings.asConfig());
  }

  @Test
  void test_settings() {
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

    settings.setProperty(GITLAB_AUTH_ALLOWED_GROUPS, new String[] {"Group1", "Group2"});
    assertThat(config.allowedGroups()).containsExactlyInAnyOrder("Group1", "Group2");

    assertThat(config.isProvisioningEnabled()).isFalse();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
    assertThat(config.isProvisioningEnabled()).isTrue();
  }

  @Test
  void isProvisioningEnabled_whenNotSet_returnsFalse() {
    enableGitlabAuthentication();
    assertThat(config.isProvisioningEnabled()).isFalse();
  }

  @Test
  void isProvisioningEnabled_ifProvisioningDisabled_returnsFalse() {
    enableGitlabAuthentication();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, false);
    assertThat(config.isProvisioningEnabled()).isFalse();
  }

  @Test
  void isProvisioningEnabled_ifProvisioningEnabledButGithubAuthDisabled_returnsFalse() {
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
    assertThat(config.isProvisioningEnabled()).isFalse();
  }

  @Test
  void isProvisioningEnabled_ifProvisioningEnabledAndGithubAuthEnabled_returnsTrue() {
    enableGitlabAuthentication();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
    assertThat(config.isProvisioningEnabled()).isTrue();
  }

  @Test
  void isProjectVisibilitySynchronizationActivated_alwaysReturnsTrue() {
    assertThat(config.isProjectVisibilitySynchronizationActivated()).isTrue();
  }

  @Test
  void isUserConsentRequiredForPermissionProvisioning_returnsFalseByDefault() {
    assertThat(config.isUserConsentRequiredAfterUpgrade()).isFalse();
  }

  @Test
  void isUserConsentRequiredForPermissionProvisioning_returnsTrueWhenPropertyPresent() {
    settings.setProperty(GITLAB_USER_CONSENT_FOR_PERMISSION_PROVISIONING_REQUIRED, "");
    assertThat(config.isUserConsentRequiredAfterUpgrade()).isTrue();
  }

  @Test
  void getProjectsPermissionsProvisioningTaskName_returnsCorrectTaskName() {
    assertThat(config.getProjectsPermissionsProvisioningTaskName()).isEqualTo(GITLAB_PROJECT_PERMISSIONS_PROVISIONING);
  }

  @Test
  void allowAllGroups_defaultsToFalse() {
    assertThat(config.allowAllGroups()).isFalse();
  }

  @Test
  void allowAllGroups_returnsConfiguredValue() {
    enableAutoProvisioningOnSelfManaged();
    settings.setProperty(GITLAB_AUTH_ALLOW_ALL_GROUPS, true);
    assertThat(config.allowAllGroups()).isTrue();
  }

  @Test
  void allowAllGroups_whenProvisioningDisabled_returnsFalseEvenIfPropertyIsTrue() {
    settings.setProperty(GITLAB_AUTH_ALLOW_ALL_GROUPS, true);
    assertThat(config.allowAllGroups()).isFalse();
  }

  @Test
  void allowAllGroups_whenGitlabCloud_returnsFalseEvenIfPropertyIsTrue() {
    enableAutoProvisioning();
    settings.setProperty(GITLAB_AUTH_URL, "https://gitlab.com");
    settings.setProperty(GITLAB_AUTH_ALLOW_ALL_GROUPS, true);

    assertThat(config.allowAllGroups()).isFalse();
  }

  @Test
  void isAllowedGroup_whenAllowAllGroupsIsTrue_returnsTrueRegardlessOfAllowedGroupsContent() {
    enableAutoProvisioningOnSelfManaged();
    settings.setProperty(GITLAB_AUTH_ALLOW_ALL_GROUPS, true);
    settings.setProperty(GITLAB_AUTH_ALLOWED_GROUPS, new String[] {"team-a"});

    assertThat(config.isAllowedGroup("team-a")).isTrue();
    assertThat(config.isAllowedGroup("unrelated-group")).isTrue();
    assertThat(config.isAllowedGroup("nested/sub-group")).isTrue();
  }

  @Test
  void isAllowedGroup_whenAllowAllGroupsIsTrueAndAllowedGroupsEmpty_returnsTrue() {
    enableAutoProvisioningOnSelfManaged();
    settings.setProperty(GITLAB_AUTH_ALLOW_ALL_GROUPS, true);

    assertThat(config.isAllowedGroup("any-group")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "https://gitlab.com",
    "https://gitlab.com/",
    "https://GitLab.com",
    "http://gitlab.com",
    "  https://gitlab.com  ",
    "gitlab.com",
    "GitLab.com",
    "gitlab.com/api/v4",
    "gitlab.com:8080"
  })
  void isGitlabCloudUrl_whenUrlMatches_returnsTrue(String url) {
    assertThat(isGitlabCloudUrl(url)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
    "https://onpremise.gitlab.com",
    "https://gitlab.acme.com",
    "https://gitlab.com.evil.example",
    "onpremise.gitlab.com",
    "gitlab.com.evil.example",
    "not a url",
    "://broken"
  })
  void isGitlabCloudUrl_whenUrlDoesNotMatch_returnsFalse(String url) {
    assertThat(isGitlabCloudUrl(url)).isFalse();
  }

  @Test
  void isAllowedGroup_whenAllowAllGroupsIsFalse_appliesAllowedGroupsFilter() {
    settings.setProperty(GITLAB_AUTH_ALLOW_ALL_GROUPS, false);
    settings.setProperty(GITLAB_AUTH_ALLOWED_GROUPS, new String[] {"team-a"});

    assertThat(config.isAllowedGroup("team-a")).isTrue();
    assertThat(config.isAllowedGroup("team-a/sub")).isTrue();
    assertThat(config.isAllowedGroup("team-b")).isFalse();
  }

  private void enableGitlabAuthentication() {
    settings.setProperty(GITLAB_AUTH_ENABLED, true);
    settings.setProperty(GITLAB_AUTH_APPLICATION_ID, "on");
    settings.setProperty(GITLAB_AUTH_SECRET, "on");
  }

  private void enableAutoProvisioning() {
    enableGitlabAuthentication();
    settings.setProperty(GITLAB_AUTH_PROVISIONING_ENABLED, true);
  }

  private void enableAutoProvisioningOnSelfManaged() {
    enableAutoProvisioning();
    settings.setProperty(GITLAB_AUTH_URL, "https://gitlab.example.com");
  }

}
