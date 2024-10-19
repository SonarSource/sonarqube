/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;

import static com.google.common.base.Strings.isNullOrEmpty;

@ServerSide
public class GitlabGlobalSettingsValidator {

  public enum ValidationMode {COMPLETE, AUTH_ONLY}
  private final Encryption encryption;
  private final GitlabApplicationClient gitlabApplicationClient;

  public GitlabGlobalSettingsValidator(GitlabApplicationClient gitlabApplicationClient, Settings settings) {
    this.encryption = settings.getEncryption();
    this.gitlabApplicationClient = gitlabApplicationClient;
  }

  public void validate(AlmSettingDto almSettingDto) {
    String gitlabUrl = almSettingDto.getUrl();
    String accessToken = almSettingDto.getDecryptedPersonalAccessToken(encryption);
    validate(ValidationMode.COMPLETE, gitlabUrl, accessToken);
  }

  public void validate(ValidationMode validationMode, @Nullable String gitlabApiUrl, @Nullable String accessToken) {
    if (gitlabApiUrl == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
    }
    gitlabApplicationClient.checkUrl(gitlabApiUrl);
    if (ValidationMode.AUTH_ONLY.equals(validationMode)) {
      return;
    }

    String decryptedToken = getDecryptedToken(accessToken);
    if (decryptedToken == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete. The GitLab access token must be set.");
    }
    gitlabApplicationClient.checkToken(gitlabApiUrl, decryptedToken);
    gitlabApplicationClient.checkReadPermission(gitlabApiUrl, decryptedToken);
    gitlabApplicationClient.checkWritePermission(gitlabApiUrl, decryptedToken);
  }

  @CheckForNull
  public String getDecryptedToken(@Nullable String token) {
    if (!isNullOrEmpty(token) && encryption.isEncrypted(token)) {
      return encryption.decrypt(token);
    }
    return token;
  }
}
