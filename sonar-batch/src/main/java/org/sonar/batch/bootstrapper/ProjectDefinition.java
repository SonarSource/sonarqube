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
package org.sonar.batch.bootstrapper;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Describes project in a form suitable to bootstrap Sonar batch.
 * We assume that project is just a set of configuration properties and directories.
 * 
 * @since 2.6
 */
public class ProjectDefinition {

  private static final String PROJECT_SOURCES_PROPERTY = "sonar.sources";
  private static final String PROJECT_TESTS_PROPERTY = "sonar.tests";
  private static final String PROJECT_BINARIES_PROPERTY = "sonar.binaries";
  private static final String PROJECT_LIBRARIES_PROPERTY = "sonar.libraries";

  private static final char SEPARATOR = ',';

  private File baseDir;
  private File workDir;
  private Properties properties;
  private List<ProjectDefinition> modules = Lists.newArrayList();

  private List<Object> containerExtensions = Lists.newArrayList();

  /**
   * @param baseDir project base directory
   * @param properties project properties
   */
  public ProjectDefinition(File baseDir, File workDir, Properties properties) {
    this.baseDir = baseDir;
    this.workDir = workDir;
    this.properties = properties;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public File getWorkDir() {
    return workDir;
  }

  public Properties getProperties() {
    return properties;
  }

  private void appendProperty(String key, String value) {
    String newValue = properties.getProperty(key, "") + SEPARATOR + value;
    properties.put(key, newValue);
  }

  public List<String> getSourceDirs() {
    String sources = properties.getProperty(PROJECT_SOURCES_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with main sources.
   *          It can be absolute or relative to project directory.
   */
  public void addSourceDir(String path) {
    appendProperty(PROJECT_SOURCES_PROPERTY, path);
  }

  public List<String> getTestDirs() {
    String sources = properties.getProperty(PROJECT_TESTS_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with test sources.
   *          It can be absolute or relative to project directory.
   */
  public void addTestDir(String path) {
    appendProperty(PROJECT_TESTS_PROPERTY, path);
  }

  public List<String> getBinaries() {
    String sources = properties.getProperty(PROJECT_BINARIES_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to directory with compiled source. In case of Java this is directory with class files.
   *          It can be absolute or relative to project directory.
   * @TODO currently Sonar supports only one such directory due to dependency on MavenProject
   */
  public void addBinaryDir(String path) {
    appendProperty(PROJECT_BINARIES_PROPERTY, path);
  }

  public List<String> getLibraries() {
    String sources = properties.getProperty(PROJECT_LIBRARIES_PROPERTY, "");
    return Arrays.asList(StringUtils.split(sources, SEPARATOR));
  }

  /**
   * @param path path to file with third-party library. In case of Java this is path to jar file.
   *          It can be absolute or relative to project directory.
   */
  public void addLibrary(String path) {
    appendProperty(PROJECT_LIBRARIES_PROPERTY, path);
  }

  /**
   * Adds an extension, which would be available in PicoContainer during analysis of this project.
   * 
   * @since 2.8
   */
  public void addContainerExtension(Object extension) {
    containerExtensions.add(extension);
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
  public void addModule(ProjectDefinition projectDefinition) {
    modules.add(projectDefinition);
  }

  /**
   * @since 2.8
   */
  public List<ProjectDefinition> getModules() {
    return modules;
  }
}
