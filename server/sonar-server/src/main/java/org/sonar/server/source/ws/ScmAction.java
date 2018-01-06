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
package org.sonar.server.source.ws;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.util.Date;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
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
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class ScmAction implements SourcesWsAction {

  private final DbClient dbClient;
  private final SourceService sourceService;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ScmAction(DbClient dbClient, SourceService sourceService, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.sourceService = sourceService;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("scm")
      .setDescription("Get SCM information of source files. Require See Source Code permission on file's project<br/>" +
        "Each element of the result array is composed of:" +
        "<ol>" +
        "<li>Line number</li>" +
        "<li>Author of the commit</li>" +
        "<li>Datetime of the commit (before 5.2 it was only the Date)</li>" +
        "<li>Revision of the commit (added in 5.2)</li>" +
        "</ol>")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "example-scm.json"))
      .setHandler(this);

    action
      .createParam("key")
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam("from")
      .setDescription("First line to return. Starts at 1")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam("to")
      .setDescription("Last line to return (inclusive)")
      .setExampleValue("20");

    action
      .createParam("commits_by_line")
      .setDescription("Group lines by SCM commit if value is false, else display commits for each line, even if two " +
        "consecutive lines relate to the same commit.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam("key");
    int from = Math.max(request.mandatoryParamAsInt("from"), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt("to"), Integer.MAX_VALUE);
    boolean commitsByLine = request.mandatoryParamAsBoolean("commits_by_line");

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto file = componentFinder.getByKey(dbSession, fileKey);
      userSession.checkComponentPermission(UserRole.CODEVIEWER, file);
      Iterable<DbFileSources.Line> sourceLines = checkFoundWithOptional(sourceService.getLines(dbSession, file.uuid(), from, to), "File '%s' has no sources", fileKey);
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        writeSource(sourceLines, commitsByLine, json);
        json.endObject().close();
      }
    }
  }

  private static void writeSource(Iterable<DbFileSources.Line> lines, boolean showCommitsByLine, JsonWriter json) {
    json.name("scm").beginArray();

    DbFileSources.Line previousLine = null;
    boolean started = false;
    for (DbFileSources.Line lineDoc : lines) {
      if (hasScm(lineDoc) && (!started || showCommitsByLine || !isSameCommit(previousLine, lineDoc))) {
        json.beginArray()
          .value(lineDoc.getLine())
          .value(lineDoc.getScmAuthor());
        json.value(lineDoc.hasScmDate() ? DateUtils.formatDateTime(new Date(lineDoc.getScmDate())) : null);
        json.value(lineDoc.getScmRevision());
        json.endArray();
        started = true;
      }
      previousLine = lineDoc;
    }
    json.endArray();
  }

  private static boolean isSameCommit(DbFileSources.Line previousLine, DbFileSources.Line currentLine) {
    return new EqualsBuilder()
      .append(previousLine.getScmAuthor(), currentLine.getScmAuthor())
      .append(previousLine.getScmDate(), currentLine.getScmDate())
      .append(previousLine.getScmRevision(), currentLine.getScmRevision())
      .isEquals();
  }

  private static boolean hasScm(DbFileSources.Line line) {
    return !Strings.isNullOrEmpty(line.getScmAuthor()) || line.hasScmDate() || !Strings.isNullOrEmpty(line.getScmRevision());
  }
}
