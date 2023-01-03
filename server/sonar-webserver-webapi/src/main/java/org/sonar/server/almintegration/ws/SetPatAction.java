/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Preconditions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.alm.setting.ALM.AZURE_DEVOPS;
import static org.sonar.db.alm.setting.ALM.BITBUCKET;
import static org.sonar.db.alm.setting.ALM.GITLAB;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SetPatAction implements AlmIntegrationsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_PAT = "pat";

  private final DbClient dbClient;
  private final UserSession userSession;

  public SetPatAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_pat")
      .setDescription("Set a Personal Access Token for the given ALM setting<br/>" +
        "Only valid for Azure DevOps, Bitbucket Server & GitLab Alm Setting<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.2")
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setDescription("ALM setting key");
    action.createParam(PARAM_PAT)
      .setRequired(true)
      .setMaximumLength(2000)
      .setDescription("Personal Access Token");
  }

  @Override
  public void handle(Request request, Response response) {
    doHandle(request);
    response.noContent();
  }

  private void doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String pat = request.mandatoryParam(PARAM_PAT);
      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);

      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null");
      AlmSettingDto almSetting = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(format("ALM Setting '%s' not found", almSettingKey)));

      Preconditions.checkArgument(Arrays.asList(AZURE_DEVOPS, BITBUCKET, GITLAB)
        .contains(almSetting.getAlm()), "Only Azure DevOps, Bibucket Server and GitLab ALM Settings are supported.");

      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSetting);
      if (almPatDto.isPresent()) {
        AlmPatDto almPat = almPatDto.get();
        almPat.setPersonalAccessToken(pat);
        dbClient.almPatDao().update(dbSession, almPat);
      } else {
        AlmPatDto almPat = new AlmPatDto()
          .setPersonalAccessToken(pat)
          .setAlmSettingUuid(almSetting.getUuid())
          .setUserUuid(userUuid);
        dbClient.almPatDao().insert(dbSession, almPat);
      }
      dbSession.commit();
    }
  }

}
