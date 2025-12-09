/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.alm.client.github;

import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ServerSide
public class GithubGlobalSettingsValidator {

  private final Encryption encryption;
  private final GithubApplicationClient githubApplicationClient;

  public GithubGlobalSettingsValidator(GithubApplicationClientImpl githubApplicationClient, Settings settings) {
    this.encryption = settings.getEncryption();
    this.githubApplicationClient = githubApplicationClient;
  }

  public GithubAppConfiguration validate(AlmSettingDto almSettingDto) {
    return validate(almSettingDto.getAppId(), almSettingDto.getClientId(), almSettingDto.getClientSecret(), almSettingDto.getPrivateKey(), almSettingDto.getUrl());
  }

  public GithubAppConfiguration validate(@Nullable String applicationId, @Nullable String clientId, String clientSecret, String privateKey,  @Nullable String url) {
    long appId;
    try {
      appId = Long.parseLong(Optional.ofNullable(applicationId).orElseThrow(() -> new IllegalArgumentException("Missing appId")));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid appId; " + e.getMessage());
    }
    if (isBlank(clientId)) {
      throw new IllegalArgumentException("Missing Client Id");
    }
    if (isBlank(getDecryptedSettingValue(clientSecret))) {
      throw new IllegalArgumentException("Missing Client Secret");
    }
    GithubAppConfiguration configuration = new GithubAppConfiguration(appId, getDecryptedSettingValue(privateKey), url);

    githubApplicationClient.checkApiEndpoint(configuration);
    githubApplicationClient.checkAppPermissions(configuration);

    return configuration;
  }

  private String getDecryptedSettingValue(String setting) {
    if (StringUtils.isNotEmpty(setting) && encryption.isEncrypted(setting)) {
      return encryption.decrypt(setting);
    }
    return setting;
  }
}
