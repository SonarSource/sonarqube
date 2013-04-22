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
package org.sonar.batch.bootstrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Describes project in a form suitable to bootstrap Sonar batch.
 * We assume that project is just a set of configuration properties and directories.
 *
 * @since 2.6
 * @deprecated since 2.9. Move into org.sonar.api.batch.bootstrap
 */
@Deprecated
public class ProjectDefinition {

  private org.sonar.api.batch.bootstrap.ProjectDefinition target = null;
  private List<ProjectDefinition> children = new ArrayList<ProjectDefinition>();

  /**
   * @param baseDir    project base directory
   * @param properties project properties
   */
  public ProjectDefinition(File baseDir, File workDir, Properties properties) {
    target = org.sonar.api.batch.bootstrap.ProjectDefinition.create(properties)
        .setBaseDir(baseDir)
        .setWorkDir(workDir);
  }

  public File getBaseDir() {
    return target.getBaseDir();
  }

  public File getWorkDir() {
    return target.getWorkDir();
  }

  public Properties getProperties() {
    return target.getProperties();
  }

  public List<String> getSourceDirs() {
    return target.getSourceDirs();
  }

  public void addSourceDir(String path) {
    target.addSourceDirs(path);
  }

  public List<String> getTestDirs() {
    return target.getTestDirs();
  }

  /**
   * @param path path to directory with test sources.
   *             It can be absolute or relative to project directory.
   */
  public void addTestDir(String path) {
    target.addTestDirs(path);
  }

  public List<String> getBinaries() {
    return target.getBinaries();
  }

  /**
   * @param path path to directory with compiled source. In case of Java this is directory with class files.
   *             It can be absolute or relative to project directory.
   * @TODO currently Sonar supports only one such directory due to dependency on MavenProject
   */
  public void addBinaryDir(String path) {
    target.addBinaryDir(path);
  }

  public List<String> getLibraries() {
    return target.getLibraries();
  }

  /**
   * @param path path to file with third-party library. In case of Java this is path to jar file.
   *             It can be absolute or relative to project directory.
   */
  public void addLibrary(String path) {
    target.addLibrary(path);
  }

  /**
   * Adds an extension, which would be available in PicoContainer during analysis of this project.
   *
   * @since 2.8
   */
  public void addContainerExtension(Object extension) {
    target.addContainerExtension(extension);
  }

  /**
   * @since 2.8
   */
  public List<Object> getContainerExtensions() {
    return target.getContainerExtensions();
  }

  /**
   * @since 2.8
   */
  public void addModule(ProjectDefinition projectDefinition) {
    target.addSubProject(projectDefinition.toNewProjectDefinition());
    children.add(projectDefinition);
  }

  /**
   * @since 2.8
   */
  public List<ProjectDefinition> getModules() {
    return children;
  }

  public org.sonar.api.batch.bootstrap.ProjectDefinition toNewProjectDefinition() {
    return target;
  }
}
