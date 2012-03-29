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
package org.sonar.plugins.jacoco;

import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

@SupportedEnvironment("maven")
public class JacocoMavenInitializer extends Initializer implements CoverageExtension, DependsUponMavenPlugin {

  private JaCoCoMavenPluginHandler handler;

  public JacocoMavenInitializer(JaCoCoMavenPluginHandler handler) {
    this.handler = handler;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)
      && !project.getFileSystem().testFiles(Java.KEY).isEmpty();
  }

  @Override
  public void execute(Project project) {
    // nothing to do
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    return handler;
  }

}
