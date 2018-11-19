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

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.qualitygate.QualityGates;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;

public class DeselectAction implements QualityGatesWsAction {

  private final QualityGates qualityGates;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public DeselectAction(QualityGates qualityGates, DbClient dbClient, ComponentFinder componentFinder) {
    this.qualityGates = qualityGates;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("deselect")
      .setDescription("Remove the association of a project from a quality gate.<br>" +
        "Requires the 'Administer Quality Gates' permission.")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this)
      .setChangelog(new Change("6.6", "The parameter 'gateId' was removed"));

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setDeprecatedSince("6.1")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.1");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = getProject(dbSession, request.param(PARAM_PROJECT_ID), request.param(PARAM_PROJECT_KEY));
      qualityGates.dissociateProject(dbSession, project);
      response.noContent();
    }
  }

  private ComponentDto getProject(DbSession dbSession, @Nullable String projectId, @Nullable String projectKey) {
    return selectProjectById(dbSession, projectId)
        .or(() -> componentFinder.getByUuidOrKey(dbSession, projectId, projectKey, ComponentFinder.ParamNames.PROJECT_ID_AND_KEY));
  }

  private Optional<ComponentDto> selectProjectById(DbSession dbSession, @Nullable String projectId) {
    if (projectId == null) {
      return Optional.absent();
    }

    try {
      long dbId = Long.parseLong(projectId);
      return dbClient.componentDao().selectById(dbSession, dbId);
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }

}
