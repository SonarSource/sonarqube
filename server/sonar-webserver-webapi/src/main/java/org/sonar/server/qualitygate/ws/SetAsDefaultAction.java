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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;

public class SetAsDefaultAction implements QualityGatesWsAction {
  private static final String DEFAULT_QUALITY_GATE_PROPERTY_NAME = "qualitygate.default";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final QualityGatesWsSupport wsSupport;
  private final Logger logger = Loggers.get(SetAsDefaultAction.class);

  public SetAsDefaultAction(DbClient dbClient, UserSession userSession, QualityGatesWsSupport qualityGatesWsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = qualityGatesWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("set_as_default")
      .setDescription("Set a quality gate as the default quality gate.<br>" +
        "Parameter 'name' must be specified. Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setChangelog(
        new Change("10.0", "Parameter 'id' is removed. Use 'name' instead."),
        new Change("8.4", "Parameter 'name' added"),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setDescription("Name of the quality gate to set as default")
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("SonarSource Way");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    String name = request.mandatoryParam(PARAM_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, organization);
      QualityGateDto qualityGate;

      qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, name);

      organization.setDefaultQualityGateUuid(qualityGate.getUuid());
      dbClient.organizationDao().update(dbSession, organization);

      dbSession.commit();
      logger.info("Quality Gate set to default for:: organization: {}, qGate: {}, user: {}", organization.getKey(),
              qualityGate.getName(), userSession.getLogin());
    }

    response.noContent();
  }

}
