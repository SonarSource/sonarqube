/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import static org.sonar.db.alm.setting.ALM.BITBUCKET;
import static org.sonar.db.alm.setting.ALM.BITBUCKET_CLOUD;

public class CreateBitbucketCloudAction implements AlmSettingsWsAction {
  private static final String PARAM_KEY = "key";
  private static final String PARAM_CLIENT_ID = "clientId";
  private static final String PARAM_CLIENT_SECRET = "clientSecret";
  private static final String PARAM_WORKSPACE = "workspace";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;

  public CreateBitbucketCloudAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_bitbucketcloud")
      .setDescription("Configure a new instance of Bitbucket Cloud. <br/>" +
        "Requires the 'Administer System' permission")
      .setPost(true)
      .setSince("8.7")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Unique key of the Bitbucket Cloud setting");
    action.createParam(PARAM_WORKSPACE)
      .setRequired(true)
      .setDescription("Bitbucket Cloud workspace ID");
    action.createParam(PARAM_CLIENT_ID)
      .setRequired(true)
      .setMaximumLength(2000)
      .setDescription("Bitbucket Cloud Client ID");
    action.createParam(PARAM_CLIENT_SECRET)
      .setRequired(true)
      .setMaximumLength(2000)
      .setDescription("Bitbucket Cloud Client Secret");
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    String key = request.mandatoryParam(PARAM_KEY);
    String clientId = request.mandatoryParam(PARAM_CLIENT_ID);
    String clientSecret = request.mandatoryParam(PARAM_CLIENT_SECRET);
    String workspace = request.mandatoryParam(PARAM_WORKSPACE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      // We do not treat Bitbucket Server and Bitbucket Cloud as different ALMs when it comes to limiting the
      // number of connections.
      almSettingsSupport.checkAlmMultipleFeatureEnabled(BITBUCKET);
      almSettingsSupport.checkAlmMultipleFeatureEnabled(BITBUCKET_CLOUD);
      almSettingsSupport.checkAlmSettingDoesNotAlreadyExist(dbSession, key);
      almSettingsSupport.checkBitbucketCloudWorkspaceIDFormat(workspace);
      dbClient.almSettingDao().insert(dbSession, new AlmSettingDto()
        .setAlm(BITBUCKET_CLOUD)
        .setKey(key)
        .setAppId(workspace)
        .setClientId(clientId)
        .setClientSecret(clientSecret));
      dbSession.commit();
    }
  }
}
