/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class SourcesShowWsHandler implements RequestHandler {

  private final SourceService sourceService;

  public SourcesShowWsHandler(SourceService sourceService) {
    this.sourceService = sourceService;
  }

  @Override
  public void handle(Request request, Response response) {
    String componentKey = request.requiredParam("key");
    Integer fromParam = request.intParam("from");
    Integer toParam = request.intParam("to");
    List<String> sourceHtml = sourceService.sourcesFromComponent(componentKey, fromParam, toParam);
    if (sourceHtml == null) {
      throw new NotFoundException("Component : " + componentKey + " has no source.");
    }

    String scmAuthorData = sourceService.findDataFromComponent(componentKey, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
    String scmDataData = sourceService.findDataFromComponent(componentKey, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);

    int from = fromParam != null ? fromParam : 1;
    int to = toParam != null ? toParam : sourceHtml.size() + from;
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    writeSource(sourceHtml, from, json);
    writeScm(scmAuthorData, scmDataData, from, to, json);
    json.endObject().close();
  }

  private void writeSource(List<String> source, int from, JsonWriter json) {
    json.name("source").beginObject();
    for (int i = 0; i < source.size(); i++) {
      String line = source.get(i);
      json.prop(Integer.toString(i + from), line);
    }
    json.endObject();
  }

  private void writeScm(String authorData, String scmDateData, int from, int to , JsonWriter json) {
    if (authorData != null) {
      json.name("scm").beginObject();
      List<String> authors = splitLine(authorData);
      List<String> dates = splitLine(scmDateData);

      String previousAuthor = null;
      String previousDate = null;
      for (int i = 0; i < authors.size(); i++) {
        String[] authorWithLine = splitColumn(authors.get(i));
        Integer line = Integer.parseInt(authorWithLine[0]);
        String author = authorWithLine[1];

        String[] dateWithLine = splitColumn(dates.get(i));
        String date = dateWithLine[1];
        String formattedDate = DateUtils.formatDate(DateUtils.parseDateTime(date));
        if (!author.equals(previousAuthor) && !date.equals(previousDate) &&
          line >= from && line <= to) {
          json.name(Integer.toString(line)).beginArray();
          json.value(author);
          json.value(formattedDate);
          json.endArray();
        }

        previousAuthor = author;
        previousDate = date;
      }
      json.endObject();
    }
  }

  private List<String> splitLine(String line){
    return newArrayList(Splitter.on(";").omitEmptyStrings().split(line));
  }

  private String[] splitColumn(String column){
    return column.split("=");
  }

}
