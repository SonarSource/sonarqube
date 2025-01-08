/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.projectdump.ws;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.server.ws.WebService;

public class ProjectDumpWs implements WebService {

  public static final String CONTROLLER_PATH = "api/project_dump";

  private final List<ProjectDumpAction> actions;

  public ProjectDumpWs(ProjectDumpAction... actions) {
    this.actions = ImmutableList.copyOf(actions);
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(CONTROLLER_PATH)
      .setDescription("Project export/import")
      .setSince("1.0");
    for (ProjectDumpAction action : actions) {
      action.define(controller);
    }
    controller.done();
  }

}
