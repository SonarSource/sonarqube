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

public class DeleteAction implements AlmSettingsWsAction {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;

  public DeleteAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("delete")
      .setDescription("Delete an DevOps Platform Setting.<br/>" +
        "Requires the 'Administer System' permission")
      .setSince("8.1")
      .setPost(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setDescription("DevOps Platform Setting key")
      .setRequired(true);
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
      dbClient.projectAlmSettingDao().deleteByAlmSetting(dbSession, almSettingDto);
      dbClient.almPatDao().deleteByAlmSetting(dbSession, almSettingDto);
      dbClient.almSettingDao().delete(dbSession, almSettingDto);
      dbSession.commit();
    }
  }
}
