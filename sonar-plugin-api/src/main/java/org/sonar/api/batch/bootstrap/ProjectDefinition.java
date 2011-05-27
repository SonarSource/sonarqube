/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.api.batch.bootstrap;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Defines project metadata (key, name, source directories, ...). It's generally used by the
 * {@link org.sonar.api.batch.bootstrap.ProjectBuilder extension point}
 * 
 * @since 2.9
 */
public final class ProjectDefinition implements BatchComponent {

  public static final String SOURCES_PROPERTY = "sonar.sources";
  public static final String TESTS_PROPERTY = "sonar.tests";
  public static final String BINARIES_PROPERTY = "sonar.binaries";
  public static final String LIBRARIES_PROPERTY = "sonar.libraries";

  private static final char SEPARATOR = ',';

  private File baseDir;
  private File workDir;
  private Properties properties;
  private ProjectDefinition parent = null;
  private List<ProjectDefinition> subProjects = Lists.newArrayList();
  private List<Object> containerExtensions = Lists.newArrayList();

  public ProjectDefinition(File baseDir, File workDir, Properties properties) {
    this.baseDir = baseDir;
    this.workDir = workDir;
    this.properties = properties;
  }

  private ProjectDefinition() {
  }

  public static ProjectDefinition create() {
    return new ProjectDefinition();
  }

  public File getBaseDir() {
    return baseDir;
  }

  public ProjectDefinition setBaseDir(File baseDir) {
    this.baseDir = baseDir;
    return this;
  }

  public ProjectDefinition setWorkDir(File workDir) {
    this.workDir = workDir;
    return this;
  }

  public File getWorkDir() {
    return workDir;
  }

  public Properties getProperties() {
    return properties;
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
    String sources = properties.getProperty(SOURCES_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with main sources.
   *          It can be absolute or relative to project directory.
   */
  public ProjectDefinition addSourceDir(String path) {
    appendProperty(SOURCES_PROPERTY, path);
    return this;
  }

  public ProjectDefinition addSourceDir(File path) {
    addSourceDir(path.getAbsolutePath());
    return this;
  }

  public ProjectDefinition setSourceDir(String path) {
    properties.setProperty(SOURCES_PROPERTY, path);
    return this;
  }

  public ProjectDefinition setSourceDir(File path) {
    setSourceDir(path.getAbsolutePath());
    return this;
  }

  public List<String> getTestDirs() {
    String sources = properties.getProperty(TESTS_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with test sources.
   *          It can be absolute or relative to project directory.
   */
  public ProjectDefinition addTestDir(String path) {
    appendProperty(TESTS_PROPERTY, path);
    return this;
  }

  public List<String> getBinaries() {
    String sources = properties.getProperty(BINARIES_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with compiled source. In case of Java this is directory with class files.
   *          It can be absolute or relative to project directory.
   * @TODO currently Sonar supports only one such directory due to dependency on MavenProject
   */
  public ProjectDefinition addBinaryDir(String path) {
    appendProperty(BINARIES_PROPERTY, path);
    return this;
  }

  public List<String> getLibraries() {
    String sources = properties.getProperty(LIBRARIES_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to file with third-party library. In case of Java this is path to jar file.
   *          It can be absolute or relative to project directory.
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

  private void setParent(ProjectDefinition parent) {
    this.parent = parent;
  }

  /**
   * @since 2.8
   */
  public List<ProjectDefinition> getSubProjects() {
    return subProjects;
  }
}
