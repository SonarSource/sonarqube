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
package org.sonar.server.v2.api.dop.controller;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.dop.response.DopSettingsResource;
import org.sonar.server.v2.api.dop.response.DopSettingsRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class DefaultDopSettingsController implements DopSettingsController {

  private final UserSession userSession;
  private final DbClient dbClient;

  public DefaultDopSettingsController(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public DopSettingsRestResponse fetchAllDopSettings() {
    userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<DopSettingsResource> dopSettingsResources = dbClient.almSettingDao().selectAll(dbSession)
        .stream()
        .map(DefaultDopSettingsController::toDopSettingsResource)
        .toList();

      PageRestResponse pageRestResponse = new PageRestResponse(1, dopSettingsResources.size(), dopSettingsResources.size());
      return new DopSettingsRestResponse(dopSettingsResources, pageRestResponse);
    }
  }

  private static DopSettingsResource toDopSettingsResource(AlmSettingDto almSettingDto) {
    return new DopSettingsResource(
      almSettingDto.getUuid(),
      almSettingDto.getRawAlm(),
      almSettingDto.getKey(),
      almSettingDto.getUrl(),
      almSettingDto.getAppId()
    );
  }

}
