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
package org.sonar.server.issue.ws;

import com.google.common.base.MoreObjects;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.Uuids;
import org.sonar.server.issue.IssueService;

import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_SET_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;

/**
 * Set tags on an issue
 */
public class SetTagsAction implements IssuesWsAction {

  private final IssueService service;

  public SetTagsAction(IssueService service) {
    this.service = service;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_SET_TAGS)
      .setHandler(this)
      .setPost(true)
      .setSince("5.1")
      .setDescription("Set tags on an issue. <br/>" +
        "Requires authentication and Browse permission on project<br/>" +
        "Since 6.3, the parameter 'key' has been replaced by '%s'", PARAM_ISSUE);
    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setSince("6.3")
      .setDeprecatedKey("key")
      .setExampleValue(Uuids.UUID_EXAMPLE_01)
      .setRequired(true);
    action.createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags. All tags are removed if parameter is empty or not set.")
      .setExampleValue("security,cwe,misra-c");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam(PARAM_ISSUE);
    List<String> tags = MoreObjects.firstNonNull(request.paramAsStrings(PARAM_TAGS), Collections.<String>emptyList());
    Collection<String> resultTags = service.setTags(key, tags);
    JsonWriter json = response.newJsonWriter().beginObject().name("tags").beginArray();
    for (String tag : resultTags) {
      json.value(tag);
    }
    json.endArray().endObject().close();
  }

}
