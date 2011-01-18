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
package org.sonar.batch.bootstrapper;

import org.apache.commons.configuration.Configuration;

import java.io.File;

/**
 * Defines project in a form suitable to bootstrap Sonar batch.
 * We assume that project is just a set of configuration properties and directories.
 * This is a part of bootstrap process, so we should take care about backward compatibility.
 * 
 * @since 2.6
 */
public class ProjectDefinition {

  private File baseDir;
  private Configuration properties;

  /**
   * @param baseDir project base directory
   * @param properties project properties
   */
  public ProjectDefinition(File baseDir, Configuration properties) {
    this.baseDir = baseDir;
    this.properties = properties;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public Configuration getProperties() {
    return properties;
  }

}
