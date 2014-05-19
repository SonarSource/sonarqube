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
package org.sonar.server.rule2.ws;

import com.google.common.io.Resources;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule2.Rule;
import org.sonar.server.rule2.RuleParam;
import org.sonar.server.rule2.RuleService;

/**
 * @since 4.4
 */
public class ShowAction implements RequestHandler {

  private final RuleService service;

  public ShowAction(RuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setDescription("Get detailed information about a rule")
      .setSince("4.2")
      .setResponseExample(Resources.getResource(getClass(), "example-show.json"))
      .setHandler(this);

    action
      .createParam("key")
      .setDescription("Rule key")
      .setRequired(true)
      .setExampleValue("javascript:EmptyBlock");
  }

  @Override
  public void handle(Request request, Response response) {
    Rule rule = service.getByKey(RuleKey.parse(request.mandatoryParam("key")));
    if (rule == null) {
      throw new NotFoundException("Rule not found");
    }
    JsonWriter json = response.newJsonWriter().beginObject().name("rule").beginObject();
    writeRule(rule, json);
    json.endObject().endObject().close();
  }

  private void writeRule(Rule rule, JsonWriter json) {
    json
      .prop("key", rule.key().toString())
      .prop("repo", rule.key().repository())
      .prop("lang", rule.language())
      .prop("name", rule.name())
      .prop("htmlDesc", rule.htmlDescription())
      .prop("status", rule.status().toString())
      .prop("template", rule.template())
      .prop("internalKey", rule.internalKey())
      .prop("severity", rule.severity().toString())
      .prop("markdownNote", rule.markdownNote())
      .prop("noteLogin", rule.noteLogin())
      .name("tags").beginArray().values(rule.tags()).endArray()
      .name("sysTags").beginArray().values(rule.systemTags()).endArray();
    json.name("params").beginArray();
    for (RuleParam param : rule.params()) {
      json
        .beginObject()
        .prop("key", param.key())
        .prop("desc", param.description())
        .prop("defaultValue", param.defaultValue())
        .endObject();
    }
    json.endArray();
  }
}
