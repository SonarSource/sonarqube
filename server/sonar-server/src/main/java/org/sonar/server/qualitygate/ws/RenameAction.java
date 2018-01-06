/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.server.qualitygate.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_NAME;
import static org.sonar.server.util.Validation.CANT_BE_EMPTY_MESSAGE;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class RenameAction implements QualityGatesWsAction {

  private final DbClient dbClient;
  private final QualityGateFinder qualityGateFinder;
  private final QualityGatesWsSupport wsSupport;

  public RenameAction(DbClient dbClient, QualityGateFinder qualityGateFinder, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.qualityGateFinder = qualityGateFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("rename")
      .setPost(true)
      .setDescription("Rename a Quality Gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("ID of the quality gate to rename")
      .setExampleValue("1");

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("New name of the quality gate")
      .setExampleValue("My Quality Gate");

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    long id = QualityGatesWs.parseId(request, PARAM_ID);
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QualityGateDto qualityGate = rename(dbSession, organization, id, request.mandatoryParam(PARAM_NAME));
      writeProtobuf(QualityGate.newBuilder()
        .setId(qualityGate.getId())
        .setName(qualityGate.getName())
        .build(), request, response);
    }
  }

  private QualityGateDto rename(DbSession dbSession, OrganizationDto organization, long id, String name) {
    QGateWithOrgDto qualityGate = qualityGateFinder.getByOrganizationAndId(dbSession, organization, id);
    wsSupport.checkCanEdit(qualityGate);
    checkArgument(!isNullOrEmpty(name), CANT_BE_EMPTY_MESSAGE, "Name");
    checkNotAlreadyExists(dbSession, organization, qualityGate, name);
    qualityGate.setName(name);
    dbClient.qualityGateDao().update(qualityGate, dbSession);
    dbSession.commit();
    return qualityGate;
  }

  private void checkNotAlreadyExists(DbSession dbSession, OrganizationDto organization, QualityGateDto qualityGate, String name) {
    QualityGateDto existingQgate = dbClient.qualityGateDao().selectByOrganizationAndName(dbSession, organization, name);
    boolean isModifyingCurrentQgate = existingQgate == null || existingQgate.getId().equals(qualityGate.getId());
    checkArgument(isModifyingCurrentQgate, "Name '%s' has already been taken", name);
  }

}
