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
package org.sonar.server.almsettings.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.removeEnd;

public class UpdateGithubAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_NEW_KEY = "newKey";
  private static final String PARAM_URL = "url";
  private static final String PARAM_APP_ID = "appId";
  private static final String PARAM_CLIENT_ID = "clientId";
  private static final String PARAM_CLIENT_SECRET = "clientSecret";
  private static final String PARAM_PRIVATE_KEY = "privateKey";
  private static final String PARAM_WEBHOOK_SECRET = "webhookSecret";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;

  public UpdateGithubAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("update_github")
      .setDescription("Update GitHub instance Setting. <br/>" +
        "Requires the 'Administer System' permission")
      .setPost(true)
      .setSince("8.1")
      .setChangelog(
        new Change("9.7", String.format("Optional parameter '%s' was added", PARAM_WEBHOOK_SECRET)),
        new Change("8.7", String.format("Parameter '%s' is no longer required", PARAM_PRIVATE_KEY)),
        new Change("8.7", String.format("Parameter '%s' is no longer required", PARAM_CLIENT_SECRET)))
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Unique key of the GitHub instance setting");
    action.createParam(PARAM_NEW_KEY)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Optional new value for an unique key of the GitHub instance setting");
    action.createParam(PARAM_URL)
      .setRequired(true)
      .setMaximumLength(2000)
      .setDescription("GitHub API URL");
    action.createParam(PARAM_APP_ID)
      .setRequired(true)
      .setMaximumLength(80)
      .setDescription("GitHub API ID");
    action.createParam(PARAM_PRIVATE_KEY)
      .setRequired(false)
      .setMaximumLength(2500)
      .setDescription("GitHub App private key");
    action.createParam(PARAM_CLIENT_ID)
      .setRequired(true)
      .setMaximumLength(80)
      .setDescription("GitHub App Client ID");
    action.createParam(PARAM_CLIENT_SECRET)
      .setRequired(false)
      .setMaximumLength(160)
      .setDescription("GitHub App Client Secret");
    action.createParam(PARAM_WEBHOOK_SECRET)
      .setRequired(false)
      .setMaximumLength(160)
      .setDescription("GitHub App Webhook Secret");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    tryDoHandle(request);
    response.noContent();
  }

  private void tryDoHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      doHandle(request, dbSession);
    }
  }

  private void doHandle(Request request, DbSession dbSession) {
    String key = request.mandatoryParam(PARAM_KEY);
    String newKey = request.param(PARAM_NEW_KEY);

    if (isNotBlank(newKey) && !newKey.equals(key)) {
      almSettingsSupport.checkAlmSettingDoesNotAlreadyExist(dbSession, newKey);
    }

    AlmSettingDto almSettingDto = almSettingsSupport.getAlmSetting(dbSession, key);
    String url = request.mandatoryParam(PARAM_URL);
    String privateKey = request.param(PARAM_PRIVATE_KEY);

    almSettingsSupport.checkPrivateKeyOnUrlUpdate(almSettingDto, url, privateKey);

    if (isNotBlank(privateKey)) {
      almSettingDto.setPrivateKey(privateKey);
    }

    String clientSecret = request.param(PARAM_CLIENT_SECRET);
    if (isNotBlank(clientSecret)) {
      almSettingDto.setClientSecret(clientSecret);
    }

    boolean hasWebhookSecretParam = request.hasParam(PARAM_WEBHOOK_SECRET);
    if (hasWebhookSecretParam) {
      String webhookSecret = request.getParam(PARAM_WEBHOOK_SECRET).getValue();
      almSettingDto.setWebhookSecret(isBlank(webhookSecret) ? null : webhookSecret);
    }

    almSettingDto
      .setKey(isNotBlank(newKey) ? newKey : key)
      .setUrl(removeEnd(request.mandatoryParam(PARAM_URL), "/"))
      .setAppId(request.mandatoryParam(PARAM_APP_ID))
      .setClientId(request.mandatoryParam(PARAM_CLIENT_ID));

    boolean isAnySecretUpdated = clientSecret != null || privateKey != null || hasWebhookSecretParam;
    dbClient.almSettingDao().update(dbSession, almSettingDto, isAnySecretUpdated);
    dbSession.commit();
  }

}
