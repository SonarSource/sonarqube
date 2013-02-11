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
package org.sonar.batch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class MavenProjectConverter {

  private static final String UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE = "Unable to determine structure of project." +
      " Probably you use Maven Advanced Reactor Options, which is not supported by Sonar and should not be used.";

  public static ProjectDefinition convert(List<MavenProject> poms, MavenProject root) {
    Map<String, MavenProject> paths = Maps.newHashMap(); // projects by canonical path to pom.xml
    Map<MavenProject, ProjectDefinition> defs = Maps.newHashMap();

    try {
      for (MavenProject pom : poms) {
        paths.put(pom.getFile().getCanonicalPath(), pom);
        defs.put(pom, convert(pom));
      }

      for (Map.Entry<String, MavenProject> entry : paths.entrySet()) {
        MavenProject pom = entry.getValue();
        for (Object m : pom.getModules()) {
          String moduleId = (String) m;
          File modulePath = new File(pom.getBasedir(), moduleId);
          if (modulePath.exists() && modulePath.isDirectory()) {
            modulePath = new File(modulePath, "pom.xml");
          }
          MavenProject module = paths.get(modulePath.getCanonicalPath());

          ProjectDefinition parentProject = defs.get(pom);
          ProjectDefinition subProject = defs.get(module);
          if (parentProject == null) {
            throw new IllegalStateException(UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE);
          }
          if (subProject == null) {
            throw new IllegalStateException(UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE);
          }
          parentProject.addSubProject(subProject);
        }
      }
    } catch (IOException e) {
      throw new SonarException(e);
    }

    ProjectDefinition rootProject = defs.get(root);
    if (rootProject == null) {
      throw new IllegalStateException(UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE);
    }

    return rootProject;
  }

  @VisibleForTesting
  static ProjectDefinition convert(MavenProject pom) {
    String key = new StringBuilder().append(pom.getGroupId()).append(":").append(pom.getArtifactId()).toString();
    ProjectDefinition definition = ProjectDefinition.create();
    // IMPORTANT NOTE : reference on properties from POM model must not be saved, instead they should be copied explicitly - see SONAR-2896
    definition
        .setProperties(pom.getModel().getProperties())
        .setKey(key)
        .setVersion(pom.getVersion())
        .setName(pom.getName())
        .setDescription(pom.getDescription())
        .addContainerExtension(pom);

    convertMavenLinksToProperties(definition, pom);

    synchronizeFileSystem(pom, definition);

    return definition;
  }

  /**
   * For SONAR-3676
   */
  private static void convertMavenLinksToProperties(ProjectDefinition definition, MavenProject pom) {
    setPropertyIfNotAlreadyExists(definition, CoreProperties.LINKS_HOME_PAGE, pom.getUrl());

    Scm scm = pom.getScm();
    if (scm == null) {
      scm = new Scm();
    }
    setPropertyIfNotAlreadyExists(definition, CoreProperties.LINKS_SOURCES, scm.getUrl());
    setPropertyIfNotAlreadyExists(definition, CoreProperties.LINKS_SOURCES_DEV, scm.getDeveloperConnection());

    CiManagement ci = pom.getCiManagement();
    if (ci == null) {
      ci = new CiManagement();
    }
    setPropertyIfNotAlreadyExists(definition, CoreProperties.LINKS_CI, ci.getUrl());

    IssueManagement issues = pom.getIssueManagement();
    if (issues == null) {
      issues = new IssueManagement();
    }
    setPropertyIfNotAlreadyExists(definition, CoreProperties.LINKS_ISSUE_TRACKER, issues.getUrl());
  }

  private static void setPropertyIfNotAlreadyExists(ProjectDefinition definition, String propertyKey, String propertyValue) {
    if (StringUtils.isBlank(definition.getProperties().getProperty(propertyKey))) {
      definition.setProperty(propertyKey, StringUtils.defaultString(propertyValue));
    }
  }

  public static void synchronizeFileSystem(MavenProject pom, ProjectDefinition into) {
    into.setBaseDir(pom.getBasedir());
    into.setWorkDir(new File(resolvePath(pom.getBuild().getDirectory(), pom.getBasedir()), "sonar"));
    into.setSourceDirs((String[]) pom.getCompileSourceRoots().toArray(new String[pom.getCompileSourceRoots().size()]));
    into.setTestDirs((String[]) pom.getTestCompileSourceRoots().toArray(new String[pom.getTestCompileSourceRoots().size()]));
  }

  static File resolvePath(String path, File basedir) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(basedir, path).getCanonicalFile();
      } catch (IOException e) {
        throw new SonarException("Unable to resolve path '" + path + "'", e);
      }
    }
    return file;
  }

  private MavenProjectConverter() {
    // only static methods
  }
}
