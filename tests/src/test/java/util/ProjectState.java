/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util;

import java.io.File;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkState;
import static util.LoadedProjects.SONAR_PROJECT_PROPERTIES_FILE_NAME;

final class ProjectState {
  private static final String SONAR_PROJECT_KEY_PROPERTY_NAME = "sonar.projectKey";
  private static final String SONAR_PROJECT_NAME_PROPERTY_NAME = "sonar.projectName";

  private final File projectDir;
  private final Properties properties;
  private boolean provisioned = false;

  ProjectState(File projectDir, Properties properties) {
    this.projectDir = projectDir;
    this.properties = properties;
  }

  public File getProjectDir() {
    return projectDir;
  }

  public Properties getProperties() {
    return properties;
  }

  public String getProjectKey() {
    return getProperty(SONAR_PROJECT_KEY_PROPERTY_NAME);
  }

  public String getProjectName() {
    return getProperty(SONAR_PROJECT_NAME_PROPERTY_NAME);
  }

  private String getProperty(String propertyName) {
    String value = this.properties.getProperty(propertyName);
    checkState(value != null, "Property %s is missing in %s file in project directory %s",
      propertyName, SONAR_PROJECT_PROPERTIES_FILE_NAME, projectDir.getAbsolutePath());
    return value;
  }

  public boolean isProvisioned() {
    return provisioned;
  }

  public void setProvisioned(boolean provisioned) {
    this.provisioned = provisioned;
  }
}
