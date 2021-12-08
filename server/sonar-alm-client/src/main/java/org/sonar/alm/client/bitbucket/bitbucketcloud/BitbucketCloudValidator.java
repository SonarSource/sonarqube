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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.util.Objects.requireNonNull;

@ServerSide
public class BitbucketCloudValidator {

  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final Settings settings;

  public BitbucketCloudValidator(BitbucketCloudRestClient bitbucketCloudRestClient, Settings settings) {
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.settings = settings;
  }

  public void validate(AlmSettingDto dto) {
    String clientId = requireNonNull(dto.getClientId());
    String appId = requireNonNull(dto.getAppId());
    String decryptedClientSecret = requireNonNull(dto.getDecryptedClientSecret(settings.getEncryption()));
    bitbucketCloudRestClient.validate(clientId, decryptedClientSecret, appId);
  }

}
