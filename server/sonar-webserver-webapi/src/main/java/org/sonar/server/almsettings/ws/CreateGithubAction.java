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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

import static org.sonar.db.alm.setting.ALM.GITHUB;

public class CreateGithubAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_URL = "url";
  private static final String PARAM_APP_ID = "appId";
  private static final String PARAM_CLIENT_ID = "clientId";
  private static final String PARAM_CLIENT_SECRET = "clientSecret";
  private static final String PARAM_PRIVATE_KEY = "privateKey";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;

  public CreateGithubAction(DbClient dbClient, UserSession userSession,
    AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_github")
      .setDescription("Create GitHub ALM instance Setting. <br/>" +
        "Requires the 'Administer System' permission")
      .setPost(true)
      .setSince("8.1")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Unique key of the GitHub instance setting");
    action.createParam(PARAM_URL)
      .setRequired(true)
      .setMaximumLength(2000)
      .setDescription("GitHub API URL");
    action.createParam(PARAM_APP_ID)
      .setRequired(true)
      .setMaximumLength(80)
      .setDescription("GitHub App ID");
    action.createParam(PARAM_PRIVATE_KEY)
      .setRequired(true)
      .setMaximumLength(2500)
      .setDescription("GitHub App private key");
    action.createParam(PARAM_CLIENT_ID)
      .setRequired(true)
      .setMaximumLength(80)
      .setDescription("GitHub App Client ID");
    action.createParam(PARAM_CLIENT_SECRET)
      .setRequired(true)
      .setMaximumLength(160)
      .setDescription("GitHub App Client Secret");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    String key = request.mandatoryParam(PARAM_KEY);
    String url = request.mandatoryParam(PARAM_URL);
    String appId = request.mandatoryParam(PARAM_APP_ID);
    String clientId = request.mandatoryParam(PARAM_CLIENT_ID);
    String clientSecret = request.mandatoryParam(PARAM_CLIENT_SECRET);
    String privateKey = request.mandatoryParam(PARAM_PRIVATE_KEY);

    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      almSettingsSupport.checkAlmMultipleFeatureEnabled(GITHUB);
      almSettingsSupport.checkAlmSettingDoesNotAlreadyExist(dbSession, key);
      dbClient.almSettingDao().insert(dbSession, new AlmSettingDto()
        .setAlm(GITHUB)
        .setKey(key)
        .setUrl(url)
        .setAppId(appId)
        .setPrivateKey(privateKey)
        .setClientId(clientId)
        .setClientSecret(clientSecret));
      dbSession.commit();
    }
  }

}
