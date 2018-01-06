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

import com.google.common.base.Throwables;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

final class LoadedProjects {

  static final String SONAR_PROJECT_PROPERTIES_FILE_NAME = "sonar-project.properties";

  private final Map<String, ProjectState> projectStatePerProjectKey = new HashMap<>();
  private final Set<String> knownProjects = new HashSet<>();

  public void reset() {
    this.projectStatePerProjectKey.clear();
    this.knownProjects.clear();
  }

  public String load(String projectRelativePath) {
    checkState(!knownProjects.contains(projectRelativePath), "Project at location %s already loaded", projectRelativePath);

    File projectDir = ItUtils.projectDir(projectRelativePath);
    Properties sonarProjectProperties = loadProjectProperties(projectDir);
    ProjectState projectState = new ProjectState(projectDir, sonarProjectProperties);

    register(projectRelativePath, projectState);

    return projectState.getProjectKey();
  }

  public ProjectState getProjectState(String projectKey) {
    ProjectState projectState = this.projectStatePerProjectKey.get(projectKey);
    checkArgument(projectState != null, "Project with key %s is unknown to %s", projectKey, ProjectAnalysisRule.class.getSimpleName());
    return projectState;
  }

  private void register(String projectRelativePath, ProjectState projectState) {
    this.projectStatePerProjectKey.put(projectState.getProjectKey(), projectState);
    this.knownProjects.add(projectRelativePath);
  }

  private static Properties loadProjectProperties(File projectDir) {
    File sonarPropertiesFile = new File(projectDir, SONAR_PROJECT_PROPERTIES_FILE_NAME);
    checkArgument(sonarPropertiesFile.exists(), "Can not locate %s in project %s", SONAR_PROJECT_PROPERTIES_FILE_NAME, projectDir.getAbsolutePath());

    Properties properties = new Properties();
    try {
      properties.load(new FileReader(sonarPropertiesFile));
    } catch (IOException e) {
      Throwables.propagate(e);
    }
    return properties;
  }
}
