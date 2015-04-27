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
package org.sonar.server.source.ws;

import com.google.common.io.Resources;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.List;

public class LinesAction implements SourcesAction {

  private static final String PARAM_UUID = "uuid";
  private static final String PARAM_KEY = "key";

  private final SourceLineIndex sourceLineIndex;
  private final HtmlSourceDecorator htmlSourceDecorator;
  private final DbClient dbClient;

  public LinesAction(DbClient dbClient, SourceLineIndex sourceLineIndex, HtmlSourceDecorator htmlSourceDecorator) {
    this.sourceLineIndex = sourceLineIndex;
    this.htmlSourceDecorator = htmlSourceDecorator;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("lines")
      .setDescription("Show source code with line oriented info. Require See Source Code permission on file's project<br/>" +
        "Each element of the result array is an object which contains:" +
        "<ol>" +
        "<li>Line number</li>" +
        "<li>Content of the line</li>" +
        "<li>Author of the line (from SCM information)</li>" +
        "<li>Revision of the line (from SCM information)</li>" +
        "<li>Last commit date of the line (from SCM information)</li>" +
        "<li>Line hits from unit test coverage</li>" +
        "<li>Number of conditions to cover in unit tests</li>" +
        "<li>Number of conditions covered by unit tests</li>" +
        "<li>Line hits from integration test coverage</li>" +
        "<li>Number of conditions to cover in integration tests</li>" +
        "<li>Number of conditions covered by integration tests</li>" +
        "</ol>")
      .setSince("5.0")
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "example-lines.json"))
      .setHandler(this);

    action
      .createParam(PARAM_UUID)
      .setDescription("File uuid. Mandatory if param 'key' is not set")
      .setExampleValue("f333aab4-7e3a-4d70-87e1-f4c491f05e5c");

    action
      .createParam(PARAM_KEY)
      .setDescription("File key. Mandatory if param 'uuid' is not set. Available since 5.2")
      .setExampleValue("org.sample:src/main/java/Foo.java");

    action
      .createParam("from")
      .setDescription("First line to return. Starts at 1")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam("to")
      .setDescription("Last line to return (inclusive)")
      .setExampleValue("20");
  }

  @Override
  public void handle(Request request, Response response) {
    ComponentDto component = loadComponent(request);
    UserSession.get().checkProjectUuidPermission(UserRole.CODEVIEWER, component.projectUuid());

    int from = Math.max(request.mandatoryParamAsInt("from"), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt("to"), Integer.MAX_VALUE);

    List<SourceLineDoc> sourceLines = sourceLineIndex.getLines(component.uuid(), from, to);
    if (sourceLines.isEmpty()) {
      throw new NotFoundException("File '" + component.key() + "' has no sources");
    }

    JsonWriter json = response.newJsonWriter().beginObject();
    writeSource(sourceLines, json);

    json.endObject().close();
  }

  private void writeSource(List<SourceLineDoc> lines, JsonWriter json) {
    json.name("sources").beginArray();
    for (SourceLineDoc line : lines) {
      json.beginObject()
        .prop("line", line.line())
        .prop("code", htmlSourceDecorator.getDecoratedSourceAsHtml(line.source(), line.highlighting(), line.symbols()))
        .prop("scmAuthor", line.scmAuthor())
        .prop("scmRevision", line.scmRevision());
      Date scmDate = line.scmDate();
      json.prop("scmDate", scmDate == null ? null : DateUtils.formatDateTime(scmDate));
      json.prop("utLineHits", line.utLineHits())
        .prop("utConditions", line.utConditions())
        .prop("utCoveredConditions", line.utCoveredConditions())
        .prop("itLineHits", line.itLineHits())
        .prop("itConditions", line.itConditions())
        .prop("itCoveredConditions", line.itCoveredConditions());
      if (!line.duplications().isEmpty()) {
        json.prop("duplicated", true);
      }
      json.endObject();
    }
    json.endArray();
  }

  private ComponentDto loadComponent(Request request) {
    DbSession session = dbClient.openSession(false);
    try {
      String fileUuid = request.param(PARAM_UUID);
      if (fileUuid != null) {
        return dbClient.componentDao().getByUuid(session, fileUuid);
      }
      String fileKey = request.param(PARAM_KEY);
      if (fileKey != null) {
        return dbClient.componentDao().getByKey(session, fileKey);
      }
      throw new IllegalArgumentException(String.format("Param %s or param %s is missing", PARAM_UUID, PARAM_KEY));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
