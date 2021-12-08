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
package org.sonar.alm.client.bitbucketserver;

import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;

@ServerSide
public class BitbucketServerSettingsValidator {
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final Encryption encryption;

  public BitbucketServerSettingsValidator(BitbucketServerRestClient bitbucketServerRestClient, Settings settings) {
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.encryption = settings.getEncryption();
  }

  public void validate(AlmSettingDto almSettingDto) {
    String bitbucketUrl = almSettingDto.getUrl();
    String bitbucketToken = almSettingDto.getDecryptedPersonalAccessToken(encryption);
    if (bitbucketUrl == null || bitbucketToken == null) {
      throw new IllegalArgumentException("Your global Bitbucket Server configuration is incomplete.");
    }

    bitbucketServerRestClient.validateUrl(bitbucketUrl);
    bitbucketServerRestClient.validateToken(bitbucketUrl, bitbucketToken);
    bitbucketServerRestClient.validateReadPermission(bitbucketUrl, bitbucketToken);
  }
}
