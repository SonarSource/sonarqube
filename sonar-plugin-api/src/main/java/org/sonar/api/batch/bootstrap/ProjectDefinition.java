/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;

/**
 * Defines project metadata (key, name, source directories, ...). It's generally used by the
 * {@link org.sonar.api.batch.bootstrap.ProjectBuilder extension point} and must not be used
 * by other standard extensions.
 *
 * Since 6.5, plugins should no longer manipulate the project's structure.
 *
 * @since 2.9
 * @deprecated since 7.6 use {@link org.sonar.api.scanner.fs.InputProject}
 */
@Deprecated
public class ProjectDefinition {

  public static final String SOURCES_PROPERTY = "sonar.sources";

  public static final String TESTS_PROPERTY = "sonar.tests";

  private static final char SEPARATOR = ',';

  private File baseDir;
  private File workDir;
  private File buildDir;
  private Map<String, String> properties = new LinkedHashMap<>();
  private ProjectDefinition parent = null;
  private List<ProjectDefinition> subProjects = new ArrayList<>();

  private ProjectDefinition(Properties p) {
    for (Entry<Object, Object> entry : p.entrySet()) {
      this.properties.put(entry.getKey().toString(), entry.getValue().toString());
    }
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

  public ProjectDefinition setWorkDir(File workDir) {
    this.workDir = workDir;
    return this;
  }

  public File getWorkDir() {
    return workDir;
  }

  /**
   * @deprecated since 6.1 notion of buildDir is not well defined
   */
  @Deprecated
  public ProjectDefinition setBuildDir(File d) {
    this.buildDir = d;
    return this;
  }

  /**
   * @deprecated since 6.1 notion of buildDir is not well defined
   */
  @Deprecated
  public File getBuildDir() {
    return buildDir;
  }

  /**
   * @deprecated since 5.0 use {@link #properties()}
   */
  @Deprecated
  public Properties getProperties() {
    Properties result = new Properties();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      result.setProperty(entry.getKey(), entry.getValue());
    }
    return result;
  }

  public Map<String, String> properties() {
    return properties;
  }

  /**
   * Copies specified properties into this object.
   *
   * @since 2.12
   * @deprecated since 5.0 use {@link #setProperties(Map)}
   */
  @Deprecated
  public ProjectDefinition setProperties(Properties properties) {
    for (Entry<Object, Object> entry : properties.entrySet()) {
      this.properties.put(entry.getKey().toString(), entry.getValue().toString());
    }
    return this;
  }

  public ProjectDefinition setProperties(Map<String, String> properties) {
    this.properties.putAll(properties);
    return this;
  }

  public ProjectDefinition setProperty(String key, String value) {
    properties.put(key, value);
    return this;
  }

  public ProjectDefinition setKey(String key) {
    properties.put(CoreProperties.PROJECT_KEY_PROPERTY, key);
    return this;
  }

  public ProjectDefinition setProjectVersion(String s) {
    properties.put(CoreProperties.PROJECT_VERSION_PROPERTY, StringUtils.defaultString(s));
    return this;
  }

  public ProjectDefinition setName(String s) {
    properties.put(CoreProperties.PROJECT_NAME_PROPERTY, StringUtils.defaultString(s));
    return this;
  }

  public ProjectDefinition setDescription(String s) {
    properties.put(CoreProperties.PROJECT_DESCRIPTION_PROPERTY, StringUtils.defaultString(s));
    return this;
  }

  public String getKey() {
    return properties.get(CoreProperties.PROJECT_KEY_PROPERTY);
  }

  /**
   * @deprecated since 7.7, use {@link #getOriginalProjectVersion()} instead
   */
  @Deprecated
  @CheckForNull
  public String getOriginalVersion() {
    return getOriginalProjectVersion();
  }

  /**
   * @deprecated since 7.7, use {@link #getProjectVersion()} instead
   */
  @Deprecated
  public String getVersion() {
    return getProjectVersion();
  }

  @CheckForNull
  public String getOriginalProjectVersion() {
    return properties.get(CoreProperties.PROJECT_VERSION_PROPERTY);
  }

  public String getProjectVersion() {
    String version = properties.get(CoreProperties.PROJECT_VERSION_PROPERTY);
    if (StringUtils.isBlank(version)) {
      version = "not provided";
    }
    return version;
  }

  @CheckForNull
  public String getOriginalName() {
    return properties.get(CoreProperties.PROJECT_NAME_PROPERTY);
  }

  public String getName() {
    String name = properties.get(CoreProperties.PROJECT_NAME_PROPERTY);
    if (StringUtils.isBlank(name)) {
      name = getKey();
    }
    return name;
  }

  public String getDescription() {
    return properties.get(CoreProperties.PROJECT_DESCRIPTION_PROPERTY);
  }

  private void appendProperty(String key, String value) {
    String current = (String) ObjectUtils.defaultIfNull(properties.get(key), "");
    if (StringUtils.isBlank(current)) {
      properties.put(key, value);
    } else {
      properties.put(key, current + SEPARATOR + value);
    }
  }

  /**
   * @return Source files and folders.
   */
  public List<String> sources() {
    String sources = (String) ObjectUtils.defaultIfNull(properties.get(SOURCES_PROPERTY), "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param paths paths to file or directory with main sources.
   *              They can be absolute or relative to project base directory.
   */
  public ProjectDefinition addSources(String... paths) {
    for (String path : paths) {
      appendProperty(SOURCES_PROPERTY, path);
    }
    return this;
  }

  public ProjectDefinition addSources(File... fileOrDirs) {
    for (File fileOrDir : fileOrDirs) {
      addSources(fileOrDir.getAbsolutePath());
    }
    return this;
  }

  public ProjectDefinition resetSources() {
    properties.remove(SOURCES_PROPERTY);
    return this;
  }

  public ProjectDefinition setSources(String... paths) {
    resetSources();
    return addSources(paths);
  }

  public ProjectDefinition setSources(File... filesOrDirs) {
    resetSources();
    for (File fileOrDir : filesOrDirs) {
      addSources(fileOrDir.getAbsolutePath());
    }
    return this;
  }

  public List<String> tests() {
    String sources = (String) ObjectUtils.defaultIfNull(properties.get(TESTS_PROPERTY), "");
    return trim(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param paths path to files or directories with test sources.
   *              It can be absolute or relative to project directory.
   */
  public ProjectDefinition addTests(String... paths) {
    for (String path : paths) {
      appendProperty(TESTS_PROPERTY, path);
    }
    return this;
  }

  public ProjectDefinition addTests(File... fileOrDirs) {
    for (File fileOrDir : fileOrDirs) {
      addTests(fileOrDir.getAbsolutePath());
    }
    return this;
  }

  public ProjectDefinition setTests(String... paths) {
    resetTests();
    return addTests(paths);
  }

  public ProjectDefinition setTests(File... fileOrDirs) {
    resetTests();
    for (File dir : fileOrDirs) {
      addTests(dir.getAbsolutePath());
    }
    return this;
  }

  public ProjectDefinition resetTests() {
    properties.remove(TESTS_PROPERTY);
    return this;
  }

  /**
   * @since 2.8
   */
  public ProjectDefinition addSubProject(ProjectDefinition child) {
    subProjects.add(child);
    child.setParent(this);
    return this;
  }

  @CheckForNull
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
    List<String> result = new ArrayList<>();
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
    return !((key != null) ? !key.equals(that.getKey()) : (that.getKey() != null));

  }

  @Override
  public int hashCode() {
    String key = getKey();
    return key != null ? key.hashCode() : 0;
  }
}
