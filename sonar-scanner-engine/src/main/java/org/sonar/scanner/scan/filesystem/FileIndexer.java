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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.util.ProgressReport;

/**
 * Index input files into {@link InputComponentStore}.
 */
public class FileIndexer {

  private static final Logger LOG = Loggers.get(FileIndexer.class);
  private final AnalysisWarnings analysisWarnings;
  private final ScanProperties properties;
  private final InputFileFilter[] filters;
  private final ProjectExclusionFilters projectExclusionFilters;
  private final ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions;
  private final IssueExclusionsLoader issueExclusionsLoader;
  private final MetadataGenerator metadataGenerator;
  private final DefaultInputProject project;
  private final ScannerComponentIdGenerator scannerComponentIdGenerator;
  private final InputComponentStore componentStore;
  private final SensorStrategy sensorStrategy;
  private final LanguageDetection langDetection;

  private boolean warnInclusionsAlreadyLogged;
  private boolean warnExclusionsAlreadyLogged;
  private boolean warnCoverageExclusionsAlreadyLogged;
  private boolean warnDuplicationExclusionsAlreadyLogged;

  public FileIndexer(DefaultInputProject project, ScannerComponentIdGenerator scannerComponentIdGenerator, InputComponentStore componentStore,
    ProjectExclusionFilters projectExclusionFilters, ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions, IssueExclusionsLoader issueExclusionsLoader,
    MetadataGenerator metadataGenerator, SensorStrategy sensorStrategy, LanguageDetection languageDetection, AnalysisWarnings analysisWarnings, ScanProperties properties,
    InputFileFilter[] filters) {
    this.project = project;
    this.scannerComponentIdGenerator = scannerComponentIdGenerator;
    this.componentStore = componentStore;
    this.projectCoverageAndDuplicationExclusions = projectCoverageAndDuplicationExclusions;
    this.issueExclusionsLoader = issueExclusionsLoader;
    this.metadataGenerator = metadataGenerator;
    this.sensorStrategy = sensorStrategy;
    this.langDetection = languageDetection;
    this.analysisWarnings = analysisWarnings;
    this.properties = properties;
    this.filters = filters;
    this.projectExclusionFilters = projectExclusionFilters;
  }

  public FileIndexer(DefaultInputProject project, ScannerComponentIdGenerator scannerComponentIdGenerator, InputComponentStore componentStore,
    ProjectExclusionFilters projectExclusionFilters, ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions, IssueExclusionsLoader issueExclusionsLoader,
    MetadataGenerator metadataGenerator, SensorStrategy sensorStrategy, LanguageDetection languageDetection, AnalysisWarnings analysisWarnings, ScanProperties properties) {
    this(project, scannerComponentIdGenerator, componentStore, projectExclusionFilters, projectCoverageAndDuplicationExclusions, issueExclusionsLoader, metadataGenerator,
      sensorStrategy, languageDetection, analysisWarnings, properties, new InputFileFilter[0]);
  }

