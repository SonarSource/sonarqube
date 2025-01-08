/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class ValidateAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;
  private final GitlabGlobalSettingsValidator gitlabSettingsValidator;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final BitbucketServerSettingsValidator bitbucketServerSettingsValidator;
  private final BitbucketCloudValidator bitbucketCloudValidator;
  private final AzureDevOpsValidator azureDevOpsValidator;

  public ValidateAction(DbClient dbClient,
    UserSession userSession,
    AlmSettingsSupport almSettingsSupport,
    GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GitlabGlobalSettingsValidator gitlabSettingsValidator,
    BitbucketServerSettingsValidator bitbucketServerSettingsValidator,
    BitbucketCloudValidator bitbucketCloudValidator,
    AzureDevOpsValidator azureDevOpsValidator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.gitlabSettingsValidator = gitlabSettingsValidator;
    this.bitbucketServerSettingsValidator = bitbucketServerSettingsValidator;
    this.bitbucketCloudValidator = bitbucketCloudValidator;
    this.azureDevOpsValidator = azureDevOpsValidator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("validate")
      .setDescription("Validate an DevOps Platform Setting by checking connectivity and permissions<br/>" +
        "Requires the 'Administer System' permission")
      .setSince("8.6")
      .setResponseExample(getClass().getResource("example-validate.json"))
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Unique key of the DevOps Platform settings");
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
          gitlabSettingsValidator.validate(almSettingDto);
          break;
        case GITHUB:
          githubGlobalSettingsValidator.validate(almSettingDto);
          break;
        case BITBUCKET:
          bitbucketServerSettingsValidator.validate(almSettingDto);
          break;
        case BITBUCKET_CLOUD:
          bitbucketCloudValidator.validate(almSettingDto);
          break;
        case AZURE_DEVOPS:
          azureDevOpsValidator.validate(almSettingDto);
          break;
      }
    }
  }
}
