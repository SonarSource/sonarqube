/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.branch.pr.ws;

import org.sonar.api.server.ws.WebService;

import static java.util.Arrays.stream;
import static org.sonar.server.branch.pr.ws.PullRequestsWsParameters.PARAM_PROJECT;
import static org.sonar.server.branch.pr.ws.PullRequestsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class PullRequestsWs implements WebService {
  private final PullRequestWsAction[] actions;

  public PullRequestsWs(PullRequestWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/project_pull_requests")
      .setSince("7.1")
      .setDescription("Manage pull request (only available when the Branch plugin is installed)");
    stream(actions).forEach(action -> action.define(controller));
    controller.done();
  }

  static void addProjectParam(NewAction action) {
    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);
  }

  static void addPullRequestParam(NewAction action) {
    action
      .createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue("1543")
      .setRequired(true);
  }

}
