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

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.sonar.api.project.ProjectDefinition;
import org.sonar.api.project.ProjectDirectory;

import java.io.File;
import java.util.List;

public class DefaultProjectDefinition implements ProjectDefinition {

  private String key;
  private Configuration configuration;
  private File sonarWorkingDirectory;
  private File basedir;
  private List<ProjectDirectory> dirs = Lists.newArrayList();
  private DefaultProjectDefinition parent;
  private List<ProjectDefinition> modules;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public File getSonarWorkingDirectory() {
    return sonarWorkingDirectory;
  }

  public void setSonarWorkingDirectory(File sonarWorkingDirectory) {
    this.sonarWorkingDirectory = sonarWorkingDirectory;
  }

  public File getBasedir() {
    return basedir;
  }

  public void setBasedir(File basedir) {
    this.basedir = basedir;
  }

  public List<ProjectDirectory> getDirs() {
    return dirs;
  }

  public void addDir(ProjectDirectory dir) {
    this.dirs.add(dir);
  }

  public ProjectDefinition getParent() {
    return parent;
  }

  public void setParent(DefaultProjectDefinition parent) {
    this.parent = parent;
    if (parent != null) {
      parent.modules.add(this);
    }
  }

  public List<ProjectDefinition> getModules() {
    return modules;
  }

}
