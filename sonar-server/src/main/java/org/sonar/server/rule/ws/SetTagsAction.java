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
package org.sonar.server.rule.ws;

import com.google.common.collect.Sets;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.rule.RuleService;

import java.util.Set;

public class SetTagsAction implements RequestHandler {

  private final RuleService service;

  public SetTagsAction(RuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction setTags = controller
      .createAction("set_tags")
      .setDescription("Set the tags of a coding rule")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);
    setTags
      .createParam("key")
      .setRequired(true)
      .setDescription("Rule key")
      .setExampleValue("javascript:EmptyBlock");
    setTags
      .createParam("tags")
      .setDescription("Comma-separated list of tags. Blank value is used to remove all tags.")
      .setRequired(true)
      .setExampleValue("java8,security");
  }

  @Override
  public void handle(Request request, Response response) {
    RuleKey key = RuleKey.parse(request.mandatoryParam("key"));
    Set<String> tags = Sets.newHashSet(request.mandatoryParamAsStrings("tags"));
    service.setTags(key, tags);
  }
}
