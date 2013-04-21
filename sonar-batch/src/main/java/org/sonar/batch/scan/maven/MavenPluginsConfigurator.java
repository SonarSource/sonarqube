/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.maven;

import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MavenPluginsConfigurator implements BatchComponent {

  private BatchExtensionDictionnary dictionnary = null;

  public MavenPluginsConfigurator(BatchExtensionDictionnary dictionnary) {
    this.dictionnary = dictionnary;
  }

  public void execute(Project project) {
    Logger logger = LoggerFactory.getLogger(getClass());
    logger.info("Configure Maven plugins");

    for (MavenPluginHandler handler : dictionnary.selectMavenPluginHandlers(project)) {
      logger.debug("Configure {}...", handler);
      configureHandler(project, handler);
    }
    savePom(project);
  }

  protected void configureHandler(Project project, MavenPluginHandler handler) {
    MavenPlugin plugin = MavenPlugin.registerPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId(), handler.getVersion(), handler.isFixedVersion());
    handler.configure(project, plugin);
  }

  protected void savePom(Project project) {
    MavenProject pom = project.getPom();
    if (pom != null) {
      File targetPom = new File(project.getFileSystem().getSonarWorkingDirectory(), "sonar-pom.xml");
      FileWriter fileWriter = null;
      try {
        fileWriter = new FileWriter(targetPom, false);
        pom.writeModel(fileWriter);

      } catch (IOException e) {
        throw new IllegalStateException("Can not save pom to " + targetPom, e);
      } finally {
        IOUtils.closeQuietly(fileWriter);
      }
    }
  }
}
