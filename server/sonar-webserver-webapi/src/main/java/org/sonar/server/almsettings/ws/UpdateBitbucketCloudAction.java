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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class UpdateBitbucketCloudAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_NEW_KEY = "newKey";
  private static final String PARAM_CLIENT_ID = "clientId";
  private static final String PARAM_CLIENT_SECRET = "clientSecret";
  private static final String PARAM_WORKSPACE = "workspace";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;

  public UpdateBitbucketCloudAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("update_bitbucketcloud")
      .setDescription("Update Bitbucket Cloud Setting. <br/>" +
        "Requires the 'Administer System' permission")
      .setPost(true)
      .setSince("8.7")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Unique key of the Bitbucket Cloud setting");
    action.createParam(PARAM_NEW_KEY)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Optional new value for an unique key of the Bitbucket Cloud setting");
    action.createParam(PARAM_WORKSPACE)
      .setRequired(true)
      .setMaximumLength(80)
      .setDescription("Bitbucket Cloud workspace ID");
    action.createParam(PARAM_CLIENT_ID)
      .setRequired(true)
      .setMaximumLength(80)
      .setDescription("Bitbucket Cloud Client ID");
    action.createParam(PARAM_CLIENT_SECRET)
      .setRequired(false)
      .setMaximumLength(160)
      .setDescription("Optional new value for the Bitbucket Cloud client secret");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    String key = request.mandatoryParam(PARAM_KEY);
    String newKey = request.param(PARAM_NEW_KEY);
    String workspace = request.mandatoryParam(PARAM_WORKSPACE);
    String clientId = request.mandatoryParam(PARAM_CLIENT_ID);
    String clientSecret = request.param(PARAM_CLIENT_SECRET);

    try (DbSession dbSession = dbClient.openSession(false)) {
      AlmSettingDto almSettingDto = almSettingsSupport.getAlmSetting(dbSession, key);
      if (isNotBlank(newKey) && !newKey.equals(key)) {
        almSettingsSupport.checkAlmSettingDoesNotAlreadyExist(dbSession, newKey);
      }

      if (isNotBlank(clientSecret)) {
        almSettingDto.setClientSecret(clientSecret);
      }

      almSettingsSupport.checkBitbucketCloudWorkspaceIDFormat(workspace);

      dbClient.almSettingDao().update(dbSession, almSettingDto
        .setKey(isNotBlank(newKey) ? newKey : key)
        .setClientId(clientId)
        .setAppId(workspace),
        clientSecret != null);
      dbSession.commit();
    }
  }

}
