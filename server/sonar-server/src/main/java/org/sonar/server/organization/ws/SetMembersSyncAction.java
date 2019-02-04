/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.OrganizationAlmBindingDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class SetMembersSyncAction implements OrganizationsWsAction {

  private static final String ENABLED = "enabled";
  private DbClient dbClient;
  private UserSession userSession;

  public SetMembersSyncAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set_members_sync")
      .setDescription("Enable or disable organization members synchronization.<br/>" +
        "Requires 'Administer System' permission on the specified organization.")
      .setSince("7.7")
      .setPost(true)
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setInternal(true)
      .setRequired(true);

    action.createParam(ENABLED)
      .setDescription("True to enable members sync, false otherwise.")
      .setInternal(true)
      .setRequired(true)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);

    try (DbSession dbSession = dbClient.openSession(false)) {

      OrganizationDto organization = checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey),
        "Organization '%s' does not exist", organizationKey);

      userSession.checkPermission(ADMINISTER, organization);

      Optional<OrganizationAlmBindingDto> orgAlmBindingDto = dbClient.organizationAlmBindingDao().selectByOrganization(dbSession, organization);
      checkArgument(orgAlmBindingDto.isPresent(), "Organization '%s' is not bound to an ALM", organization.getKey());

      dbClient.organizationAlmBindingDao().updateMembersSync(dbSession, orgAlmBindingDto.get(), request.mandatoryParamAsBoolean(ENABLED));

      dbSession.commit();
    }

    response.noContent();
  }

}
