/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Reporting;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.ProjectDefinition;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * This is a dirty hack for for non-Maven environments,
 * which allows to create {@link MavenProject} based on {@link ProjectDefinition}.
 */
public class InMemoryPomCreator {

  private ProjectDefinition project;

  public InMemoryPomCreator(ProjectDefinition project) {
    this.project = project;
  }

  public MavenProject create() {
    Properties properties = project.getProperties();
    final String[] binaries = StringUtils.split(properties.getProperty("sonar.projectBinaries", ""), ',');

    final MavenProject pom = new MavenProject() {
      /**
       * This allows to specify base directory without specifying location of a pom.xml
       */
      @Override
      public File getBasedir() {
        return project.getBaseDir();
      };

      /**
       * This allows to specify project binaries.
       */
      @Override
      public List<String> getCompileClasspathElements() throws DependencyResolutionRequiredException {
        return Arrays.asList(binaries);
      }
    };

    String key = getPropertyOrDie(properties, CoreProperties.PROJECT_KEY_PROPERTY);
    String[] keys = key.split(":");
    pom.setGroupId(keys[0]);
    pom.setArtifactId(keys[1]);
    pom.setVersion(getPropertyOrDie(properties, CoreProperties.PROJECT_VERSION_PROPERTY));

    pom.getModel().setProperties(properties);

    pom.setArtifacts(Collections.EMPTY_SET);

    // Configure fake directories
    String buildDirectory = properties.getProperty("project.build.directory", project.getBaseDir().getAbsolutePath() + "/target");
    pom.getBuild().setDirectory(buildDirectory);
    String buildOutputDirectory = properties.getProperty("project.build.outputDirectory", buildDirectory + "/classes");
    pom.getBuild().setOutputDirectory(buildOutputDirectory);
    Reporting reporting = new Reporting();
    String reportingOutputDirectory = properties.getProperty("project.reporting.outputDirectory", buildDirectory + "/site");
    reporting.setOutputDirectory(reportingOutputDirectory);
    pom.setReporting(reporting);

    // Configure source directories
    for (String dir : project.getSourceDirs()) {
      pom.addCompileSourceRoot(dir);
    }

    // Configure test directories
    for (String dir : project.getTestDirs()) {
      pom.addTestCompileSourceRoot(dir);
    }

    return pom;
  }

  private static String getPropertyOrDie(Properties properties, String key) {
    String value = properties.getProperty(key);
    if (StringUtils.isBlank(value)) {
      throw new SonarException("Property '" + key + "' must be specified");
    }
    return value;
  }

}
