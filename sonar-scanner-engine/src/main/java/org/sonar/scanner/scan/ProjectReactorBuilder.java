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
package org.sonar.scanner.scan;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.impl.utils.ScannerUtils;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.config.IssueExclusionProperties;
import org.sonar.scanner.bootstrap.ProcessedScannerProperties;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.sonar.api.config.internal.MultivalueProperty.parseAsCsv;

/**
 * Class that creates a project definition based on a set of properties.
 */
public class ProjectReactorBuilder {

  private static final String INVALID_VALUE_OF_X_FOR_Y = "Invalid value of {0} for {1}";

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
    PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY
  };

  /**
   * Array of all mandatory properties required for a project with children.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT = {PROPERTY_PROJECT_BASEDIR, CoreProperties.PROJECT_KEY_PROPERTY};

  /**
   * Array of all mandatory properties required for a child project before its properties get merged with its parent ones.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_CHILD = {MODULE_KEY_PROPERTY};

  private static final Collection<String> UNSUPPORTED_PROPS_FOR_MODULES = asList(IssueExclusionPatternInitializer.CONFIG_KEY, IssueInclusionPatternInitializer.CONFIG_KEY,
    IssueExclusionProperties.PATTERNS_BLOCK_KEY, IssueExclusionProperties.PATTERNS_ALLFILE_KEY);

  /**
   * Properties that must not be passed from the parent project to its children.
   */
  private static final List<String> NON_HERITED_PROPERTIES_FOR_CHILD = Stream.concat(Stream.of(PROPERTY_PROJECT_BASEDIR, CoreProperties.WORKING_DIRECTORY, PROPERTY_MODULES,
    CoreProperties.PROJECT_DESCRIPTION_PROPERTY), UNSUPPORTED_PROPS_FOR_MODULES.stream()).collect(toList());

  private final ProcessedScannerProperties scannerProps;
  private final AnalysisWarnings analysisWarnings;
  private File rootProjectWorkDir;
  private boolean warnExclusionsAlreadyLogged;

  public ProjectReactorBuilder(ProcessedScannerProperties props, AnalysisWarnings analysisWarnings) {
    this.scannerProps = props;
    this.analysisWarnings = analysisWarnings;
  }

  public ProjectReactor execute() {
    Profiler profiler = Profiler.create(LOG).startInfo("Process project properties");
    Map<String, Map<String, String>> propertiesByModuleIdPath = new HashMap<>();
    extractPropertiesByModule(propertiesByModuleIdPath, "", "", new HashMap<>(scannerProps.properties()));
    ProjectDefinition rootProject = createModuleDefinition(propertiesByModuleIdPath.get(""), null);
    rootProjectWorkDir = rootProject.getWorkDir();
    defineChildren(rootProject, propertiesByModuleIdPath, "");
    cleanAndCheckProjectDefinitions(rootProject);
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

  protected ProjectDefinition createModuleDefinition(Map<String, String> moduleProperties, @Nullable ProjectDefinition parent) {
    if (moduleProperties.containsKey(PROPERTY_MODULES)) {
      checkMandatoryProperties(moduleProperties, MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT);
    } else {
      checkMandatoryProperties(moduleProperties, MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT);
    }
    File baseDir = new File(moduleProperties.get(PROPERTY_PROJECT_BASEDIR));
    final String projectKey = moduleProperties.get(CoreProperties.PROJECT_KEY_PROPERTY);
    File workDir;
    if (parent == null) {
      validateDirectories(moduleProperties, baseDir, projectKey);
      workDir = initRootProjectWorkDir(baseDir, moduleProperties);
    } else {
      workDir = initModuleWorkDir(baseDir, moduleProperties);
      checkUnsupportedIssueExclusions(moduleProperties, parent.properties());
    }

    return ProjectDefinition.create().setProperties(moduleProperties)
      .setBaseDir(baseDir)
      .setWorkDir(workDir)
      .setBuildDir(initModuleBuildDir(baseDir, moduleProperties));
  }

  private void checkUnsupportedIssueExclusions(Map<String, String> moduleProperties, Map<String, String> parentProps) {
    UNSUPPORTED_PROPS_FOR_MODULES.stream().forEach(p -> {
      if (moduleProperties.containsKey(p) && !Objects.equals(moduleProperties.get(p), parentProps.get(p))) {
        warnOnceUnsupportedIssueExclusions(
          "Specifying issue exclusions at module level is not supported anymore. Configure the property '" + p + "' and any other issue exclusions at project level.");
      }
    });
  }

  private void warnOnceUnsupportedIssueExclusions(String msg) {
    if (!warnExclusionsAlreadyLogged) {
      LOG.warn(msg);
      analysisWarnings.addUnique(msg);
      warnExclusionsAlreadyLogged = true;
    }
  }

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

    return createModuleDefinition(moduleProps, parentProject);
  }

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

  protected static void cleanAndCheckModuleProperties(ProjectDefinition project) {
    Map<String, String> properties = project.properties();

    // We need to check the existence of source directories
    String[] sourcePaths = getListFromProperty(properties, PROPERTY_SOURCES);
    checkExistenceOfPaths(project.getKey(), project.getBaseDir(), sourcePaths, PROPERTY_SOURCES);
  }

  protected static void mergeParentProperties(Map<String, String> childProps, Map<String, String> parentProps) {
    for (Map.Entry<String, String> entry : parentProps.entrySet()) {
      String key = entry.getKey();
      if ((!childProps.containsKey(key) || childProps.get(key).equals(entry.getValue()))
        && !NON_HERITED_PROPERTIES_FOR_CHILD.contains(key)) {
        childProps.put(entry.getKey(), entry.getValue());
      }
    }
  }

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
   * <p>
   * This works even if they are separated by whitespace characters (space char, EOL, ...)
   */
  static String[] getListFromProperty(Map<String, String> properties, String key) {
    String propValue = properties.get(key);
    if (propValue != null) {
      return parseAsCsv(key, propValue);
    }
    return new String[0];
  }

}
