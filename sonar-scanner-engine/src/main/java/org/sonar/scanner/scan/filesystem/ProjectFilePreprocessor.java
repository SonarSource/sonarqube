/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalServerSettings;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.scan.ModuleConfiguration;
import org.sonar.scanner.scan.ModuleConfigurationProvider;
import org.sonar.scanner.scan.ProjectServerSettings;
import org.sonar.scanner.scan.SonarGlobalPropertiesFilter;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.util.ProgressReport;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class ProjectFilePreprocessor {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectFilePreprocessor.class);
  private static final String TELEMETRY_STEP_NAME = "file.preprocessing";

  private final AnalysisWarnings analysisWarnings;
  private final IgnoreCommand ignoreCommand;
  private final boolean useScmExclusion;
  private final ScmConfiguration scmConfiguration;
  private final InputModuleHierarchy inputModuleHierarchy;
  private final GlobalConfiguration globalConfig;
  private final GlobalServerSettings globalServerSettings;
  private final ProjectServerSettings projectServerSettings;
  private final LanguageDetection languageDetection;
  private final FilePreprocessor filePreprocessor;
  private final ProjectExclusionFilters projectExclusionFilters;

  private final SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter;

  private final Map<DefaultInputModule, List<Path>> mainSourcesByModule = new HashMap<>();
  private final Map<DefaultInputModule, List<Path>> testSourcesByModule = new HashMap<>();

  private int totalFilesPreprocessed = 0;

  public ProjectFilePreprocessor(AnalysisWarnings analysisWarnings, ScmConfiguration scmConfiguration, InputModuleHierarchy inputModuleHierarchy,
    GlobalConfiguration globalConfig, GlobalServerSettings globalServerSettings, ProjectServerSettings projectServerSettings,
    LanguageDetection languageDetection, FilePreprocessor filePreprocessor,
    ProjectExclusionFilters projectExclusionFilters, SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter) {
    this.analysisWarnings = analysisWarnings;
    this.scmConfiguration = scmConfiguration;
    this.inputModuleHierarchy = inputModuleHierarchy;
    this.globalConfig = globalConfig;
    this.globalServerSettings = globalServerSettings;
    this.projectServerSettings = projectServerSettings;
    this.languageDetection = languageDetection;
    this.filePreprocessor = filePreprocessor;
    this.projectExclusionFilters = projectExclusionFilters;
    this.sonarGlobalPropertiesFilter = sonarGlobalPropertiesFilter;
    this.ignoreCommand = loadIgnoreCommand();
    this.useScmExclusion = ignoreCommand != null;
  }

  public void execute() {
    ProgressReport progressReport = new ProgressReport("Report about progress of file preprocessing",
      TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Preprocessing files...");
    ExclusionCounter exclusionCounter = new ExclusionCounter();

    if (useScmExclusion) {
      ignoreCommand.init(inputModuleHierarchy.root().getBaseDir().toAbsolutePath());
      processModulesRecursively(inputModuleHierarchy.root(), exclusionCounter);
      ignoreCommand.clean();
    } else {
      processModulesRecursively(inputModuleHierarchy.root(), exclusionCounter);
    }

    int totalLanguagesDetected = languageDetection.getDetectedLanguages().size();

    progressReport.stop(String.format("%s detected in %s", pluralizeWithCount("language", totalLanguagesDetected),
      pluralizeWithCount("preprocessed file", totalFilesPreprocessed)));

    int excludedFileByPatternCount = exclusionCounter.getByPatternsCount();
    if (projectExclusionFilters.hasPattern() || excludedFileByPatternCount > 0) {
      if (LOG.isInfoEnabled()) {
        LOG.info("{} ignored because of inclusion/exclusion patterns", pluralizeWithCount("file", excludedFileByPatternCount));
      }
    }

    int excludedFileByScmCount = exclusionCounter.getByScmCount();
    if (useScmExclusion) {
      if (LOG.isInfoEnabled()) {
        LOG.info("{} ignored because of scm ignore settings", pluralizeWithCount("file", excludedFileByScmCount));
      }
    }
  }

  private void processModulesRecursively(DefaultInputModule module, ExclusionCounter exclusionCounter) {
    inputModuleHierarchy.children(module).stream().sorted(Comparator.comparing(DefaultInputModule::key)).forEach(
      m -> processModulesRecursively(m, exclusionCounter));
    processModule(module, exclusionCounter);
  }

  private void processModule(DefaultInputModule module, ExclusionCounter exclusionCounter) {
    // Emulate creation of module level settings
    ModuleConfiguration moduleConfig = new ModuleConfigurationProvider(sonarGlobalPropertiesFilter).provide(globalConfig, module, globalServerSettings, projectServerSettings);
    ModuleExclusionFilters moduleExclusionFilters = new ModuleExclusionFilters(moduleConfig, analysisWarnings);
    boolean hasChildModules = !module.definition().getSubProjects().isEmpty();
    boolean hasTests = module.getTestDirsOrFiles().isPresent();
    // Default to index basedir when no sources provided
    List<Path> mainSourceDirsOrFiles = module.getSourceDirsOrFiles()
      .orElseGet(() -> hasChildModules || hasTests ? emptyList() : singletonList(module.getBaseDir().toAbsolutePath()));
    List<Path> processedSources = processModuleSources(module, moduleExclusionFilters, mainSourceDirsOrFiles, InputFile.Type.MAIN,
      exclusionCounter);
    mainSourcesByModule.put(module, processedSources);
    totalFilesPreprocessed += processedSources.size();
    module.getTestDirsOrFiles().ifPresent(tests -> {
      List<Path> processedTestSources = processModuleSources(module, moduleExclusionFilters, tests, InputFile.Type.TEST, exclusionCounter);
      testSourcesByModule.put(module, processedTestSources);
      totalFilesPreprocessed += processedTestSources.size();
    });
  }

  private List<Path> processModuleSources(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters, List<Path> sources,
    InputFile.Type type, ExclusionCounter exclusionCounter) {
    List<Path> processedFiles = new ArrayList<>();
    try {
      for (Path dirOrFile : sources) {
        if (dirOrFile.toFile().isDirectory()) {
          processedFiles.addAll(processDirectory(module, moduleExclusionFilters, dirOrFile, type, exclusionCounter));
        } else {
          filePreprocessor.processFile(module, moduleExclusionFilters, dirOrFile, type, exclusionCounter, ignoreCommand)
            .ifPresent(processedFiles::add);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to preprocess files", e);
    }
    return processedFiles;
  }

  private List<Path> processDirectory(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters, Path path,
    InputFile.Type type, ExclusionCounter exclusionCounter) throws IOException {
    List<Path> processedFiles = new ArrayList<>();
    Files.walkFileTree(path.normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new DirectoryFileVisitor(file -> filePreprocessor.processFile(module, moduleExclusionFilters, file, type, exclusionCounter,
        ignoreCommand).ifPresent(processedFiles::add), module, moduleExclusionFilters, inputModuleHierarchy, type)
    );
    return processedFiles;
  }

  public List<Path> getMainSourcesByModule(DefaultInputModule module) {
    return Collections.unmodifiableList(mainSourcesByModule.get(module));
  }

  public Optional<List<Path>> getTestSourcesByModule(DefaultInputModule module) {
    return Optional.ofNullable(testSourcesByModule.get(module)).map(Collections::unmodifiableList);
  }

  private IgnoreCommand loadIgnoreCommand() {
    try {
      ScmProvider provider = scmConfiguration.provider();
      if (!scmConfiguration.isExclusionDisabled() && provider != null) {
        return provider.ignoreCommand();
      }
    } catch (UnsupportedOperationException e) {
      LOG.debug("File exclusion based on SCM ignore information is not available with this plugin.");
    }

    return null;
  }

  private static String pluralizeWithCount(String str, int count) {
    String pluralized = count == 1 ? str : (str + "s");
    return count + " " + pluralized;
  }

  public static class ExclusionCounter {
    private int excludedByPatternsCount = 0;
    private int excludedByScmCount = 0;

    public void increaseByPatternsCount() {
      excludedByPatternsCount++;
    }

    public int getByPatternsCount() {
      return excludedByPatternsCount;
    }

    public void increaseByScmCount() {
      excludedByScmCount++;
    }

    public int getByScmCount() {
      return excludedByScmCount;
    }
  }
}
