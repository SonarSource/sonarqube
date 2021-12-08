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
package org.sonar.alm.client.github;

import java.util.Optional;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.apache.commons.lang.StringUtils.isBlank;

@ServerSide
public class GithubGlobalSettingsValidator {

  private final Encryption encryption;
  private final GithubApplicationClient githubApplicationClient;

  public GithubGlobalSettingsValidator(GithubApplicationClientImpl githubApplicationClient, Settings settings) {
    this.encryption = settings.getEncryption();
    this.githubApplicationClient = githubApplicationClient;
  }

  public GithubAppConfiguration validate(AlmSettingDto settings) {
    long appId;
    try {
      appId = Long.parseLong(Optional.ofNullable(settings.getAppId()).orElseThrow(() -> new IllegalArgumentException("Missing appId")));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid appId; " + e.getMessage());
    }
    if (isBlank(settings.getClientId())) {
      throw new IllegalArgumentException("Missing Client Id");
    }
    if (isBlank(settings.getDecryptedClientSecret(encryption))) {
      throw new IllegalArgumentException("Missing Client Secret");
    }
    GithubAppConfiguration configuration = new GithubAppConfiguration(appId, settings.getDecryptedPrivateKey(encryption),
      settings.getUrl());

    githubApplicationClient.checkApiEndpoint(configuration);
    githubApplicationClient.checkAppPermissions(configuration);

    return configuration;
  }
}
