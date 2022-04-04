/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.almsettings.ws;

import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.isBlank;

public class ValidateAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;
  private final AzureDevOpsHttpClient azureDevOpsHttpClient;
  private final GitlabHttpClient gitlabHttpClient;
  private final GithubApplicationClient githubApplicationClient;
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;

  public ValidateAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport,
    AzureDevOpsHttpClient azureDevOpsHttpClient,
    GithubApplicationClientImpl githubApplicationClient, GitlabHttpClient gitlabHttpClient,
    BitbucketServerRestClient bitbucketServerRestClient, BitbucketCloudRestClient bitbucketCloudRestClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
    this.githubApplicationClient = githubApplicationClient;
    this.gitlabHttpClient = gitlabHttpClient;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("validate")
      .setDescription("Validate an ALM Setting by checking connectivity and permissions<br/>" +
        "Requires the 'Administer System' permission")
      .setSince("8.6")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Unique key of the ALM settings");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    String key = request.mandatoryParam(PARAM_KEY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      AlmSettingDto almSettingDto = almSettingsSupport.getAlmSetting(dbSession, key);
      switch (almSettingDto.getAlm()) {
        case GITLAB:
          validateGitlab(almSettingDto);
          break;
        case GITHUB:
          validateGitHub(almSettingDto);
          break;
        case BITBUCKET:
          validateBitbucketServer(almSettingDto);
          break;
        case BITBUCKET_CLOUD:
          validateBitbucketCloud(almSettingDto);
          break;
        case AZURE_DEVOPS:
          validateAzure(almSettingDto);
          break;
      }
    }
  }

  private void validateAzure(AlmSettingDto almSettingDto) {
    try {
      azureDevOpsHttpClient.checkPAT(almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid Azure URL or Personal Access Token", e);
    }
  }

  private void validateGitlab(AlmSettingDto almSettingDto) {
    gitlabHttpClient.checkUrl(almSettingDto.getUrl());
    gitlabHttpClient.checkToken(almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken());
    gitlabHttpClient.checkReadPermission(almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken());
    gitlabHttpClient.checkWritePermission(almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken());
  }

  private void validateGitHub(AlmSettingDto settings) {
    long appId;
    try {
      appId = Long.parseLong(settings.getAppId());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid appId; " + e.getMessage());
    }
    if (isBlank(settings.getClientId())) {
      throw new IllegalArgumentException("Missing Client Id");
    }
    if (isBlank(settings.getClientSecret())) {
      throw new IllegalArgumentException("Missing Client Secret");
    }
    GithubAppConfiguration configuration = new GithubAppConfiguration(appId, settings.getPrivateKey(), settings.getUrl());

    githubApplicationClient.checkApiEndpoint(configuration);
    githubApplicationClient.checkAppPermissions(configuration);
  }

  private void validateBitbucketServer(AlmSettingDto almSettingDto) {
    bitbucketServerRestClient.validateUrl(almSettingDto.getUrl());
    bitbucketServerRestClient.validateToken(almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken());
    bitbucketServerRestClient.validateReadPermission(almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken());
  }

  private void validateBitbucketCloud(AlmSettingDto almSettingDto) {
    bitbucketCloudRestClient.validate(almSettingDto.getClientId(), almSettingDto.getClientSecret(), almSettingDto.getAppId());
  }
}
