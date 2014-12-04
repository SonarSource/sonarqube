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
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.source.SourceService;

public class ScmAction implements RequestHandler {

  private final SourceService service;
  private final ScmWriter scmWriter;

  public ScmAction(SourceService service, ScmWriter scmWriter) {
    this.service = service;
    this.scmWriter = scmWriter;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("scm")
      .setDescription("Get SCM information of source files. Require See Source Code permission on file's project<br/>" +
        "Each element of the result array is composed of:" +
        "<ol>" +
        "<li>Line number</li>" +
        "<li>Author of the commit</li>" +
        "<li>Date of the commit</li>" +
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

    String authors = service.getScmAuthorData(fileKey);
    String dates = service.getScmDateData(fileKey);

    JsonWriter json = response.newJsonWriter().beginObject();
    int from = Math.max(request.mandatoryParamAsInt("from"), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt("to"), Integer.MAX_VALUE);
    scmWriter.write(authors, dates, from, to, request.mandatoryParamAsBoolean("commits_by_line"), json);
    json.endObject().close();
  }
}
