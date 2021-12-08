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
package org.sonar.alm.client.gitlab;

import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;

@ServerSide
public class GitlabGlobalSettingsValidator {

  private final Encryption encryption;
  private final GitlabHttpClient gitlabHttpClient;

  public GitlabGlobalSettingsValidator(GitlabHttpClient gitlabHttpClient, Settings settings) {
    this.encryption = settings.getEncryption();
    this.gitlabHttpClient = gitlabHttpClient;
  }

  public void validate(AlmSettingDto almSettingDto) {
    String gitlabUrl = almSettingDto.getUrl();
    String accessToken = almSettingDto.getDecryptedPersonalAccessToken(encryption);

    if (gitlabUrl == null || accessToken == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete.");
    }

    gitlabHttpClient.checkUrl(gitlabUrl);
    gitlabHttpClient.checkToken(gitlabUrl, accessToken);
    gitlabHttpClient.checkReadPermission(gitlabUrl, accessToken);
    gitlabHttpClient.checkWritePermission(gitlabUrl, accessToken);
  }

}
