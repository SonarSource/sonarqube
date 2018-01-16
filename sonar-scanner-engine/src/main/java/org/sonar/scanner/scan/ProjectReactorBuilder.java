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
package org.sonar.scanner.scan;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.analysis.AnalysisProperties;
import org.sonar.scanner.bootstrap.DroppedPropertyChecker;
import org.sonar.scanner.util.ScannerUtils;

import static org.sonar.core.config.MultivalueProperty.parseAsCsv;

/**
 * Class that creates a project definition based on a set of properties.
 */
public class ProjectReactorBuilder {

  private static final String INVALID_VALUE_OF_X_FOR_Y = "Invalid value of {0} for {1}";

  /**
   * A map of dropped properties as key and specific message to display for that property
   * (what will happen, what should the user do, ...) as a value
   */
  private static final Map<String, String> DROPPED_PROPERTIES = ImmutableMap.of(
    "sonar.qualitygate", "It will be ignored.");

  private static final Logger LOG = Loggers.get(ProjectReactorBuilder.class);

  /**
   * @since 4.1 but not yet exposed in {@link CoreProperties}
   */
  private static final String MODULE_KEY_PROPERTY = "sonar.moduleKey";

  protected static final String PROPERTY_PROJECT_BASEDIR = "sonar.projectBaseDir";
  /**
   * @deprecated since 6.1 notion of buildDir is not well defined
   */
  @Deprecated
  private static final String PROPERTY_PROJECT_BUILDDIR = "sonar.projectBuildDir";
  private static final String PROPERTY_MODULES = "sonar.modules";

  /**
   * New properties, to be consistent with Sonar naming conventions
   *
   * @since 1.5
   */
  private static final String PROPERTY_SOURCES = ProjectDefinition.SOURCES_PROPERTY;
  private static final String PROPERTY_TESTS = ProjectDefinition.TESTS_PROPERTY;

  /**
   * Array of all mandatory properties required for a project without child.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT = {
    PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY, PROPERTY_SOURCES
  };

  /**
   * Array of all mandatory properties required for a project with children.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT = {PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY};

  /**
   * Array of all mandatory properties required for a child project before its properties get merged with its parent ones.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_CHILD = {MODULE_KEY_PROPERTY};

  /**
   * Properties that must not be passed from the parent project to its children.
   */
  private static final List<String> NON_HERITED_PROPERTIES_FOR_CHILD = Lists.newArrayList(PROPERTY_PROJECT_BASEDIR, CoreProperties.WORKING_DIRECTORY, PROPERTY_MODULES,
    CoreProperties.PROJECT_DESCRIPTION_PROPERTY);

  private final AnalysisProperties analysisProps;
  private File rootProjectWorkDir;

  public ProjectReactorBuilder(AnalysisProperties props) {
    this.analysisProps = props;
  }

  public ProjectReactor execute() {
    Profiler profiler = Profiler.create(LOG).startInfo("Process project properties");
    new DroppedPropertyChecker(analysisProps.properties(), DROPPED_PROPERTIES).checkDroppedProperties();
    Map<String, Map<String, String>> propertiesByModuleIdPath = new HashMap<>();
    extractPropertiesByModule(propertiesByModuleIdPath, "", "", analysisProps.properties());
    ProjectDefinition rootProject = defineRootProject(propertiesByModuleIdPath.get(""), null);
    rootProjectWorkDir = rootProject.getWorkDir();
    defineChildren(rootProject, propertiesByModuleIdPath, "");
    cleanAndCheckProjectDefinitions(rootProject);
    // Since task properties are now empty we should add root module properties
    analysisProps.properties().putAll(propertiesByModuleIdPath.get(""));
    profiler.stopDebug();
    return new ProjectReactor(rootProject);
  }

