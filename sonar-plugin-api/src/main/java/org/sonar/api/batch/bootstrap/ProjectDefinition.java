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
package org.sonar.api.batch.bootstrap;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Defines project metadata (key, name, source directories, ...). It's generally used by the
 * {@link org.sonar.api.batch.bootstrap.ProjectBuilder extension point} and must not be used
 * by other standard extensions.
 *
 * @since 2.9
 */
public class ProjectDefinition {

  public static final String SOURCE_DIRS_PROPERTY = "sonar.sources";
  public static final String SOURCE_FILES_PROPERTY = "sonar.sourceFiles";
  public static final String TEST_DIRS_PROPERTY = "sonar.tests";
  public static final String TEST_FILES_PROPERTY = "sonar.testFiles";
  public static final String BINARIES_PROPERTY = "sonar.binaries";
  public static final String LIBRARIES_PROPERTY = "sonar.libraries";
  public static final String BUILD_DIR_PROPERTY = "sonar.buildDir";

  private static final char SEPARATOR = ',';

  private File baseDir, workDir, buildDir;
  private Properties properties = new Properties();
  private ProjectDefinition parent = null;
  private List<ProjectDefinition> subProjects = Lists.newArrayList();
  private List<Object> containerExtensions = Lists.newArrayList();

  private ProjectDefinition(Properties p) {
    this.properties = p;
  }

  /**
   * @deprecated in 2.12, because it uses external object to represent internal state.
   *             To ensure backward-compatibility with Ant task this method cannot clone properties,
   *             so other callers must explicitly make clone of properties before passing into this method.
   *             Thus better to use {@link #create()} with combination of other methods like {@link #setProperties(Properties)} and {@link #setProperty(String, String)}.
   */
  @Deprecated
  public static ProjectDefinition create(Properties properties) {
    return new ProjectDefinition(properties);
  }

  public static ProjectDefinition create() {
    return new ProjectDefinition(new Properties());
  }

  public ProjectDefinition setBaseDir(File baseDir) {
    this.baseDir = baseDir;
    return this;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public ProjectDefinition setWorkDir(@Nullable File workDir) {
    this.workDir = workDir;
    return this;
  }

  @CheckForNull
  public File getWorkDir() {
    return workDir;
  }

  public ProjectDefinition setBuildDir(@Nullable File d) {
    this.buildDir = d;
    return this;
  }

  @CheckForNull
  public File getBuildDir() {
    return buildDir;
  }

  public Properties getProperties() {
    return properties;
  }

  /**
   * Copies specified properties into this object.
   *
   * @since 2.12
   */
  public ProjectDefinition setProperties(Properties properties) {
    this.properties.putAll(properties);
    return this;
  }

  public ProjectDefinition setProperty(String key, String value) {
    properties.setProperty(key, value);
    return this;
  }

  public ProjectDefinition setKey(String key) {
    properties.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, key);
    return this;
  }

  public ProjectDefinition setVersion(String s) {
    properties.setProperty(CoreProperties.PROJECT_VERSION_PROPERTY, StringUtils.defaultString(s));
    return this;
  }

  public ProjectDefinition setName(String s) {
    properties.setProperty(CoreProperties.PROJECT_NAME_PROPERTY, StringUtils.defaultString(s));
    return this;
  }

