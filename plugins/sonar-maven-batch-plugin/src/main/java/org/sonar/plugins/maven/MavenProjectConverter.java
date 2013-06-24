/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.maven;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.java.api.JavaUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MavenProjectConverter implements TaskExtension {

  private static final String UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE = "Unable to determine structure of project." +
    " Probably you use Maven Advanced Reactor Options, which is not supported by Sonar and should not be used.";

  public ProjectDefinition configure(List<MavenProject> poms, MavenProject root) {
    // projects by canonical path to pom.xml
    Map<String, MavenProject> paths = Maps.newHashMap();
    Map<MavenProject, ProjectDefinition> defs = Maps.newHashMap();

    try {
      for (MavenProject pom : poms) {
        paths.put(pom.getFile().getCanonicalPath(), pom);
        ProjectDefinition def = ProjectDefinition.create();
        merge(pom, def);
        defs.put(pom, def);
      }

      for (Map.Entry<String, MavenProject> entry : paths.entrySet()) {
        MavenProject pom = entry.getValue();
        for (Object m : pom.getModules()) {
          String moduleId = (String) m;
          File modulePath = new File(pom.getBasedir(), moduleId);
          MavenProject module = findMavenProject(modulePath, paths);

          ProjectDefinition parentProject = defs.get(pom);
          if (parentProject == null) {
            throw new IllegalStateException(UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE);
          }
          ProjectDefinition subProject = defs.get(module);
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

  private static MavenProject findMavenProject(final File modulePath, Map<String, MavenProject> paths) throws IOException {
    if (modulePath.exists() && modulePath.isDirectory()) {
      for (Map.Entry<String, MavenProject> entry : paths.entrySet()) {
        String pomFileParentDir = new File(entry.getKey()).getParent();
        if (pomFileParentDir.equals(modulePath.getCanonicalPath())) {
          return entry.getValue();
        }
      }
      return null;
    } else {
      return paths.get(modulePath.getCanonicalPath());
    }
  }

  @VisibleForTesting
  static void merge(MavenProject pom, ProjectDefinition definition) {
    String key = getSonarKey(pom);
    // IMPORTANT NOTE : reference on properties from POM model must not be saved,
    // instead they should be copied explicitly - see SONAR-2896
    definition
        .setProperties(pom.getModel().getProperties())
        .setKey(key)
        .setVersion(pom.getVersion())
        .setName(pom.getName())
        .setDescription(pom.getDescription())
        .addContainerExtension(pom);
    guessJavaVersion(pom, definition);
    guessEncoding(pom, definition);
    convertMavenLinksToProperties(definition, pom);
    synchronizeFileSystem(pom, definition);
  }

  public static String getSonarKey(MavenProject pom) {
    return new StringBuilder().append(pom.getGroupId()).append(":").append(pom.getArtifactId()).toString();
  }

  private static void guessEncoding(MavenProject pom, ProjectDefinition definition) {
    // See http://jira.codehaus.org/browse/SONAR-2151
    String encoding = MavenUtils.getSourceEncoding(pom);
    if (encoding != null) {
      definition.setProperty(CoreProperties.ENCODING_PROPERTY, encoding);
    }
  }

  private static void guessJavaVersion(MavenProject pom, ProjectDefinition definition) {
    // See http://jira.codehaus.org/browse/SONAR-2148
    // Get Java source and target versions from maven-compiler-plugin.
    String version = MavenUtils.getJavaSourceVersion(pom);
    if (version != null) {
      definition.setProperty(JavaUtils.JAVA_SOURCE_PROPERTY, version);
    }
    version = MavenUtils.getJavaVersion(pom);
    if (version != null) {
      definition.setProperty(JavaUtils.JAVA_TARGET_PROPERTY, version);
    }
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
    File buildDir = getBuildDir(pom);
    if (buildDir != null) {
      into.setBuildDir(buildDir);
      into.setWorkDir(getSonarWorkDir(pom));
    }
    List<String> filteredCompileSourceRoots = filterExisting(pom.getCompileSourceRoots(), pom.getBasedir());
    List<String> filteredTestCompileSourceRoots = filterExisting(pom.getTestCompileSourceRoots(), pom.getBasedir());
    into.setSourceDirs((String[]) filteredCompileSourceRoots.toArray(new String[filteredCompileSourceRoots.size()]));
    into.setTestDirs((String[]) filteredTestCompileSourceRoots.toArray(new String[filteredTestCompileSourceRoots.size()]));
    File binaryDir = resolvePath(pom.getBuild().getOutputDirectory(), pom.getBasedir());
    if (binaryDir != null) {
      into.addBinaryDir(binaryDir);
    }
  }

  public static File getSonarWorkDir(MavenProject pom) {
    return new File(getBuildDir(pom), "sonar");
  }

  private static File getBuildDir(MavenProject pom) {
    return resolvePath(pom.getBuild().getDirectory(), pom.getBasedir());
  }

  private static List<String> filterExisting(List<String> filePaths, final File baseDir) {
    return Lists.newArrayList(Collections2.filter(filePaths, new Predicate<String>() {
      @Override
      public boolean apply(String filePath) {
        File file = resolvePath(filePath, baseDir);
        return file != null && file.exists();
      }
    }));
  }

  public static void synchronizeFileSystem(MavenProject pom, DefaultModuleFileSystem into) {
    into.resetDirs(
        pom.getBasedir(),
        getBuildDir(pom),
        resolvePaths((List<String>) pom.getCompileSourceRoots(), pom.getBasedir()),
        resolvePaths((List<String>) pom.getTestCompileSourceRoots(), pom.getBasedir()),
        Arrays.asList(resolvePath(pom.getBuild().getOutputDirectory(), pom.getBasedir()))
        );
  }

  static File resolvePath(String path, File basedir) {
    if (path != null) {
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
    return null;
  }

  static List<File> resolvePaths(List<String> paths, File basedir) {
    List<File> result = Lists.newArrayList();
    for (String path : paths) {
      File dir = resolvePath(path, basedir);
      if (dir != null) {
        result.add(dir);
      }
    }
    return result;
  }
}
