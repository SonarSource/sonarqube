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
package org.sonar.server.almintegration.validator;

import org.junit.Test;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class GitlabGlobalSettingsValidatorTest {

  private final GitlabHttpClient gitlabHttpClient = mock(GitlabHttpClient.class);

  private final GitlabGlobalSettingsValidator underTest = new GitlabGlobalSettingsValidator(gitlabHttpClient);

  @Test
  public void validate_success() {
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl("https://gitlab.com/api")
      .setPersonalAccessToken("personal-access-token");

    underTest.validate(almSettingDto);
  }

  @Test
  public void validate_fail_url_not_set() {
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl(null)
      .setPersonalAccessToken("personal-access-token");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Your Gitlab global configuration is incomplete.");
  }

  @Test
  public void validate_fail_pat_not_set() {
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl("https://gitlab.com/api")
      .setPersonalAccessToken(null);

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Your Gitlab global configuration is incomplete.");
  }

}
