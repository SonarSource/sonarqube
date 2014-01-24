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
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.rule.RuleTags;

public class RuleTagsWs implements WebService {

  private final RuleTags ruleTags;

  public RuleTagsWs(RuleTags ruleTags) {
    this.ruleTags = ruleTags;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/rule_tags")
        .setDescription("Rule tags");

    controller.newAction("list")
      .setDescription("List all available rule tags")
      .setSince("4.2")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          list(request, response);
        }
      });

    controller.newAction("create")
      .setPost(true)
      .setDescription("Create a new rule tag")
      .setSince("4.2")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          create(request, response);
        }
      })
      .newParam("tag").setDescription("Value of the new rule tag");

    controller.done();
  }

  private void list(Request request, Response response) {
    JsonWriter writer = response.newJsonWriter();
    writer.beginArray();
    for (String tag: ruleTags.listAllTags()) {
      writer.value(tag);
    }
    writer.endArray().close();
  }

  private void create(Request request, Response response) {
    RuleTagDto newTag = ruleTags.create(request.requiredParam("tag"));
    response.newJsonWriter()
      .beginObject()
      .prop("id", newTag.getId())
      .prop("tag", newTag.getTag())
      .endObject()
      .close();
  }
}
