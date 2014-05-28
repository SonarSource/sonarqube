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
import org.sonar.server.rule.RuleUpdate;

import java.util.List;
import java.util.Set;

public class UpdateAction implements RequestHandler {

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("update")
      .setDescription("Update an existing rule")
      .setSince("4.4")
      .setHandler(this);

    action.createParam("rule_key")
      .setRequired(true)
      .setDescription("Key of the rule to update")
      .setExampleValue("javascript:NullCheck");

    action.createParam("tags")
      .setDescription("Optional comma-separated list of tags to set. Use empty value to remove all current tags.")
      .setExampleValue("java8,security");

    action.createParam("markdown_note")
      .setDescription("Optional note in markdown format. Use empty value to remove current note.")
      .setExampleValue("my *note*");

    action.createParam("debt_sub_characteristic")
      .setDescription("Optional key of the new sub-characteristic to set. Use empty value to unset (-> none) or '" +
        RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC + "' to revert to default sub-characteristic .")
      .setExampleValue("FAULT_TOLERANCE");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
  }
}
