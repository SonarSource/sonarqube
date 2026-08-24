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
package org.sonar.alm.client.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.api.config.internal.Settings;

import static java.util.Objects.requireNonNull;

@ServerSide
public class AzureDevOpsValidator {

  private static final Logger LOG = LoggerFactory.getLogger(AzureDevOpsValidator.class);

  public static final String GLOBAL_PAT_ERROR_MESSAGE = "Global personal access tokens (\"All accessible organizations\") are being retired by " +
    "Microsoft and cannot be used. Create a personal access token scoped to a single organization.";

  private final AzureDevOpsHttpClient azureDevOpsHttpClient;
  private final Settings settings;

  public AzureDevOpsValidator(AzureDevOpsHttpClient azureDevOpsHttpClient, Settings settings) {
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
    this.settings = settings;
  }

  public void validate(AlmSettingDto dto) {
    try {
      azureDevOpsHttpClient.checkPAT(requireNonNull(dto.getUrl()),
        requireNonNull(dto.getDecryptedPersonalAccessToken(settings.getEncryption())));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid Azure URL or Personal Access Token", e);
    }
  }

  /**
   * Denies binding an Azure DevOps Services (Cloud) organization with a Global PAT ("All accessible
   * organizations"). Azure DevOps Server has no cross-org scope, so non-Cloud URLs are always allowed.
   * A probe failure (network error, Microsoft outage) is treated as inconclusive and fails open, so a
   * transient issue never blocks configuring a valid binding.
   */
  public void checkPatIsNotGlobal(String url, String pat) {
    if (!AzureDevOpsUrls.isAzureDevOpsServices(url)) {
      return;
    }
    boolean isGlobal;
    try {
      isGlobal = azureDevOpsHttpClient.isGlobalPat(pat);
    } catch (IllegalArgumentException e) {
      LOG.warn("Unable to determine whether the Azure DevOps personal access token is global, allowing the binding", e);
      return;
    }
    if (isGlobal) {
      throw new IllegalArgumentException(GLOBAL_PAT_ERROR_MESSAGE);
    }
  }
}