  public ProjectDefinition setDescription(String s) {
    properties.setProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY, StringUtils.defaultString(s));
    return this;
  }

  public String getKey() {
    return properties.getProperty(CoreProperties.PROJECT_KEY_PROPERTY);
  }

  public String getVersion() {
    return properties.getProperty(CoreProperties.PROJECT_VERSION_PROPERTY);
  }

  public String getName() {
    String name = properties.getProperty(CoreProperties.PROJECT_NAME_PROPERTY);
    if (StringUtils.isBlank(name)) {
      name = "Unnamed - " + getKey();
    }
    return name;
  }

  public String getDescription() {
    return properties.getProperty(CoreProperties.PROJECT_DESCRIPTION_PROPERTY);
  }

  private void appendProperty(String key, String value) {
    String newValue = properties.getProperty(key, "") + SEPARATOR + value;
    properties.put(key, newValue);
  }

  public List<String> getSourceDirs() {
    String sources = properties.getProperty(SOURCE_DIRS_PROPERTY, "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param paths paths to directory with main sources.
   *              They can be absolute or relative to project base directory.
   */
  public ProjectDefinition addSourceDirs(String... paths) {
    for (String path : paths) {
      appendProperty(SOURCE_DIRS_PROPERTY, path);
    }
    return this;
  }

  public ProjectDefinition addSourceDirs(File... dirs) {
    for (File dir : dirs) {
      addSourceDirs(dir.getAbsolutePath());
    }
    return this;
  }

  public ProjectDefinition resetSourceDirs() {
    properties.remove(SOURCE_DIRS_PROPERTY);
    return this;
  }

  public ProjectDefinition setSourceDirs(String... paths) {
    resetSourceDirs();
    return addSourceDirs(paths);
  }

  public ProjectDefinition setSourceDirs(File... dirs) {
    resetSourceDirs();
    for (File dir : dirs) {
      addSourceDirs(dir.getAbsolutePath());
    }
    return this;
  }

  /**
   * Adding source files is possible only if no source directories have been set.
   * Absolute path or relative path from project base dir.
   */
  public ProjectDefinition addSourceFiles(String... paths) {
    for (String path : paths) {
      appendProperty(SOURCE_FILES_PROPERTY, path);
    }
    return this;
  }

  /**
   * Adding source files is possible only if no source directories have been set.
   */
  public ProjectDefinition addSourceFiles(File... files) {
    for (File file : files) {
      addSourceFiles(file.getAbsolutePath());
    }
    return this;
  }

  public List<String> getSourceFiles() {
    String sources = properties.getProperty(SOURCE_FILES_PROPERTY, "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  public List<String> getTestDirs() {
    String sources = properties.getProperty(TEST_DIRS_PROPERTY, "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param paths path to directory with test sources.
   *              It can be absolute or relative to project directory.
   */
  public ProjectDefinition addTestDirs(String... paths) {
    for (String path : paths) {
      appendProperty(TEST_DIRS_PROPERTY, path);
    }
    return this;
  }

  public ProjectDefinition addTestDirs(File... dirs) {
    for (File dir : dirs) {
      addTestDirs(dir.getAbsolutePath());
    }
    return this;
  }

  public ProjectDefinition setTestDirs(String... paths) {
    resetTestDirs();
    return addTestDirs(paths);
  }

  public ProjectDefinition setTestDirs(File... dirs) {
    resetTestDirs();
    for (File dir : dirs) {
      addTestDirs(dir.getAbsolutePath());
    }
    return this;
  }

  public ProjectDefinition resetTestDirs() {
    properties.remove(TEST_DIRS_PROPERTY);
    return this;
  }

  /**
   * Adding source files is possible only if no source directories have been set.
   * Absolute path or relative path from project base dir.
   */
  public ProjectDefinition addTestFiles(String... paths) {
    for (String path : paths) {
      appendProperty(TEST_FILES_PROPERTY, path);
    }
    return this;
  }

  /**
   * Adding source files is possible only if no source directories have been set.
   */
  public ProjectDefinition addTestFiles(File... files) {
    for (File file : files) {
      addTestFiles(file.getAbsolutePath());
    }
    return this;
  }

  public List<String> getTestFiles() {
    String sources = properties.getProperty(TEST_FILES_PROPERTY, "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  public List<String> getBinaries() {
    String sources = properties.getProperty(BINARIES_PROPERTY, "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with compiled source. In case of Java this is directory with class files.
   *             It can be absolute or relative to project directory.
   *             TODO currently Sonar supports only one such directory due to dependency on MavenProject
   */
  public ProjectDefinition addBinaryDir(String path) {
    appendProperty(BINARIES_PROPERTY, path);
    return this;
  }

  public ProjectDefinition addBinaryDir(File f) {
    return addBinaryDir(f.getAbsolutePath());
  }

  public List<String> getLibraries() {
    String sources = properties.getProperty(LIBRARIES_PROPERTY, "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to file with third-party library. In case of Java this is path to jar file.
   *             It can be absolute or relative to project directory.
   */
  public void addLibrary(String path) {
    appendProperty(LIBRARIES_PROPERTY, path);
  }

  /**
   * Adds an extension, which would be available in PicoContainer during analysis of this project.
   *
   * @since 2.8
   */
  public ProjectDefinition addContainerExtension(Object extension) {
    containerExtensions.add(extension);
    return this;
  }

  /**
   * @since 2.8
   */
  public List<Object> getContainerExtensions() {
    return containerExtensions;
  }

  /**
   * @since 2.8
   */
  public ProjectDefinition addSubProject(ProjectDefinition child) {
    subProjects.add(child);
    child.setParent(this);
    return this;
  }

  public ProjectDefinition getParent() {
    return parent;
  }

  public void remove() {
    if (parent != null) {
      parent.subProjects.remove(this);
      parent = null;
      subProjects.clear();
    }
  }

  private void setParent(ProjectDefinition parent) {
    this.parent = parent;
  }

  /**
   * @since 2.8
   */
  public List<ProjectDefinition> getSubProjects() {
    return subProjects;
  }

  private static List<String> trim(String[] strings) {
    List<String> result = Lists.newArrayList();
    for (String s : strings) {
      result.add(StringUtils.trim(s));
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDefinition that = (ProjectDefinition) o;
    String key = getKey();
    if (key != null ? !key.equals(that.getKey()) : that.getKey() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    String key = getKey();
    return key != null ? key.hashCode() : 0;
  }
}