  void indexFile(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters, ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions,
    Path sourceFile, Type type, ProgressReport progressReport, ProjectFileIndexer.ExclusionCounter exclusionCounter, @Nullable IgnoreCommand ignoreCommand)
    throws IOException {
    // get case of real file without resolving link
    Path realAbsoluteFile = sourceFile.toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    if (!realAbsoluteFile.startsWith(project.getBaseDir())) {
      LOG.warn("File '{}' is ignored. It is not located in project basedir '{}'.", realAbsoluteFile.toAbsolutePath(), project.getBaseDir());
      return;
    }
    if (!realAbsoluteFile.startsWith(module.getBaseDir())) {
      LOG.warn("File '{}' is ignored. It is not located in module basedir '{}'.", realAbsoluteFile.toAbsolutePath(), module.getBaseDir());
      return;
    }
    Path projectRelativePath = project.getBaseDir().relativize(realAbsoluteFile);
    Path moduleRelativePath = module.getBaseDir().relativize(realAbsoluteFile);
    boolean included = evaluateInclusionsFilters(moduleExclusionFilters, realAbsoluteFile, projectRelativePath, moduleRelativePath, type);
    if (!included) {
      exclusionCounter.increaseByPatternsCount();
      return;
    }
    boolean excluded = evaluateExclusionsFilters(moduleExclusionFilters, realAbsoluteFile, projectRelativePath, moduleRelativePath, type);
    if (excluded) {
      exclusionCounter.increaseByPatternsCount();
      return;
    }

    String language = langDetection.language(realAbsoluteFile, projectRelativePath);

    if (ignoreCommand != null && ignoreCommand.isIgnored(realAbsoluteFile)) {
      LOG.debug("File '{}' is excluded by the scm ignore settings.", realAbsoluteFile);
      exclusionCounter.increaseByScmCount();
      return;
    }

    DefaultIndexedFile indexedFile = new DefaultIndexedFile(realAbsoluteFile, project.key(),
      projectRelativePath.toString(),
      moduleRelativePath.toString(),
      type, language, scannerComponentIdGenerator.getAsInt(), sensorStrategy);
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> metadataGenerator.setMetadata(module.getKeyWithBranch(), f, module.getEncoding()));
    if (language != null) {
      inputFile.setPublished(true);
    }
    if (!accept(inputFile)) {
      return;
    }
    checkIfAlreadyIndexed(inputFile);
    componentStore.put(module.key(), inputFile);
    issueExclusionsLoader.addMulticriteriaPatterns(inputFile);
    LOG.debug("'{}' indexed {}with language '{}'", projectRelativePath, type == Type.TEST ? "as test " : "", inputFile.language());
    evaluateCoverageExclusions(moduleCoverageAndDuplicationExclusions, inputFile);
    evaluateDuplicationExclusions(moduleCoverageAndDuplicationExclusions, inputFile);
    if (properties.preloadFileMetadata()) {
      inputFile.checkMetadata();
    }
    int count = componentStore.inputFiles().size();
    progressReport.message(count + " " + pluralizeFiles(count) + " indexed...  (last one was " + inputFile.getProjectRelativePath() + ")");
  }

  private boolean evaluateInclusionsFilters(ModuleExclusionFilters moduleExclusionFilters, Path realAbsoluteFile, Path projectRelativePath, Path moduleRelativePath,
    InputFile.Type type) {
    if (!Arrays.equals(moduleExclusionFilters.getInclusionsConfig(type), projectExclusionFilters.getInclusionsConfig(type))) {
      // Module specific configuration
      return moduleExclusionFilters.isIncluded(realAbsoluteFile, moduleRelativePath, type);
    }
    boolean includedByProjectConfiguration = projectExclusionFilters.isIncluded(realAbsoluteFile, projectRelativePath, type);
    if (includedByProjectConfiguration) {
      return true;
    } else if (moduleExclusionFilters.isIncluded(realAbsoluteFile, moduleRelativePath, type)) {
      warnOnce(
        type == Type.MAIN ? CoreProperties.PROJECT_INCLUSIONS_PROPERTY : CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY,
        FilenameUtils.normalize(projectRelativePath.toString(), true), () -> warnInclusionsAlreadyLogged, () -> warnInclusionsAlreadyLogged = true);
      return true;
    }
    return false;
  }

  private boolean evaluateExclusionsFilters(ModuleExclusionFilters moduleExclusionFilters, Path realAbsoluteFile, Path projectRelativePath, Path moduleRelativePath,
    InputFile.Type type) {
    if (!Arrays.equals(moduleExclusionFilters.getExclusionsConfig(type), projectExclusionFilters.getExclusionsConfig(type))) {
      // Module specific configuration
      return moduleExclusionFilters.isExcluded(realAbsoluteFile, moduleRelativePath, type);
    }
    boolean includedByProjectConfiguration = projectExclusionFilters.isExcluded(realAbsoluteFile, projectRelativePath, type);
    if (includedByProjectConfiguration) {
      return true;
    } else if (moduleExclusionFilters.isExcluded(realAbsoluteFile, moduleRelativePath, type)) {
      warnOnce(
        type == Type.MAIN ? CoreProperties.PROJECT_EXCLUSIONS_PROPERTY : CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY,
        FilenameUtils.normalize(projectRelativePath.toString(), true), () -> warnExclusionsAlreadyLogged, () -> warnExclusionsAlreadyLogged = true);
      return true;
    }
    return false;
  }

  private void checkIfAlreadyIndexed(DefaultInputFile inputFile) {
    if (componentStore.inputFile(inputFile.getProjectRelativePath()) != null) {
      throw MessageException.of("File " + inputFile + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
        + "disjoint sets for main and test files");
    }
  }

  private void evaluateCoverageExclusions(ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, DefaultInputFile inputFile) {
    boolean excludedForCoverage = isExcludedForCoverage(moduleCoverageAndDuplicationExclusions, inputFile);
    inputFile.setExcludedForCoverage(excludedForCoverage);
    if (excludedForCoverage) {
      LOG.debug("File {} excluded for coverage", inputFile);
    }
  }

  private boolean isExcludedForCoverage(ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, DefaultInputFile inputFile) {
    if (!Arrays.equals(moduleCoverageAndDuplicationExclusions.getCoverageExclusionConfig(), projectCoverageAndDuplicationExclusions.getCoverageExclusionConfig())) {
      // Module specific configuration
      return moduleCoverageAndDuplicationExclusions.isExcludedForCoverage(inputFile);
    }
    boolean excludedByProjectConfiguration = projectCoverageAndDuplicationExclusions.isExcludedForCoverage(inputFile);
    if (excludedByProjectConfiguration) {
      return true;
    } else if (moduleCoverageAndDuplicationExclusions.isExcludedForCoverage(inputFile)) {
      warnOnce(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY, inputFile.getProjectRelativePath(), () -> warnCoverageExclusionsAlreadyLogged,
        () -> warnCoverageExclusionsAlreadyLogged = true);
      return true;
    }
    return false;
  }

  private void evaluateDuplicationExclusions(ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, DefaultInputFile inputFile) {
    boolean excludedForDuplications = isExcludedForDuplications(moduleCoverageAndDuplicationExclusions, inputFile);
    inputFile.setExcludedForDuplication(excludedForDuplications);
    if (excludedForDuplications) {
      LOG.debug("File {} excluded for duplication", inputFile);
    }
  }

  private boolean isExcludedForDuplications(ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, DefaultInputFile inputFile) {
    if (!Arrays.equals(moduleCoverageAndDuplicationExclusions.getDuplicationExclusionConfig(), projectCoverageAndDuplicationExclusions.getDuplicationExclusionConfig())) {
      // Module specific configuration
      return moduleCoverageAndDuplicationExclusions.isExcludedForDuplication(inputFile);
    }
    boolean excludedByProjectConfiguration = projectCoverageAndDuplicationExclusions.isExcludedForDuplication(inputFile);
    if (excludedByProjectConfiguration) {
      return true;
    } else if (moduleCoverageAndDuplicationExclusions.isExcludedForDuplication(inputFile)) {
      warnOnce(CoreProperties.CPD_EXCLUSIONS, inputFile.getProjectRelativePath(), () -> warnDuplicationExclusionsAlreadyLogged,
        () -> warnDuplicationExclusionsAlreadyLogged = true);
      return true;
    }
    return false;
  }

  private void warnOnce(String propKey, String filePath, BooleanSupplier alreadyLoggedGetter, Runnable markAsLogged) {
    if (!alreadyLoggedGetter.getAsBoolean()) {
      String msg = "Specifying module-relative paths at project level in the property '" + propKey + "' is deprecated. " +
        "To continue matching files like '" + filePath + "', update this property so that patterns refer to project-relative paths.";
      LOG.warn(msg);
      analysisWarnings.addUnique(msg);
      markAsLogged.run();
    }
  }

  private boolean accept(InputFile indexedFile) {
    // InputFileFilter extensions. Might trigger generation of metadata
    for (InputFileFilter filter : filters) {
      if (!filter.accept(indexedFile)) {
        LOG.debug("'{}' excluded by {}", indexedFile, filter.getClass().getName());
        return false;
      }
    }
    return true;
  }

  private static String pluralizeFiles(int count) {
    return count == 1 ? "file" : "files";
  }

}
