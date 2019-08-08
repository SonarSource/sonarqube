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
package org.sonar.server.branch.ws;

import java.util.Arrays;
import org.sonar.api.server.ws.WebService;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.CONTROLLER;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;

public class BranchesWs implements WebService {
  private final BranchWsAction[] actions;

  public BranchesWs(BranchWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(CONTROLLER)
      .setSince("6.6")
      .setDescription("Manage branch (only available when the Branch plugin is installed)");
    Arrays.stream(actions).forEach(action -> action.define(controller));
    controller.done();
  }

  static void addProjectParam(NewAction action) {
    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);
  }

  static void addBranchParam(NewAction action) {
    action
      .createParam(PARAM_BRANCH)
      .setDescription("Name of the branch")
      .setExampleValue("branch1")
      .setRequired(true);
  }

}
