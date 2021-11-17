/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

    settings.setProperty(GITLAB_AUTH_URL, "https://gitlab.com/api/");
    assertThat(config.url()).isEqualTo("https://gitlab.com/api");

    settings.setProperty(GITLAB_AUTH_URL, "https://gitlab.com/api");
    assertThat(config.url()).isEqualTo("https://gitlab.com/api");

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
  }
}
