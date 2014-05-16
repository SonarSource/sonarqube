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
import org.sonar.server.rule2.RuleService;

public class SetNoteAction implements RequestHandler {

  private final RuleService service;

  public SetNoteAction(RuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction setTags = controller
      .createAction("set_note")
      .setDescription("Extend the description of a coding rule")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);
    setTags
      .createParam("key")
      .setRequired(true)
      .setDescription("Rule key")
      .setExampleValue("javascript:EmptyBlock");
    setTags
      .createParam("text")
      .setDescription("Markdown text. Set to blank to remove the note.")
      .setRequired(true)
      .setExampleValue("java8,security");
  }

  @Override
  public void handle(Request request, Response response) {
    RuleKey key = RuleKey.parse(request.mandatoryParam("key"));
    service.setNote(key, request.mandatoryParam("text"));
  }
}
