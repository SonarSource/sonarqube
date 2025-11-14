/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalServerSettings;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.scan.ModuleConfiguration;
import org.sonar.scanner.scan.ModuleConfigurationProvider;
import org.sonar.scanner.scan.ProjectServerSettings;
import org.sonar.scanner.scan.SonarGlobalPropertiesFilter;
import org.sonar.scanner.util.ProgressReport;

/**
 * Index project input files into {@link InputComponentStore}.
 */
public class ProjectFileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectFileIndexer.class);
  private final ProjectExclusionFilters projectExclusionFilters;
  private final SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter;
  private final ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions;
  private final InputComponentStore componentStore;
  private final InputModuleHierarchy inputModuleHierarchy;
  private final GlobalConfiguration globalConfig;
  private final GlobalServerSettings globalServerSettings;
  private final ProjectServerSettings projectServerSettings;
  private final FileIndexer fileIndexer;
  private final ProjectFilePreprocessor projectFilePreprocessor;
  private final AnalysisWarnings analysisWarnings;
  private final HiddenFilesProjectData hiddenFilesProjectData;

  private ProgressReport progressReport;

  public ProjectFileIndexer(InputComponentStore componentStore, ProjectExclusionFilters exclusionFilters,
    SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter, InputModuleHierarchy inputModuleHierarchy,
    GlobalConfiguration globalConfig, GlobalServerSettings globalServerSettings, ProjectServerSettings projectServerSettings,
    FileIndexer fileIndexer, ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions,
    ProjectFilePreprocessor projectFilePreprocessor, AnalysisWarnings analysisWarnings, HiddenFilesProjectData hiddenFilesProjectData) {
    this.componentStore = componentStore;
    this.sonarGlobalPropertiesFilter = sonarGlobalPropertiesFilter;
    this.inputModuleHierarchy = inputModuleHierarchy;
    this.globalConfig = globalConfig;
    this.globalServerSettings = globalServerSettings;
    this.projectServerSettings = projectServerSettings;
    this.fileIndexer = fileIndexer;
    this.projectExclusionFilters = exclusionFilters;
    this.projectCoverageAndDuplicationExclusions = projectCoverageAndDuplicationExclusions;
    this.projectFilePreprocessor = projectFilePreprocessor;
    this.analysisWarnings = analysisWarnings;
    this.hiddenFilesProjectData = hiddenFilesProjectData;
  }

  public void index() {
    progressReport = new ProgressReport("Report about progress of file indexing", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Indexing files...");
    LOG.info("Project configuration:");
    projectExclusionFilters.log("  ");
    projectCoverageAndDuplicationExclusions.log("  ");

    indexModulesRecursively(inputModuleHierarchy.root());
    hiddenFilesProjectData.clearHiddenFilesData();

    int totalIndexed = componentStore.inputFiles().size();
    progressReport.stopAndLogTotalTime(totalIndexed + " " + pluralizeFiles(totalIndexed) + " indexed");
  }

  private void indexModulesRecursively(DefaultInputModule module) {
    inputModuleHierarchy.children(module).stream()
      .sorted(Comparator.comparing(DefaultInputModule::key))
      .forEach(this::indexModulesRecursively);
    index(module);
  }

  private void index(DefaultInputModule module) {
    // Emulate creation of module level settings
    ModuleConfiguration moduleConfig = new ModuleConfigurationProvider(sonarGlobalPropertiesFilter).provide(globalConfig, module, globalServerSettings, projectServerSettings);
    ModuleExclusionFilters moduleExclusionFilters = new ModuleExclusionFilters(moduleConfig, analysisWarnings);
    ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions = new ModuleCoverageAndDuplicationExclusions(moduleConfig);
    if (componentStore.allModules().size() > 1) {
      LOG.info("Indexing files of module '{}'", module.getName());
      LOG.info("  Base dir: {}", module.getBaseDir().toAbsolutePath());
      module.getSourceDirsOrFiles().ifPresent(srcs -> logPaths("  Source paths: ", module.getBaseDir(), srcs));
      module.getTestDirsOrFiles().ifPresent(tests -> logPaths("  Test paths: ", module.getBaseDir(), tests));
      moduleExclusionFilters.log("  ");
      moduleCoverageAndDuplicationExclusions.log("  ");
    }
    List<Path> mainSourceDirsOrFiles = projectFilePreprocessor.getMainSourcesByModule(module);
    indexFiles(module, moduleConfig, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, mainSourceDirsOrFiles, Type.MAIN);
    projectFilePreprocessor.getTestSourcesByModule(module)
      .ifPresent(tests -> indexFiles(module, moduleConfig, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, tests, Type.TEST));
  }

  private static void logPaths(String label, Path baseDir, List<Path> paths) {
    if (!paths.isEmpty()) {
      StringBuilder sb = new StringBuilder(label);
      for (Iterator<Path> it = paths.iterator(); it.hasNext();) {
        Path file = it.next();
        Optional<String> relativePathToBaseDir = PathResolver.relativize(baseDir, file);
        if (relativePathToBaseDir.isEmpty()) {
          sb.append(file);
        } else if (StringUtils.isBlank(relativePathToBaseDir.get())) {
          sb.append(".");
        } else {
          sb.append(relativePathToBaseDir.get());
        }
        if (it.hasNext()) {
          sb.append(", ");
        }
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(sb.toString());
      } else {
        LOG.info(StringUtils.abbreviate(sb.toString(), 80));
      }
    }
  }

  private void indexFiles(DefaultInputModule module, ModuleConfiguration moduleConfig, ModuleExclusionFilters moduleExclusionFilters,
    ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions,
    List<Path> sources, Type type) {
    try {
      for (Path dirOrFile : sources) {
        if (dirOrFile.toFile().isDirectory()) {
          indexDirectory(module, moduleConfig, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, dirOrFile, type);
        } else {
          fileIndexer.indexFile(module, moduleCoverageAndDuplicationExclusions, dirOrFile, type, progressReport);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to index files", e);
    }
  }

  private void indexDirectory(DefaultInputModule module, ModuleConfiguration moduleConfig, ModuleExclusionFilters moduleExclusionFilters,
    ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions,
    Path dirToIndex, Type type) throws IOException {
    Files.walkFileTree(dirToIndex.normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new DirectoryFileVisitor(file -> fileIndexer.indexFile(module, moduleCoverageAndDuplicationExclusions, file, type, progressReport),
        module, moduleConfig, moduleExclusionFilters, inputModuleHierarchy, type, hiddenFilesProjectData));
  }

  private static String pluralizeFiles(int count) {
    return count == 1 ? "file" : "files";
  }

}
