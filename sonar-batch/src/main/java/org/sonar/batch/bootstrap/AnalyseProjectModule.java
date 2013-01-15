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

import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.resources.Project;

public class AnalyseProjectModule extends Module {

  private Project rootProject;

  public AnalyseProjectModule(Project rootProject) {
    this.rootProject = rootProject;
  }

  @Override
  protected void configure() {
    container.addSingleton(MetricProvider.class);

    registerPerBatchExtensions();
  }

  private void registerPerBatchExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installBatchExtensions(container, InstantiationStrategy.PER_BATCH);
  }

  /**
   * Analyze project
   */
  @Override
  protected void doStart() {
    analyze(rootProject);
  }

  private void analyze(Project project) {
    for (Project subProject : project.getModules()) {
      analyze(subProject);
    }

    ProjectModule projectModule = new ProjectModule(project);
    try {
      installChild(projectModule);
      projectModule.start();
    } finally {
      projectModule.stop();
      uninstallChild();
    }
  }
}
