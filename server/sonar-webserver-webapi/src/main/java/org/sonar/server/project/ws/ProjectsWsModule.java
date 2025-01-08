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
package org.sonar.server.project.ws;

import org.sonar.core.platform.Module;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.ProjectLifeCycleListenersImpl;

public class ProjectsWsModule extends Module {

  public ProjectsWsModule() {
    // nothing to do
  }

  @Override
  protected void configureModule() {
    add(
      ImportProjectService.class,
      ProjectDefaultVisibility.class,
      ProjectFinder.class,
      ProjectLifeCycleListenersImpl.class,
      ProjectsWs.class,
      ProjectCreator.class,
      CreateAction.class,
      BulkDeleteAction.class,
      DeleteAction.class,
      UpdateKeyAction.class,
      SearchMyProjectsAction.class,
      SearchAction.class,
      SearchMyScannableProjectsAction.class,
      UpdateVisibilityAction.class,
      UpdateDefaultVisibilityAction.class);
  }
}
