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

import com.google.common.base.Splitter;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ShowAction implements RequestHandler {

  private final SourceService sourceService;

  public ShowAction(SourceService sourceService) {
    this.sourceService = sourceService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get source code. Parameter 'output' with value 'raw' is missing before being marked as a public WS.")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam("key")
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam("from")
      .setDescription("First line to return. Starts at 1.")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam("to")
      .setDescription("Last line to return (inclusive)")
      .setExampleValue("20");

    action
      .createParam("scm")
      .setDescription("Enable loading of SCM information per line")
      .setPossibleValues("true", "false")
      .setDefaultValue("false");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam("key");
    int from = Math.max(request.paramAsInt("from", 1), 1);

    Integer toParam = request.paramAsInt("to");

    List<String> sourceHtml = sourceService.getLinesAsHtml(fileKey, from, toParam);
    if (sourceHtml.isEmpty()) {
      throw new NotFoundException("File '" + fileKey + "' has no sources");
    }

    int to = toParam != null ? toParam : sourceHtml.size() + from;
    JsonWriter json = response.newJsonWriter().beginObject();
    writeSource(sourceHtml, from, json);

    if (request.paramAsBoolean("scm", false)) {
      String scmAuthorData = sourceService.getScmAuthorData(fileKey);
      String scmDataData = sourceService.getScmDateData(fileKey);
      writeScm(scmAuthorData, scmDataData, from, to, json);
    }
    json.endObject().close();
  }

  private void writeSource(List<String> lines, int from, JsonWriter json) {
    json.name("source").beginObject();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      json.prop(Integer.toString(i + from), line);
    }
    json.endObject();
  }

  private void writeScm(@Nullable String authorData, @Nullable String scmDateData, int from, int to, JsonWriter json) {
    if (authorData != null) {
      json.name("scm").beginObject();
      List<String> authors = splitLine(authorData);
      List<String> dates = splitLine(scmDateData);

      String previousAuthor = null;
      String previousDate = null;
      boolean scmDataAdded = false;
      for (int i = 0; i < authors.size(); i++) {
        String[] authorWithLine = splitColumn(authors.get(i));
        Integer line = Integer.parseInt(authorWithLine[0]);
        String author = authorWithLine[1];

        String[] dateWithLine = splitColumn(dates.get(i));
        String date = dateWithLine[1];
        String formattedDate = DateUtils.formatDate(DateUtils.parseDateTime(date));
        if (line >= from && line <= to && (!isSameCommit(date, previousDate, author, previousAuthor) || !scmDataAdded)) {
          json.name(Integer.toString(line)).beginArray();
          json.value(author);
          json.value(formattedDate);
          json.endArray();
          scmDataAdded = true;
        }
        previousAuthor = author;
        previousDate = date;
      }
      json.endObject();
    }
  }

  private boolean isSameCommit(String date, String previousDate, String author, String previousAuthor) {
    return author.equals(previousAuthor) && date.equals(previousDate);
  }

  private List<String> splitLine(@Nullable String line) {
    if (line == null) {
      return Collections.emptyList();
    }
    return newArrayList(Splitter.on(";").omitEmptyStrings().split(line));
  }

  private String[] splitColumn(String column) {
    return column.split("=");
  }
}
