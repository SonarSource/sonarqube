/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.maven;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
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
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.utils.MessageException;
import org.sonar.java.api.JavaUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @deprecated since 4.3 kept only to support old version of SonarQube Mojo
 */
@Deprecated
@SupportedEnvironment("maven")
public class MavenProjectConverter implements TaskExtension {

  private static final String UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE = "Unable to determine structure of project." +
    " Probably you use Maven Advanced Reactor Options, which is not supported by SonarQube and should not be used.";

  public ProjectDefinition configure(List<MavenProject> poms, MavenProject root) {
    // projects by canonical path to pom.xml
    Map<String, MavenProject> paths = Maps.newHashMap();
    Map<MavenProject, ProjectDefinition> defs = Maps.newHashMap();

    try {
      configureModules(poms, paths, defs);

      rebuildModuleHierarchy(paths, defs);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot configure project", e);
    }

    ProjectDefinition rootProject = defs.get(root);
    if (rootProject == null) {
      throw new IllegalStateException(UNABLE_TO_DETERMINE_PROJECT_STRUCTURE_EXCEPTION_MESSAGE);
    }
    return rootProject;
  }

  private void rebuildModuleHierarchy(Map<String, MavenProject> paths, Map<MavenProject, ProjectDefinition> defs) throws IOException {
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
  }

  private void configureModules(List<MavenProject> poms, Map<String, MavenProject> paths, Map<MavenProject, ProjectDefinition> defs) throws IOException {
    for (MavenProject pom : poms) {
      paths.put(pom.getFile().getCanonicalPath(), pom);
      ProjectDefinition def = ProjectDefinition.create();
      merge(pom, def);
      defs.put(pom, def);
    }
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
    }
    return paths.get(modulePath.getCanonicalPath());
  }

  @VisibleForTesting
  void merge(MavenProject pom, ProjectDefinition definition) {
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

  private static String getSonarKey(MavenProject pom) {
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
    if (StringUtils.isBlank(definition.properties().get(propertyKey))) {
      definition.setProperty(propertyKey, StringUtils.defaultString(propertyValue));
    }
  }

  public void synchronizeFileSystem(MavenProject pom, ProjectDefinition into) {
    into.setBaseDir(pom.getBasedir());
    File buildDir = getBuildDir(pom);
    if (buildDir != null) {
      into.setBuildDir(buildDir);
      into.setWorkDir(getSonarWorkDir(pom));
    }
    into.setSourceDirs(toPaths(mainDirs(pom)));
    into.setTestDirs(toPaths(testDirs(pom)));
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

  static File resolvePath(@Nullable String path, File basedir) {
    if (path != null) {
      File file = new File(StringUtils.trim(path));
      if (!file.isAbsolute()) {
        try {
          file = new File(basedir, path).getCanonicalFile();
        } catch (IOException e) {
          throw new IllegalStateException("Unable to resolve path '" + path + "'", e);
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

  private List<File> mainDirs(MavenProject pom) {
    return sourceDirs(pom, ProjectDefinition.SOURCE_DIRS_PROPERTY, pom.getCompileSourceRoots());
  }

  private List<File> testDirs(MavenProject pom) {
    return sourceDirs(pom, ProjectDefinition.TEST_DIRS_PROPERTY, pom.getTestCompileSourceRoots());
  }

  private List<File> sourceDirs(MavenProject pom, String propertyKey, List mavenDirs) {
    List<String> paths;
    String prop = pom.getProperties().getProperty(propertyKey);
    if (prop != null) {
      paths = Arrays.asList(StringUtils.split(prop, ","));
      // do not remove dirs that do not exist. They must be kept in order to
      // notify users that value of sonar.sources has a typo.
      return existingDirsOrFail(resolvePaths(paths, pom.getBasedir()), pom, propertyKey);
    }

    List<File> dirs = resolvePaths(mavenDirs, pom.getBasedir());

    // Maven provides some directories that do not exist. They
    // should be removed
    return keepExistingDirs(dirs);
  }

  private List<File> existingDirsOrFail(List<File> dirs, MavenProject pom, String propertyKey) {
    for (File dir : dirs) {
      if (!dir.isDirectory() || !dir.exists()) {
        throw MessageException.of(String.format(
          "The directory '%s' does not exist for Maven module %s. Please check the property %s",
          dir.getAbsolutePath(), pom.getId(), propertyKey));
      }
    }
    return dirs;
  }

  private static List<File> keepExistingDirs(List<File> files) {
    return Lists.newArrayList(Collections2.filter(files, new Predicate<File>() {
      @Override
      public boolean apply(File dir) {
        return dir != null && dir.exists() && dir.isDirectory();
      }
    }));
  }

  private static String[] toPaths(Collection<File> dirs) {
    Collection<String> paths = Collections2.transform(dirs, new Function<File, String>() {
      @Override
      public String apply(File dir) {
        return dir.getAbsolutePath();
      }
    });
    return paths.toArray(new String[paths.size()]);
  }
}
