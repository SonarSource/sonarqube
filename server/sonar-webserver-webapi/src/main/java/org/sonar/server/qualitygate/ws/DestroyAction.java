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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateFinder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;

public class DestroyAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;
  private final QualityGateFinder finder;
  private final Logger logger = Loggers.get(DestroyAction.class);

  public DestroyAction(DbClient dbClient, QualityGatesWsSupport wsSupport, QualityGateFinder finder) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.finder = finder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("destroy")
      .setDescription("Delete a Quality Gate.<br>" +
        "Either 'id' or 'name' must be specified. Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setPost(true)
      .setChangelog(
        new Change("8.4", "Parameter 'name' added"),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."))
      .setHandler(this);

    action.createParam(QualityGatesWsParameters.PARAM_ID)
      .setDescription("ID of the quality gate to delete. This parameter is deprecated. Use 'name' instead.")
      .setRequired(false)
      .setDeprecatedSince("8.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(QualityGatesWsParameters.PARAM_NAME)
      .setDescription("Name of the quality gate to delete")
      .setRequired(false)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setExampleValue("SonarSource Way");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    String uuid = request.param(QualityGatesWsParameters.PARAM_ID);
    String name = request.param(QualityGatesWsParameters.PARAM_NAME);

    checkArgument(name != null ^ uuid != null, "One of 'id' or 'name' must be provided, and not both");

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateDto qualityGate;

      if (uuid != null) {
        qualityGate = wsSupport.getByOrganizationAndUuid(dbSession, organization, uuid);
      } else {
        qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, name);
      }

      QualityGateDto defaultQualityGate = finder.getDefault(dbSession, organization);
      checkArgument(!defaultQualityGate.getUuid().equals(qualityGate.getUuid()), "The default quality gate cannot be removed");
      wsSupport.checkCanEdit(qualityGate);

      logger.debug("Delete Project-qGate association:: organization: {}, qGate: {}", organization.getKey(),
              qualityGate.getName());
      dbClient.projectQgateAssociationDao().deleteByQGateUuid(dbSession, qualityGate.getUuid());
      logger.debug("Delete qGate group permissions:: organization: {}, qGate: {}", organization.getKey(),
              qualityGate.getName());
      dbClient.qualityGateGroupPermissionsDao().deleteByQualityGate(dbSession, qualityGate);
      logger.debug("Delete qGate user permissions:: organization: {}, qGate: {}", organization.getKey(),
              qualityGate.getName());
      dbClient.qualityGateUserPermissionDao().deleteByQualityGate(dbSession, qualityGate);
      logger.debug("Delete qGate conditions:: organization: {}, qGate: {}", organization.getKey(),
              qualityGate.getName());
      dbClient.gateConditionDao().deleteQGateCondition(qualityGate, dbSession);
      dbClient.qualityGateDao().delete(qualityGate, dbSession);
      dbSession.commit();
      logger.info("Deleted Quality Gate:: organization: {}, qGate: {}", organization.getKey(), qualityGate.getName());
      response.noContent();
    }
  }

}
