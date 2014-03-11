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

import org.sonar.api.server.ws.WebService;

public class RulesWs implements WebService {

  private final RuleSearchWsHandler searchHandler;
  private final RuleShowWsHandler showHandler;
  private final AddTagsWsHandler addTagsWsHandler;
  private final RemoveTagsWsHandler removeTagsWsHandler;

  public RulesWs(RuleSearchWsHandler searchHandler, RuleShowWsHandler showHandler, AddTagsWsHandler addTagsWsHandler, RemoveTagsWsHandler removeTagsWsHandler) {
    this.searchHandler = searchHandler;
    this.showHandler = showHandler;
    this.addTagsWsHandler = addTagsWsHandler;
    this.removeTagsWsHandler = removeTagsWsHandler;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/rules")
      .setDescription("Coding rules");

    controller.newAction("list")
      .setDescription("List rules that match the given criteria")
      .setSince("4.3")
      .setHandler(searchHandler)
      .newParam("s", "An optional query that will be matched against rule titles.")
      .newParam("k", "An optional query that will be matched exactly agains rule keys.")
      .newParam("ps", "Optional page size (default is 25).")
      .newParam("p", "Optional page number (default is 0).");

    controller.newAction("show")
      .setDescription("Detail of rule")
      .setSince("4.2")
      .setHandler(showHandler)
      .newParam("key", "Mandatory key of rule");

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

  private void addTagParams(final NewAction action) {
    action.newParam("key", "Full key of the rule");
    action.newParam("tags", "Comma separated list of tags");
  }
}
