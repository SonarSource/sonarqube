/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.duplication.ws;

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureQuery;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.component.ComponentFinder.ParamNames.UUID_AND_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ShowAction implements DuplicationsWsAction {

  private final DbClient dbClient;
  private final DuplicationsParser parser;
  private final ShowResponseBuilder responseBuilder;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ShowAction(DbClient dbClient, DuplicationsParser parser, ShowResponseBuilder responseBuilder, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.parser = parser;
    this.responseBuilder = responseBuilder;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get duplications. Require Browse permission on file's project")
      .setSince("4.4")
      .setHandler(this)
      .setResponseExample(getClass().getResource("show-example.json"));

    action.setChangelog(
      new Change("6.5", "The fields 'uuid', 'projectUuid', 'subProjectUuid' are deprecated in the response."));

    action
      .createParam("key")
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam("uuid")
      .setDeprecatedSince("6.5")
      .setDescription("File ID. If provided, 'key' must not be provided.")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByUuidOrKey(dbSession, request.param("uuid"), request.param("key"), UUID_AND_KEY);
      userSession.checkComponentPermission(UserRole.CODEVIEWER, component);
      String duplications = findDataFromComponent(dbSession, component);
      List<DuplicationsParser.Block> blocks = parser.parse(component, duplications, dbSession);

      writeProtobuf(responseBuilder.build(blocks, dbSession), request, response);
    }
  }

  @CheckForNull
  private String findDataFromComponent(DbSession dbSession, ComponentDto component) {
    MeasureQuery query = MeasureQuery.builder()
      .setComponentUuid(component.uuid())
      .setMetricKey(CoreMetrics.DUPLICATIONS_DATA_KEY)
      .build();
    return dbClient.measureDao().selectSingle(dbSession, query)
      .map(MeasureDto::getData)
      .orElse(null);
  }
}