  private static void extractPropertiesByModule(Map<String, Map<String, String>> propertiesByModuleIdPath, String currentModuleId, String currentModuleIdPath,
    Map<String, String> parentProperties) {
    if (propertiesByModuleIdPath.containsKey(currentModuleIdPath)) {
      throw MessageException.of(String.format("Two modules have the same id: '%s'. Each module must have a unique id.", currentModuleId));
    }

    Map<String, String> currentModuleProperties = new HashMap<>();
    String prefix = !currentModuleId.isEmpty() ? (currentModuleId + ".") : "";
    int prefixLength = prefix.length();

    // By default all properties starting with module prefix belong to current module
    Iterator<Entry<String, String>> it = parentProperties.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> e = it.next();
      String key = e.getKey();
      if (key.startsWith(prefix)) {
        currentModuleProperties.put(key.substring(prefixLength), e.getValue());
        it.remove();
      }
    }
    String[] moduleIds = getListFromProperty(currentModuleProperties, PROPERTY_MODULES);
    // Sort modules by reverse lexicographic order to avoid issue when one module id is a prefix of another one
    Arrays.sort(moduleIds);
    ArrayUtils.reverse(moduleIds);

    propertiesByModuleIdPath.put(currentModuleIdPath, currentModuleProperties);

    for (String moduleId : moduleIds) {
      if ("sonar".equals(moduleId)) {
        throw MessageException.of("'sonar' is not a valid module id. Please check property '" + PROPERTY_MODULES + "'.");
      }
      String subModuleIdPath = currentModuleIdPath.isEmpty() ? moduleId : (currentModuleIdPath + "." + moduleId);
      extractPropertiesByModule(propertiesByModuleIdPath, moduleId, subModuleIdPath, currentModuleProperties);
    }
  }

  protected ProjectDefinition defineRootProject(Map<String, String> rootProperties, @Nullable ProjectDefinition parent) {
    if (rootProperties.containsKey(PROPERTY_MODULES)) {
      checkMandatoryProperties(rootProperties, MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT);
    } else {
      checkMandatoryProperties(rootProperties, MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT);
    }
    File baseDir = new File(rootProperties.get(PROPERTY_PROJECT_BASEDIR));
    final String projectKey = rootProperties.get(CoreProperties.PROJECT_KEY_PROPERTY);
    File workDir;
    if (parent == null) {
      validateDirectories(rootProperties, baseDir, projectKey);
      workDir = initRootProjectWorkDir(baseDir, rootProperties);
    } else {
      workDir = initModuleWorkDir(baseDir, rootProperties);
    }

    return ProjectDefinition.create().setProperties(rootProperties)
      .setBaseDir(baseDir)
      .setWorkDir(workDir)
      .setBuildDir(initModuleBuildDir(baseDir, rootProperties));
  }

  @VisibleForTesting
  protected File initRootProjectWorkDir(File baseDir, Map<String, String> rootProperties) {
    String workDir = rootProperties.get(CoreProperties.WORKING_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      return new File(baseDir, CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE);
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(baseDir, customWorkDir.getPath());
  }

  @VisibleForTesting
  protected File initModuleWorkDir(File moduleBaseDir, Map<String, String> moduleProperties) {
    String workDir = moduleProperties.get(CoreProperties.WORKING_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      return new File(rootProjectWorkDir, ScannerUtils.cleanKeyForFilename(moduleProperties.get(CoreProperties.PROJECT_KEY_PROPERTY)));
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(moduleBaseDir, customWorkDir.getPath());
  }

  @CheckForNull
  private static File initModuleBuildDir(File moduleBaseDir, Map<String, String> moduleProperties) {
    String buildDir = moduleProperties.get(PROPERTY_PROJECT_BUILDDIR);
    if (StringUtils.isBlank(buildDir)) {
      return null;
    }

    File customBuildDir = new File(buildDir);
    if (customBuildDir.isAbsolute()) {
      return customBuildDir;
    }
    return new File(moduleBaseDir, customBuildDir.getPath());
  }

  private void defineChildren(ProjectDefinition parentProject, Map<String, Map<String, String>> propertiesByModuleIdPath, String parentModuleIdPath) {
    Map<String, String> parentProps = parentProject.properties();
    if (parentProps.containsKey(PROPERTY_MODULES)) {
      for (String moduleId : getListFromProperty(parentProps, PROPERTY_MODULES)) {
        String moduleIdPath = parentModuleIdPath.isEmpty() ? moduleId : (parentModuleIdPath + "." + moduleId);
        Map<String, String> moduleProps = propertiesByModuleIdPath.get(moduleIdPath);
        ProjectDefinition childProject = loadChildProject(parentProject, moduleProps, moduleId);
        // check the uniqueness of the child key
        checkUniquenessOfChildKey(childProject, parentProject);
        // the child project may have children as well
        defineChildren(childProject, propertiesByModuleIdPath, moduleIdPath);
        // and finally add this child project to its parent
        parentProject.addSubProject(childProject);
      }
    }
  }

  protected ProjectDefinition loadChildProject(ProjectDefinition parentProject, Map<String, String> moduleProps, String moduleId) {
    final File baseDir;
    if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
      baseDir = resolvePath(parentProject.getBaseDir(), moduleProps.get(PROPERTY_PROJECT_BASEDIR));
      setProjectBaseDir(baseDir, moduleProps, moduleId);
    } else {
      baseDir = new File(parentProject.getBaseDir(), moduleId);
      setProjectBaseDir(baseDir, moduleProps, moduleId);
    }

    setModuleKeyAndNameIfNotDefined(moduleProps, moduleId, parentProject.getKey());

    // and finish
    checkMandatoryProperties(moduleProps, MANDATORY_PROPERTIES_FOR_CHILD);
    validateDirectories(moduleProps, baseDir, moduleId);

    mergeParentProperties(moduleProps, parentProject.properties());

    return defineRootProject(moduleProps, parentProject);
  }

  @VisibleForTesting
  protected static void setModuleKeyAndNameIfNotDefined(Map<String, String> childProps, String moduleId, String parentKey) {
    if (!childProps.containsKey(MODULE_KEY_PROPERTY)) {
      if (!childProps.containsKey(CoreProperties.PROJECT_KEY_PROPERTY)) {
        childProps.put(MODULE_KEY_PROPERTY, parentKey + ":" + moduleId);
      } else {
        String childKey = childProps.get(CoreProperties.PROJECT_KEY_PROPERTY);
        childProps.put(MODULE_KEY_PROPERTY, parentKey + ":" + childKey);
      }
    }
    if (!childProps.containsKey(CoreProperties.PROJECT_NAME_PROPERTY)) {
      childProps.put(CoreProperties.PROJECT_NAME_PROPERTY, moduleId);
    }
    // For backward compatibility with ProjectDefinition
    childProps.put(CoreProperties.PROJECT_KEY_PROPERTY, childProps.get(MODULE_KEY_PROPERTY));
  }

  @VisibleForTesting
  protected static void checkUniquenessOfChildKey(ProjectDefinition childProject, ProjectDefinition parentProject) {
    for (ProjectDefinition definition : parentProject.getSubProjects()) {
      if (definition.getKey().equals(childProject.getKey())) {
        throw MessageException.of("Project '" + parentProject.getKey() + "' can't have 2 modules with the following key: " + childProject.getKey());
      }
    }
  }

  protected static void setProjectBaseDir(File baseDir, Map<String, String> childProps, String moduleId) {
    if (!baseDir.isDirectory()) {
      throw MessageException.of("The base directory of the module '" + moduleId + "' does not exist: " + baseDir.getAbsolutePath());
    }
    childProps.put(PROPERTY_PROJECT_BASEDIR, baseDir.getAbsolutePath());
  }

  @VisibleForTesting
  protected static void checkMandatoryProperties(Map<String, String> props, String[] mandatoryProps) {
    StringBuilder missing = new StringBuilder();
    for (String mandatoryProperty : mandatoryProps) {
      if (!props.containsKey(mandatoryProperty)) {
        if (missing.length() > 0) {
          missing.append(", ");
        }
        missing.append(mandatoryProperty);
      }
    }
    String moduleKey = StringUtils.defaultIfBlank(props.get(MODULE_KEY_PROPERTY), props.get(CoreProperties.PROJECT_KEY_PROPERTY));
    if (missing.length() != 0) {
      throw MessageException.of("You must define the following mandatory properties for '" + (moduleKey == null ? "Unknown" : moduleKey) + "': " + missing);
    }
  }

  protected static void validateDirectories(Map<String, String> props, File baseDir, String projectId) {
    if (!props.containsKey(PROPERTY_MODULES)) {
      // SONARPLUGINS-2285 Not an aggregator project so we can validate that paths are correct if defined

      // Check sonar.tests
      String[] testPaths = getListFromProperty(props, PROPERTY_TESTS);
      checkExistenceOfPaths(projectId, baseDir, testPaths, PROPERTY_TESTS);
    }
  }

  @VisibleForTesting
  protected static void cleanAndCheckProjectDefinitions(ProjectDefinition project) {
    if (project.getSubProjects().isEmpty()) {
      cleanAndCheckModuleProperties(project);
    } else {
      logMissingSourcesAndTests(project);

      // clean modules properties as well
      for (ProjectDefinition module : project.getSubProjects()) {
        cleanAndCheckProjectDefinitions(module);
      }
    }
  }

  private static void logMissingSourcesAndTests(ProjectDefinition project) {
    Map<String, String> properties = project.properties();

    File baseDir = project.getBaseDir();
    logMissingPaths("source", baseDir, getListFromProperty(properties, PROPERTY_SOURCES));
    logMissingPaths("test", baseDir, getListFromProperty(properties, PROPERTY_TESTS));
  }

  private static void logMissingPaths(String label, File baseDir, String[] paths) {
    for (String path : paths) {
      File file = resolvePath(baseDir, path);
      if (!file.exists()) {
        LOG.debug("Path '{}' does not exist, will not be used as {}", file, label);
      }
    }
  }

  @VisibleForTesting
  protected static void cleanAndCheckModuleProperties(ProjectDefinition project) {
    Map<String, String> properties = project.properties();

    // We need to check the existence of source directories
    String[] sourcePaths = getListFromProperty(properties, PROPERTY_SOURCES);
    checkExistenceOfPaths(project.getKey(), project.getBaseDir(), sourcePaths, PROPERTY_SOURCES);
  }

  @VisibleForTesting
  protected static void mergeParentProperties(Map<String, String> childProps, Map<String, String> parentProps) {
    for (Map.Entry<String, String> entry : parentProps.entrySet()) {
      String key = entry.getKey();
      if ((!childProps.containsKey(key) || childProps.get(key).equals(entry.getValue()))
        && !NON_HERITED_PROPERTIES_FOR_CHILD.contains(key)) {
        childProps.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @VisibleForTesting
  protected static void checkExistenceOfPaths(String moduleRef, File baseDir, String[] paths, String propName) {
    for (String path : paths) {
      File sourceFolder = resolvePath(baseDir, path);
      if (!sourceFolder.exists()) {
        LOG.error(MessageFormat.format(INVALID_VALUE_OF_X_FOR_Y, propName, moduleRef));
        throw MessageException.of("The folder '" + path + "' does not exist for '" + moduleRef +
          "' (base directory = " + baseDir.getAbsolutePath() + ")");
      }
    }
  }

  protected static File resolvePath(File baseDir, String path) {
    Path filePath = Paths.get(path);
    if (!filePath.isAbsolute()) {
      filePath = baseDir.toPath().resolve(path);
    }
    return filePath.normalize().toFile();
  }

  /**
   * Transforms a comma-separated list String property in to a array of trimmed strings.
   *
   * This works even if they are separated by whitespace characters (space char, EOL, ...)
   *
   */
  static String[] getListFromProperty(Map<String, String> properties, String key) {
    String propValue = properties.get(key);
    if (propValue != null) {
      return parseAsCsv(key, propValue);
    }
    return new String[0];
  }

}
