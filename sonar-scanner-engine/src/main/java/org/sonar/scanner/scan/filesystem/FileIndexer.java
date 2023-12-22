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

import java.nio.file.Path;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.repository.language.Language;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scm.ScmChangedFiles;
import org.sonar.scanner.util.ProgressReport;

import static java.lang.String.format;

/**
 * Index input files into {@link InputComponentStore}.
 */
public class FileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);

  private final ScanProperties properties;
  private final ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions;
  private final IssueExclusionsLoader issueExclusionsLoader;
  private final MetadataGenerator metadataGenerator;
  private final DefaultInputProject project;
  private final ScannerComponentIdGenerator scannerComponentIdGenerator;
  private final InputComponentStore componentStore;
  private final SensorStrategy sensorStrategy;
  private final LanguageDetection langDetection;
  private final StatusDetection statusDetection;
  private final ScmChangedFiles scmChangedFiles;

  private final ModuleRelativePathWarner moduleRelativePathWarner;
  private final InputFileFilterRepository inputFileFilterRepository;

  public FileIndexer(DefaultInputProject project, ScannerComponentIdGenerator scannerComponentIdGenerator, InputComponentStore componentStore,
    ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions, IssueExclusionsLoader issueExclusionsLoader,
    MetadataGenerator metadataGenerator, SensorStrategy sensorStrategy, LanguageDetection languageDetection, ScanProperties properties,
    ScmChangedFiles scmChangedFiles, StatusDetection statusDetection, ModuleRelativePathWarner moduleRelativePathWarner, InputFileFilterRepository inputFileFilterRepository) {
    this.project = project;
    this.scannerComponentIdGenerator = scannerComponentIdGenerator;
    this.componentStore = componentStore;
    this.projectCoverageAndDuplicationExclusions = projectCoverageAndDuplicationExclusions;
    this.issueExclusionsLoader = issueExclusionsLoader;
    this.metadataGenerator = metadataGenerator;
    this.sensorStrategy = sensorStrategy;
    this.langDetection = languageDetection;
    this.properties = properties;
    this.scmChangedFiles = scmChangedFiles;
    this.statusDetection = statusDetection;
    this.moduleRelativePathWarner = moduleRelativePathWarner;
    this.inputFileFilterRepository = inputFileFilterRepository;
  }

  void indexFile(DefaultInputModule module, ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, Path sourceFile,
    Type type, ProgressReport progressReport) {
    Path projectRelativePath = project.getBaseDir().relativize(sourceFile);
    Path moduleRelativePath = module.getBaseDir().relativize(sourceFile);

    // This should be fast; language should be cached from preprocessing step
    Language language = langDetection.language(sourceFile, projectRelativePath);

    DefaultIndexedFile indexedFile = new DefaultIndexedFile(
      sourceFile,
      project.key(),
      projectRelativePath.toString(),
      moduleRelativePath.toString(),
      type,
      language != null ? language.key() : null,
      scannerComponentIdGenerator.getAsInt(),
      sensorStrategy,
      scmChangedFiles.getOldRelativeFilePath(sourceFile)
    );

    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> metadataGenerator.setMetadata(module.key(), f, module.getEncoding()),
      f -> f.setStatus(statusDetection.findStatusFromScm(f)));
    if (language != null && language.isPublishAllFiles()) {
      inputFile.setPublished(true);
    }
    if (!accept(inputFile)) {
      return;
    }
    checkIfAlreadyIndexed(inputFile);
    componentStore.put(module.key(), inputFile);
    issueExclusionsLoader.addMulticriteriaPatterns(inputFile);
    String langStr = inputFile.language() != null ? format("with language '%s'", inputFile.language()) : "with no language";
    if (LOG.isDebugEnabled()) {
      LOG.debug("'{}' indexed {}{}", projectRelativePath, type == Type.TEST ? "as test " : "", langStr);
    }
    evaluateCoverageExclusions(moduleCoverageAndDuplicationExclusions, inputFile);
    evaluateDuplicationExclusions(moduleCoverageAndDuplicationExclusions, inputFile);
    if (properties.preloadFileMetadata()) {
      inputFile.checkMetadata();
    }
    int count = componentStore.inputFiles().size();
    progressReport.message(count + " " + pluralizeFiles(count) + " indexed...  (last one was " + inputFile.getProjectRelativePath() + ")");
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
      moduleRelativePathWarner.warnOnce(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY, inputFile.getProjectRelativePath());
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
      moduleRelativePathWarner.warnOnce(CoreProperties.CPD_EXCLUSIONS, inputFile.getProjectRelativePath());
      return true;
    }
    return false;
  }

  private boolean accept(InputFile indexedFile) {
    // InputFileFilter extensions. Might trigger generation of metadata
    for (InputFileFilter filter : inputFileFilterRepository.getInputFileFilters()) {
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
