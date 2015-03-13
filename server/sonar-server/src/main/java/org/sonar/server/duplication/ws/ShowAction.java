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

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import java.util.List;

public class ShowAction implements RequestHandler {

  private final DbClient dbClient;
  private final ComponentDao componentDao;
  private final MeasureDao measureDao;
  private final DuplicationsParser parser;
  private final DuplicationsJsonWriter duplicationsJsonWriter;

  public ShowAction(DbClient dbClient, ComponentDao componentDao, MeasureDao measureDao, DuplicationsParser parser, DuplicationsJsonWriter duplicationsJsonWriter) {
    this.dbClient = dbClient;
    this.componentDao = componentDao;
    this.measureDao = measureDao;
    this.parser = parser;
    this.duplicationsJsonWriter = duplicationsJsonWriter;
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
    String fileKey = request.param("key");
    String fileUuid = request.param("uuid");
    Preconditions.checkArgument(fileKey != null || fileUuid != null, "At least one of 'key' or 'uuid' must be provided");

    DbSession session = dbClient.openSession(false);
    if (fileKey == null) {
      fileKey = componentDao.getByUuid(session, fileUuid).key();
    }

    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);

    try {
      ComponentDto component = findComponent(fileKey, session);
      JsonWriter json = response.newJsonWriter().beginObject();
      String duplications = findDataFromComponent(fileKey, CoreMetrics.DUPLICATIONS_DATA_KEY, session);
      List<DuplicationsParser.Block> blocks = parser.parse(component, duplications, session);
      duplicationsJsonWriter.write(blocks, json, session);
      json.endObject().close();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  private String findDataFromComponent(String fileKey, String metricKey, DbSession session) {
    MeasureDto measure = measureDao.findByComponentKeyAndMetricKey(session, fileKey, metricKey);
    if (measure != null) {
      return measure.getData();
    }
    return null;
  }

  private ComponentDto findComponent(String key, DbSession session) {
    ComponentDto componentDto = componentDao.getNullableByKey(session, key);
    if (componentDto == null) {
      throw new NotFoundException(String.format("Component with key '%s' not found", key));
    }
    return componentDto;
  }
}
