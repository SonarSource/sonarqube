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

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.List;

public class ScmAction implements SourcesAction {

  private final DbClient dbClient;
  private final SourceLineIndex sourceLineIndex;

  public ScmAction(DbClient dbClient, SourceLineIndex sourceLineIndex) {
    this.dbClient = dbClient;
    this.sourceLineIndex = sourceLineIndex;
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

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto fileDto = dbClient.componentDao().getByKey(session, fileKey);
      UserSession.get().checkProjectUuidPermission(UserRole.CODEVIEWER, fileDto.projectUuid());
      List<SourceLineDoc> sourceLines = sourceLineIndex.getLines(fileDto.uuid(), from, to);
      if (sourceLines.isEmpty()) {
        throw new NotFoundException("File '" + fileKey + "' has no sources");
      }

      JsonWriter json = response.newJsonWriter().beginObject();
      writeSource(sourceLines, commitsByLine, json);
      json.endObject().close();
    } finally {
      session.close();
    }
  }

  private static void writeSource(List<SourceLineDoc> lines, boolean showCommitsByLine, JsonWriter json) {
    json.name("scm").beginArray();

    SourceLineDoc previousLine = null;
    boolean started = false;
    for (SourceLineDoc lineDoc : lines) {
      if (hasScm(lineDoc) && (!started || showCommitsByLine || !isSameCommit(previousLine, lineDoc))) {
        json.beginArray()
          .value(lineDoc.line())
          .value(lineDoc.scmAuthor());
        Date date = lineDoc.scmDate();
        json.value(date == null ? null : DateUtils.formatDateTime(date));
        json.value(lineDoc.scmRevision());
        json.endArray();
        started = true;
      }
      previousLine = lineDoc;
    }
    json.endArray();
  }

  private static boolean isSameCommit(SourceLineDoc previousLine, SourceLineDoc currentLine) {
    return new EqualsBuilder()
      .append(previousLine.scmAuthor(), currentLine.scmAuthor())
      .append(previousLine.scmDate(), currentLine.scmDate())
      .append(previousLine.scmRevision(), currentLine.scmRevision())
      .isEquals();
  }

  private static boolean hasScm(SourceLineDoc line){
    return !Strings.isNullOrEmpty(line.scmAuthor()) || line.scmDate() != null || !Strings.isNullOrEmpty(line.scmRevision());
  }
}
