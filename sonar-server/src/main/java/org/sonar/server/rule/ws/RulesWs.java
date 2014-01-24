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
package org.sonar.server.rule.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

public class RulesWs implements WebService {

  private final RuleShowWsHandler showHandler;
  private final AddTagsWsHandler addTagsWsHandler;
  private final RemoveTagsWsHandler removeTagsWsHandler;

  public RulesWs(RuleShowWsHandler showHandler, AddTagsWsHandler addTagsWsHandler, RemoveTagsWsHandler removeTagsWsHandler) {
    this.showHandler = showHandler;
    this.addTagsWsHandler = addTagsWsHandler;
    this.removeTagsWsHandler = removeTagsWsHandler;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/rules")
      .setDescription("Coding rules");

    controller.newAction("list")
      .setDescription("List coding rules")
      .setSince("4.2")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          list(request, response);
        }
      });

    controller.newAction("show")
      .setDescription("Detail of rule")
      .setSince("4.2")
      .setHandler(showHandler);

    addTagParams(controller.newAction("add_tags")
      .setDescription("Add tags to a rule")
      .setSince("4.2")
      .setPost(true)
      .setHandler(addTagsWsHandler));

    addTagParams(controller.newAction("remove_tags")
      .setDescription("Remove tags from a rule")
      .setSince("4.2")
      .setPost(true)
      .setHandler(removeTagsWsHandler));

    controller.done();
  }

  void list(Request request, Response response) {
    response.newJsonWriter().beginObject()
      .prop("TODO", true)
      .endObject()
      .close();
  }

  private void addTagParams(final NewAction action) {
    action.newParam("key", "Full key of the rule");
    action.newParam("tags", "Comma separated list of tags");
  }
}
