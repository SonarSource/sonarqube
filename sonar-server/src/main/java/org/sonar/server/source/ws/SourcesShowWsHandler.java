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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.source.HtmlSourceDecorator;
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;

public class SourcesShowWsHandler implements RequestHandler {

  private final HtmlSourceDecorator sourceDecorator;

  public SourcesShowWsHandler(HtmlSourceDecorator sourceDecorator) {
    this.sourceDecorator = sourceDecorator;
  }

  @Override
  public void handle(Request request, Response response) {
    String componentKey = request.requiredParam("key");
    List<String> sourceHtml = sourceDecorator.getDecoratedSourceAsHtml(componentKey);
    if (sourceHtml == null) {
      throw new NotFoundException("Source code not found for : " + componentKey);
    }
    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("source").beginObject();
    for (int i = 0; i < sourceHtml.size(); i++) {
      String line = sourceHtml.get(i);
      json.prop(Integer.toString(i + 1), line);
    }
    json.endObject().endObject().close();
  }

}
