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
package org.sonar.server.almintegration.ws.github;

import java.util.List;
import java.util.Optional;
import org.sonar.auth.github.client.GithubApplicationClient;
import org.sonar.auth.github.client.GithubApplicationClient.Organization;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.auth.github.security.AccessToken;
import org.sonar.auth.github.security.UserAccessToken;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.AlmIntegrations.ListGithubOrganizationsWsResponse;
import org.sonarqube.ws.Common;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListGithubOrganizationsAction implements AlmIntegrationsWsAction {

  public static final String PARAM_ALM_SETTING = "almSetting";
  public static final String PARAM_TOKEN = "token";

  private final DbClient dbClient;
  private final Encryption encryption;
  private final UserSession userSession;
  private final GithubApplicationClient githubApplicationClient;

  public ListGithubOrganizationsAction(DbClient dbClient, Settings settings, UserSession userSession,
    GithubApplicationClientImpl githubApplicationClient) {
    this.dbClient = dbClient;
    this.encryption = settings.getEncryption();
    this.userSession = userSession;
    this.githubApplicationClient = githubApplicationClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list_github_organizations")
      .setDescription("List GitHub organizations<br/>" +
        "Requires the 'Create Projects' permission")
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-list_github_organizations.json"))
      .setSince("8.4")
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

    action.createParam(PARAM_TOKEN)
      .setMaximumLength(200)
      .setDescription("Github authorization code");

    action.createParam(PAGE)
      .setDescription("Index of the page to display")
      .setDefaultValue(1);
    action.createParam(PAGE_SIZE)
      .setDescription("Size for the paging to apply")
      .setDefaultValue(100);
  }

  @Override
  public void handle(Request request, Response response) {
    ListGithubOrganizationsWsResponse getResponse = doHandle(request);
    writeProtobuf(getResponse, request, response);
  }

  private ListGithubOrganizationsWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("GitHub Setting '%s' not found", almSettingKey)));

      String userUuid = requireNonNull(userSession.getUuid(), "User UUID is not null");
      String url = requireNonNull(almSettingDto.getUrl(), String.format("No URL set for GitHub '%s'", almSettingKey));

      AccessToken accessToken;
      if (request.hasParam(PARAM_TOKEN)) {
        String code = request.mandatoryParam(PARAM_TOKEN);
        String clientId = requireNonNull(almSettingDto.getClientId(), String.format("No clientId set for GitHub '%s'", almSettingKey));
        String clientSecret = requireNonNull(almSettingDto.getDecryptedClientSecret(encryption), String.format("No clientSecret set for GitHub '%s'",
          almSettingKey));

        try {
          accessToken = githubApplicationClient.createUserAccessToken(url, clientId, clientSecret, code);
        } catch (IllegalArgumentException e) {
          // it could also be that the code has expired!
          throw BadRequestException.create("Unable to authenticate with GitHub. "
            + "Check the GitHub App client ID and client secret configured in the Global Settings and try again.");
        }
        Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
        if (almPatDto.isPresent()) {
          AlmPatDto almPat = almPatDto.get();
          almPat.setPersonalAccessToken(accessToken.getValue());
          dbClient.almPatDao().update(dbSession, almPat, userSession.getLogin(), almSettingDto.getKey());
        } else {
          AlmPatDto almPat = new AlmPatDto()
            .setPersonalAccessToken(accessToken.getValue())
            .setAlmSettingUuid(almSettingDto.getUuid())
            .setUserUuid(userUuid);
          dbClient.almPatDao().insert(dbSession, almPat, userSession.getLogin(), almSettingDto.getKey());
        }
        dbSession.commit();
      } else {
        accessToken = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
          .map(AlmPatDto::getPersonalAccessToken)
          .map(UserAccessToken::new)
          .orElseThrow(() -> new IllegalArgumentException("No personal access token found"));
      }

      int page = request.hasParam(PAGE) ? request.mandatoryParamAsInt(PAGE) : 1;
      int pageSize = request.hasParam(PAGE_SIZE) ? request.mandatoryParamAsInt(PAGE_SIZE) : 100;
      GithubApplicationClient.Organizations githubOrganizations = githubApplicationClient.listOrganizations(url, accessToken, page, pageSize);

      ListGithubOrganizationsWsResponse.Builder response = ListGithubOrganizationsWsResponse.newBuilder()
        .setPaging(Common.Paging.newBuilder()
          .setPageIndex(page)
          .setPageSize(pageSize)
          .setTotal(githubOrganizations.getTotal())
          .build());

      List<Organization> organizations = githubOrganizations.getOrganizations();
      if (organizations != null) {
        organizations
          .forEach(githubOrganization -> response.addOrganizations(AlmIntegrations.GithubOrganization.newBuilder()
            .setKey(githubOrganization.getLogin())
            .setName(githubOrganization.getLogin())
            .build()));
      }

      return response.build();
    }
  }
}
