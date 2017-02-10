/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule.ws;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.rule.RuleDeleter;

public class DeleteAction implements RulesWsAction {

  public static final String PARAM_KEY = "key";

  private final RuleDeleter ruleDeleter;
  private final RuleWsSupport ruleWsSupport;

  public DeleteAction(RuleDeleter ruleDeleter, RuleWsSupport ruleWsSupport) {
    this.ruleDeleter = ruleDeleter;
    this.ruleWsSupport = ruleWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("delete")
      .setDescription("Delete custom rule")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setDescription("Rule key")
      .setRequired(true)
      .setExampleValue("squid:XPath_1402065390816");
  }

  @Override
  public void handle(Request request, Response response) {
    ruleWsSupport.checkQProfileAdminPermission();
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    ruleDeleter.delete(key);
  }
}
