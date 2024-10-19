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
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_CURRENT_NAME;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class RenameAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGatesWsSupport wsSupport;
  private final Logger logger = Loggers.get(RenameAction.class);

  public RenameAction(DbClient dbClient, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("rename")
      .setPost(true)
      .setDescription("Rename a Quality Gate.<br>" +
        "'currentName' must be specified. Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setChangelog(
        new Change("10.0", "Field 'id' in the response is deprecated"),
        new Change("10.0", "Parameter 'id' is removed. Use 'currentName' instead."),
        new Change("8.4", "Parameter 'currentName' added"),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'currentName' instead."))
      .setHandler(this);

    action.createParam(PARAM_CURRENT_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setSince("8.4")
      .setDescription("Current name of the quality gate")
      .setExampleValue("My Quality Gate");

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("New name of the quality gate")
      .setExampleValue("My New Quality Gate");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    String currentName = request.mandatoryParam(PARAM_CURRENT_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateDto qualityGate;

      qualityGate = wsSupport.getByOrganizationAndName(dbSession, organization, currentName);

      QualityGateDto renamedQualityGate = rename(dbSession, organization, qualityGate, request.mandatoryParam(PARAM_NAME));
      logger.info("Renamed Quality Gate:: organization: {}, renamedTo: {}", organization.getKey(), currentName);
      writeProtobuf(QualityGate.newBuilder()
        .setId(renamedQualityGate.getUuid())
        .setName(renamedQualityGate.getName())
        .build(), request, response);
    }
  }

  private QualityGateDto rename(DbSession dbSession, OrganizationDto organization, QualityGateDto qualityGate, String name) {
    wsSupport.checkCanEdit(qualityGate);
    checkNotAlreadyExists(dbSession, organization, qualityGate, name);
    qualityGate.setName(name);
    dbClient.qualityGateDao().update(qualityGate, dbSession);
    dbSession.commit();
    return qualityGate;
  }

  private void checkNotAlreadyExists(DbSession dbSession, OrganizationDto organization, QualityGateDto qualityGate, String name) {
    QualityGateDto existingQgate = dbClient.qualityGateDao().selectByOrganizationAndName(dbSession, organization, name);
    boolean isModifyingCurrentQgate = existingQgate == null || existingQgate.getUuid().equals(qualityGate.getUuid());
    checkArgument(isModifyingCurrentQgate, "Name '%s' has already been taken", name);
  }

}
