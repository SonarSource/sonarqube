/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Issues;

import static java.util.Collections.singletonList;
import static org.sonar.server.issue.ws.SearchAdditionalField.ALL_ADDITIONAL_FIELDS;

public class OperationResponseWriter {

  private final SearchResponseLoader loader;
  private final SearchResponseFormat format;

  public OperationResponseWriter(SearchResponseLoader loader, SearchResponseFormat format) {
    this.loader = loader;
    this.format = format;
  }

  public void write(String issueKey, SearchResponseData preloadedResponseData, Request request, Response response) {
    SearchResponseLoader.Collector collector = new SearchResponseLoader.Collector(singletonList(issueKey));
    SearchResponseData data = loader.load(preloadedResponseData, collector, ALL_ADDITIONAL_FIELDS,null);

    Issues.Operation responseBody = format.formatOperation(data);

    WsUtils.writeProtobuf(responseBody, request, response);
  }
}
