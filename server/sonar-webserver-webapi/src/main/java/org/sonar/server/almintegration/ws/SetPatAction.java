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
package org.sonar.server.almintegration.ws;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
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

import static java.util.Objects.requireNonNull;
import static org.sonar.db.alm.setting.ALM.BITBUCKET_CLOUD;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class SetPatAction implements AlmIntegrationsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_PAT = "pat";
  private static final String PARAM_USERNAME = "username";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ImportHelper importHelper;

  public SetPatAction(DbClient dbClient, UserSession userSession, ImportHelper importHelper) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.importHelper = importHelper;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_pat")
      .setDescription("Set a Personal Access Token for the given DevOps Platform setting<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.2")
      .setHandler(this)
      .setChangelog(
        new Change("9.0", "Bitbucket Cloud support and optional Username parameter were added"),
        new Change("10.3", "Allow setting Personal Access Tokens for all DevOps platforms"),
        new Change("10.3", String.format("Parameter %s becomes optional if you have only one DevOps Platform configuration", PARAM_ALM_SETTING)));

    action.createParam(PARAM_ALM_SETTING)
      .setDescription("DevOps Platform configuration key. This parameter is optional if you have only one single DevOps Platform integration.");

    action.createParam(PARAM_PAT)
      .setRequired(true)
      .setMaximumLength(2000)
      .setDescription("Personal Access Token");

    action.createParam(PARAM_USERNAME)
      .setRequired(false)
      .setMaximumLength(2000)
      .setDescription("Username");
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
      String username = request.param(PARAM_USERNAME);

      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null");
      AlmSettingDto almSettingDto = importHelper.getAlmSettingDto(request);

      if (almSettingDto.getAlm().equals(BITBUCKET_CLOUD)) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username), "Username cannot be null for Bitbucket Cloud");
      }

      String resultingPat = CredentialsEncoderHelper.encodeCredentials(almSettingDto.getAlm(), pat, username);

      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
      if (almPatDto.isPresent()) {
        AlmPatDto almPat = almPatDto.get();
        almPat.setPersonalAccessToken(resultingPat);
        dbClient.almPatDao().update(dbSession, almPat, userSession.getLogin(), almSettingDto.getKey());
      } else {
        AlmPatDto almPat = new AlmPatDto()
          .setPersonalAccessToken(resultingPat)
          .setAlmSettingUuid(almSettingDto.getUuid())
          .setUserUuid(userUuid);
        dbClient.almPatDao().insert(dbSession, almPat, userSession.getLogin(), almSettingDto.getKey());
      }
      dbSession.commit();
    }
  }

  public AlmSettingDto getAlmConfig(@Nullable String almSettingKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (almSettingKey != null) {
        return getAlmSettingDtoFromKey(dbSession, almSettingKey);
      }
      return getAlmSettingDtoFromAlm(dbSession);
    }
  }

  private AlmSettingDto getAlmSettingDtoFromKey(DbSession dbSession, String almSettingKey) {
    return dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
      .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform configuration '%s' not found.", almSettingKey)));
  }

  private AlmSettingDto getAlmSettingDtoFromAlm(DbSession dbSession) {
    List<AlmSettingDto> almSettingDtos = dbClient.almSettingDao().selectAll(dbSession);
    if (almSettingDtos.isEmpty()) {
      throw new NotFoundException("There is no configuration for DevOps Platforms. Please add one.");
    }
    if (almSettingDtos.size() == 1) {
      return almSettingDtos.get(0);
    }
    throw new IllegalArgumentException(String.format("Parameter %s is required as there are multiple DevOps Platform configurations.", PARAM_ALM_SETTING));
  }

}
