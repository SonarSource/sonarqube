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
package org.sonar.server.almintegration.ws;

import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class CheckPatAction implements AlmIntegrationsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String APP_PASSWORD_CANNOT_BE_NULL = "App Password and Username cannot be null";
  private static final String PAT_CANNOT_BE_NULL = "PAT cannot be null";
  private static final String URL_CANNOT_BE_NULL = "URL cannot be null";
  private static final String WORKSPACE_CANNOT_BE_NULL = "Workspace cannot be null";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AzureDevOpsHttpClient azureDevOpsHttpClient;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final GitlabHttpClient gitlabHttpClient;

  public CheckPatAction(DbClient dbClient, UserSession userSession,
    AzureDevOpsHttpClient azureDevOpsHttpClient,
    BitbucketCloudRestClient bitbucketCloudRestClient,
    BitbucketServerRestClient bitbucketServerRestClient,
    GitlabHttpClient gitlabHttpClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.gitlabHttpClient = gitlabHttpClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("check_pat")
      .setDescription("Check validity of a Personal Access Token for the given DevOps Platform setting<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setInternal(true)
      .setSince("8.2")
      .setHandler(this)
      .setChangelog(new Change("9.0", "Bitbucket Cloud support was added"));

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");
  }

  @Override
  public void handle(Request request, Response response) {
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      String userUuid = requireNonNull(userSession.getUuid(), "User cannot be null");
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));

      AlmPatDto almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
        .orElseThrow(() -> new IllegalArgumentException(String.format("personal access token for '%s' is missing", almSettingKey)));

      switch (almSettingDto.getAlm()) {
        case AZURE_DEVOPS:
          azureDevOpsHttpClient.checkPAT(
            requireNonNull(almSettingDto.getUrl(), URL_CANNOT_BE_NULL),
            requireNonNull(almPatDto.getPersonalAccessToken(), PAT_CANNOT_BE_NULL));
          break;
        case BITBUCKET:
          // Do an authenticate call to Bitbucket Server to validate that the user's personal access token is valid
          bitbucketServerRestClient.getRecentRepo(
            requireNonNull(almSettingDto.getUrl(), URL_CANNOT_BE_NULL),
            requireNonNull(almPatDto.getPersonalAccessToken(), PAT_CANNOT_BE_NULL));
          break;
        case GITLAB:
          gitlabHttpClient.searchProjects(
            requireNonNull(almSettingDto.getUrl(), URL_CANNOT_BE_NULL),
            requireNonNull(almPatDto.getPersonalAccessToken(), PAT_CANNOT_BE_NULL),
            null, null, null);
          break;
        case BITBUCKET_CLOUD:
          bitbucketCloudRestClient.validateAppPassword(
            requireNonNull(almPatDto.getPersonalAccessToken(), APP_PASSWORD_CANNOT_BE_NULL),
            requireNonNull(almSettingDto.getAppId(), WORKSPACE_CANNOT_BE_NULL));
          break;
        case GITHUB:
        default:
          throw new IllegalArgumentException(String.format("unsupported DevOps Platform %s", almSettingDto.getAlm()));
      }

    }
  }

}
