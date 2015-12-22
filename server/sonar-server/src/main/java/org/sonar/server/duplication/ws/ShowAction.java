/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.duplication.ws;

import com.google.common.io.Resources;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.component.ComponentFinder.ParamNames.UUID_AND_KEY;

public class ShowAction implements RequestHandler {

  private final DbClient dbClient;
  private final MeasureDao measureDao;
  private final DuplicationsParser parser;
  private final DuplicationsJsonWriter duplicationsJsonWriter;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ShowAction(DbClient dbClient, MeasureDao measureDao, DuplicationsParser parser,
                    DuplicationsJsonWriter duplicationsJsonWriter, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.measureDao = measureDao;
    this.parser = parser;
    this.duplicationsJsonWriter = duplicationsJsonWriter;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get duplications. Require Browse permission on file's project")
      .setSince("4.4")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-show.json"));

    action
      .createParam("key")
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam("uuid")
      .setDescription("File UUID")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");
  }

  @Override
  public void handle(Request request, Response response) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto component = componentFinder.getByUuidOrKey(dbSession, request.param("uuid"), request.param("key"), UUID_AND_KEY);
      String componentKey = component.key();
      userSession.checkComponentPermission(UserRole.CODEVIEWER, componentKey);
      JsonWriter json = response.newJsonWriter().beginObject();
      String duplications = findDataFromComponent(componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY, dbSession);
      List<DuplicationsParser.Block> blocks = parser.parse(component, duplications, dbSession);
      duplicationsJsonWriter.write(blocks, json, dbSession);
      json.endObject().close();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @CheckForNull
  private String findDataFromComponent(String fileKey, String metricKey, DbSession session) {
    MeasureDto measure = measureDao.selectByComponentKeyAndMetricKey(session, fileKey, metricKey);
    if (measure != null) {
      return measure.getData();
    }
    return null;
  }
}
