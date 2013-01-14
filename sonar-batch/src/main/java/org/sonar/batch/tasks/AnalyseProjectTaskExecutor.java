/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.tasks;

import org.sonar.api.batch.TaskExecutor;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.AnalyseProjectModule;

public class AnalyseProjectTaskExecutor implements TaskExecutor {

  private final ComponentContainer container;
  private final ProjectTree projectTree;

  public AnalyseProjectTaskExecutor(ProjectTree projectTree, ComponentContainer container) {
    this.container = container;
    this.projectTree = projectTree;
  }

  public void execute() {
    AnalyseProjectModule analyseProjectModule = new AnalyseProjectModule(projectTree.getRootProject());
    try {
      ComponentContainer childContainer = container.createChild();
      analyseProjectModule.init(childContainer);
      analyseProjectModule.start();
    } finally {
      analyseProjectModule.stop();
      container.removeChild();
    }
  }

}
