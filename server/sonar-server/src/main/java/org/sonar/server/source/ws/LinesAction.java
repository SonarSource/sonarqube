/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.source.ws;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import java.util.Date;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import static org.sonar.server.component.ComponentFinder.ParamNames.UUID_AND_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_001;

public class LinesAction implements SourcesWsAction {

  private static final String PARAM_UUID = "uuid";
  private static final String PARAM_KEY = "key";
  private static final String PARAM_FROM = "from";
  private static final String PARAM_TO = "to";

  private final ComponentFinder componentFinder;
  private final SourceService sourceService;
  private final HtmlSourceDecorator htmlSourceDecorator;
  private final DbClient dbClient;
  private final UserSession userSession;

  public LinesAction(ComponentFinder componentFinder, DbClient dbClient, SourceService sourceService,
                     HtmlSourceDecorator htmlSourceDecorator, UserSession userSession) {
    this.componentFinder = componentFinder;
    this.sourceService = sourceService;
    this.htmlSourceDecorator = htmlSourceDecorator;
    this.dbClient = dbClient;
    this.userSession = userSession;
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
      .setExampleValue(KEY_FILE_EXAMPLE_001);

    action
      .createParam(PARAM_FROM)
      .setDescription("First line to return. Starts from 1")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam(PARAM_TO)
      .setDescription("Optional last line to return (inclusive). It must be greater than " +
        "or equal to parameter 'from'. If unset, then all the lines greater than or equal to 'from' " +
        "are returned.")
      .setExampleValue("20");
  }

  @Override
  public void handle(Request request, Response response) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto file = componentFinder.getByUuidOrKey(dbSession, request.param(PARAM_UUID), request.param(PARAM_KEY), UUID_AND_KEY);
      userSession.checkComponentUuidPermission(UserRole.CODEVIEWER, file.projectUuid());

      int from = request.mandatoryParamAsInt(PARAM_FROM);
      int to = Objects.firstNonNull(request.paramAsInt(PARAM_TO), Integer.MAX_VALUE);

      Optional<Iterable<DbFileSources.Line>> lines = sourceService.getLines(dbSession, file.uuid(), from, to);
      if (!lines.isPresent()) {
        throw new NotFoundException();
      }

      JsonWriter json = response.newJsonWriter().beginObject();
      writeSource(lines.get(), json);
      json.endObject().close();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void writeSource(Iterable<DbFileSources.Line> lines, JsonWriter json) {
    json.name("sources").beginArray();
    for (DbFileSources.Line line : lines) {
      json.beginObject()
        .prop("line", line.getLine())
        .prop("code", htmlSourceDecorator.getDecoratedSourceAsHtml(line.getSource(), line.getHighlighting(), line.getSymbols()))
        .prop("scmAuthor", line.getScmAuthor())
        .prop("scmRevision", line.getScmRevision());
      if (line.hasScmDate()) {
        json.prop("scmDate", DateUtils.formatDateTime(new Date(line.getScmDate())));
      }
      if (line.hasUtLineHits()) {
        json.prop("utLineHits", line.getUtLineHits());
      }
      if (line.hasUtConditions()) {
        json.prop("utConditions", line.getUtConditions());
      }
      if (line.hasUtCoveredConditions()) {
        json.prop("utCoveredConditions", line.getUtCoveredConditions());
      }
      if (line.hasItLineHits()) {
        json.prop("itLineHits", line.getItLineHits());
      }
      if (line.hasItConditions()) {
        json.prop("itConditions", line.getItConditions());
      }
      if (line.hasItCoveredConditions()) {
        json.prop("itCoveredConditions", line.getItCoveredConditions());
      }
      json.prop("duplicated", line.getDuplicationCount() > 0);
      json.endObject();
    }
    json.endArray();
  }

}
