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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule2.Rule;
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
      .setSince("4.4")
      .setHandler(this);

    action
      .createParam("repo")
      .setDescription("Repository key")
      .setRequired(true)
      .setExampleValue("javascript");

    action
      .createParam("key")
      .setDescription("Rule key")
      .setRequired(true)
      .setExampleValue("EmptyBlock");
  }

  @Override
  public void handle(Request request, Response response) {
    String repoKey = request.mandatoryParam("repo");
    String ruleKey = request.mandatoryParam("key");
    Rule rule = service.getByKey(RuleKey.of(repoKey, ruleKey));
    if (rule == null) {
      throw new NotFoundException("Rule not found");
    }
    JsonWriter json = response.newJsonWriter().beginObject().name("rule").beginObject();
    writeRule(rule, json);
    json.endObject().endObject().close();
  }

  private void writeRule(Rule rule, JsonWriter json) {
    json.prop("repo", rule.key().repository());
    json.prop("key", rule.key().rule());
    json.prop("lang", rule.language());
    json.prop("name", rule.name());
    json.prop("desc", rule.description());
    json.prop("status", rule.status().toString());
    json.prop("template", rule.template());
    json.prop("severity", rule.severity().toString());
    json.name("tags").beginArray().values(rule.tags()).endArray();
    json.name("sysTags").beginArray().values(rule.systemTags()).endArray();
    //TODO debt, params
  }
}
