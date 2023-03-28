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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

public class SetAsDefaultAction implements QualityGatesWsAction {
  private static final String DEFAULT_QUALITY_GATE_PROPERTY_NAME = "qualitygate.default";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGatesWsSupport wsSupport;

  public SetAsDefaultAction(DbClient dbClient, UserSession userSession, QualityGatesWsSupport qualityGatesWsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = qualityGatesWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("set_as_default")
      .setDescription("Set a quality gate as the default quality gate.<br>" +
        "Either 'id' or 'name' must be specified. Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setChangelog(
        new Change("8.4", "Parameter 'name' added"),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("ID of the quality gate to set as default. This parameter is deprecated. Use 'name' instead.")
      .setDeprecatedSince("8.4")
      .setRequired(false)
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_NAME)
      .setDescription("Name of the quality gate to set as default")
      .setRequired(false)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("SonarSource Way");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    String uuid = request.param(PARAM_ID);
    String name = request.param(PARAM_NAME);
    checkArgument(name != null ^ uuid != null, "One of 'id' or 'name' must be provided, and not both");

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, organization);
      QualityGateDto qualityGate;

      if (uuid != null) {
        qualityGate = wsSupport.getByOrganizationAndUuid(dbSession, organization, uuid);
      } else {
        qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, name);
      }
      organization.setDefaultQualityGateUuid(qualityGate.getUuid());
      dbClient.organizationDao().update(dbSession, organization);
      dbSession.commit();
    }

    response.noContent();
  }

}
