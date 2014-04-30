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
package org.sonar.server.issue.ws;

import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class IssuesWs implements WebService {

  private final IssueShowAction showHandler;

  public IssuesWs(IssueShowAction showHandler) {
    this.showHandler = showHandler;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/issues");
    controller.setDescription("Coding rule issues");
    controller.setSince("3.6");
    showHandler.define(controller);

    WebService.NewAction search = controller.createAction("search")
      .setDescription("Get a list of issues. If the number of issues is greater than 10,000, only the first 10,000 ones are returned by the web service. Requires Browse permission on project(s).")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE);
    search.createParam("issues")
      .setDescription("Comma-separated list of issue keys.")
      .setExampleValue("5bccd6e8-f525-43a2-8d76-fcb13dde79ef");
    search.createParam("severities")
      .setDescription("Comma-separated list of severities.")
      .setExampleValue("BLOCKER,CRITICAL")
      .setPossibleValues(Severity.ALL.toArray(new String[Severity.ALL.size()]));

    controller.done();
  }
}
