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

import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.rule2.RuleQuery;
import org.sonar.server.rule2.RuleService;

/**
 * @since 4.4
 */
public class SearchAction implements RequestHandler {

  private final RuleService service;

  public SearchAction(RuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("search")
      .setDescription("Search for a collection of relevant rules matching a specified query")
      .setSince("4.4")
      .setHandler(this);

    action
      .createParam("q")
      .setDescription("UTF-8 search query")
      .setExampleValue("null pointer");

    action
      .createParam("repositories")
      .setDescription("Comma-separated list of repositories")
      .setExampleValue("checkstyle,findbugs");

    action
      .createParam("severities")
      .setDescription("Comma-separated list of default severities. Not the same than severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

    action
      .createParam("statuses")
      .setDescription("Comma-separated list of status codes")
      .setPossibleValues(RuleStatus.values())
      .setExampleValue("BETA,DEPRECATED");

    action
      .createParam("tags")
      .setDescription("Comma-separated list of tags")
      .setExampleValue("security,java8");

    action
      .createParam("qProfile")
      .setDescription("Key of Quality profile")
      .setExampleValue("java:Sonar way");

    action
      .createParam("activation")
      .setDescription("Used only if 'qProfile' is set. Possible values are: true | false | all")
      .setExampleValue("java:Sonar way");
  }

  @Override
  public void handle(Request request, Response response) {
    RuleQuery query = service.newRuleQuery();
    query.setQueryText(request.param("q"));
    query.setSeverities(request.paramAsStrings("severities"));
    query.setRepositories(request.paramAsStrings("repositories"));

    service.search(query);
  }
}
