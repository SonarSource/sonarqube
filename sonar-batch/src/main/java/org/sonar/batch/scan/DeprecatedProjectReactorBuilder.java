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
package org.sonar.batch.scan;

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.batch.bootstrap.TaskProperties;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Class that creates a project definition based on a set of properties. This class is intended to work with
 * SQ Runner <= 2.3. Starting from SQ Runner 2.4 loading of sonar-project.properties file should not be done by
 * the core.
 * @deprecated since 4.3 should be removed when we will require SQ Runner 2.4+
 */
@Deprecated
public class DeprecatedProjectReactorBuilder extends ProjectReactorBuilder {

  private static final String PROPERTY_PROJECT_CONFIG_FILE = "sonar.projectConfigFile";

  public DeprecatedProjectReactorBuilder(TaskProperties props) {
    super(props);
  }

  @Override
  protected ProjectDefinition loadChildProject(ProjectDefinition parentProject, Map<String, String> moduleProps, String moduleId) {
    final File baseDir;
    if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
      baseDir = resolvePath(parentProject.getBaseDir(), moduleProps.get(PROPERTY_PROJECT_BASEDIR));
      setProjectBaseDir(baseDir, moduleProps, moduleId);
      try {
        if (!parentProject.getBaseDir().getCanonicalFile().equals(baseDir.getCanonicalFile())) {
          tryToFindAndLoadPropsFile(baseDir, moduleProps, moduleId);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Error when resolving baseDir", e);
      }
    } else if (moduleProps.containsKey(PROPERTY_PROJECT_CONFIG_FILE)) {
      baseDir = loadPropsFile(parentProject, moduleProps, moduleId);
    } else {
      baseDir = new File(parentProject.getBaseDir(), moduleId);
      setProjectBaseDir(baseDir, moduleProps, moduleId);
      tryToFindAndLoadPropsFile(baseDir, moduleProps, moduleId);
    }

    setModuleKeyAndNameIfNotDefined(moduleProps, moduleId, parentProject.getKey());

    // and finish
    checkMandatoryProperties(moduleProps, MANDATORY_PROPERTIES_FOR_CHILD);
    validateDirectories(moduleProps, baseDir, moduleId);

    mergeParentProperties(moduleProps, parentProject.properties());

    return defineRootProject(moduleProps, parentProject);
  }

  /**
   * @return baseDir
   */
  private File loadPropsFile(ProjectDefinition parentProject, Map<String, String> moduleProps, String moduleId) {
    File propertyFile = resolvePath(parentProject.getBaseDir(), moduleProps.get(PROPERTY_PROJECT_CONFIG_FILE));
    if (propertyFile.isFile()) {
      Properties propsFromFile = toProperties(propertyFile);
      for (Entry<Object, Object> entry : propsFromFile.entrySet()) {
        moduleProps.put(entry.getKey().toString(), entry.getValue().toString());
      }
      File baseDir = null;
      if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
        baseDir = resolvePath(propertyFile.getParentFile(), moduleProps.get(PROPERTY_PROJECT_BASEDIR));
      } else {
        baseDir = propertyFile.getParentFile();
      }
      setProjectBaseDir(baseDir, moduleProps, moduleId);
      return baseDir;
    } else {
      throw new IllegalStateException("The properties file of the module '" + moduleId + "' does not exist: " + propertyFile.getAbsolutePath());
    }
  }

  private void tryToFindAndLoadPropsFile(File baseDir, Map<String, String> moduleProps, String moduleId) {
    File propertyFile = new File(baseDir, "sonar-project.properties");
    if (propertyFile.isFile()) {
      Properties propsFromFile = toProperties(propertyFile);
      for (Entry<Object, Object> entry : propsFromFile.entrySet()) {
        moduleProps.put(entry.getKey().toString(), entry.getValue().toString());
      }
      if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
        File overwrittenBaseDir = resolvePath(propertyFile.getParentFile(), moduleProps.get(PROPERTY_PROJECT_BASEDIR));
        setProjectBaseDir(overwrittenBaseDir, moduleProps, moduleId);
      }
    }
  }

}
