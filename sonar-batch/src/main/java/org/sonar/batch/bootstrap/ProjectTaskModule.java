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
package org.sonar.batch.bootstrap;

import org.sonar.api.batch.TaskDefinition;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.DefaultIndex;

public class ProjectTaskModule extends AbstractTaskModule {

  public ProjectTaskModule(TaskDefinition task) {
    super(task);
  }

  @Override
  protected void configure() {
    super.configure();
    registerCoreComponentsRequiringProject();
  }

  private void registerCoreComponentsRequiringProject() {
    container.addSingleton(ProjectExclusions.class);
    container.addSingleton(ProjectReactorReady.class);
    container.addSingleton(ProjectTree.class);
    container.addSingleton(ProjectConfigurator.class);
    container.addSingleton(DefaultIndex.class);
    container.addSingleton(DefaultFileLinesContextFactory.class);
    container.addSingleton(ProjectLock.class);

    container.addSingleton(DryRunDatabase.class);
  }
}
