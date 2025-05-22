/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.scanner.scan.ScanProperties;

public class FilePreprocessor {

  private static final Logger LOG = LoggerFactory.getLogger(FilePreprocessor.class);

  private final ModuleRelativePathWarner moduleRelativePathWarner;
  private final DefaultInputProject project;
  private final LanguageDetection languageDetection;
  private final ProjectExclusionFilters projectExclusionFilters;
  private final ScanProperties properties;

  public FilePreprocessor(ModuleRelativePathWarner moduleRelativePathWarner, DefaultInputProject project,
    LanguageDetection languageDetection, ProjectExclusionFilters projectExclusionFilters, ScanProperties properties) {
    this.moduleRelativePathWarner = moduleRelativePathWarner;
    this.project = project;
    this.languageDetection = languageDetection;
    this.projectExclusionFilters = projectExclusionFilters;
    this.properties = properties;
  }

  public Optional<Path> processFile(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters, Path sourceFile,
    InputFile.Type type, ProjectFilePreprocessor.ExclusionCounter exclusionCounter, @CheckForNull IgnoreCommand ignoreCommand) throws IOException {
    // get case of real file without resolving link
    Path realAbsoluteFile = sourceFile.toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();

    if (!isValidSymbolicLink(realAbsoluteFile, module.getBaseDir())) {
      return Optional.empty();
    }

    Path projectRelativePath = project.getBaseDir().relativize(realAbsoluteFile);
    Path moduleRelativePath = module.getBaseDir().relativize(realAbsoluteFile);
    boolean included = isFileIncluded(moduleExclusionFilters, realAbsoluteFile, projectRelativePath, moduleRelativePath, type);
    if (!included) {
      exclusionCounter.increaseByPatternsCount();
      return Optional.empty();
    }
    boolean excluded = isFileExcluded(moduleExclusionFilters, realAbsoluteFile, projectRelativePath, moduleRelativePath, type);
    if (excluded) {
      exclusionCounter.increaseByPatternsCount();
      return Optional.empty();
    }

    if (!realAbsoluteFile.startsWith(project.getBaseDir())) {
      LOG.warn("File '{}' is ignored. It is not located in project basedir '{}'.", realAbsoluteFile.toAbsolutePath(), project.getBaseDir());
      return Optional.empty();
    }
    if (!realAbsoluteFile.startsWith(module.getBaseDir())) {
      LOG.warn("File '{}' is ignored. It is not located in module basedir '{}'.", realAbsoluteFile.toAbsolutePath(), module.getBaseDir());
      return Optional.empty();
    }

    if (ignoreCommand != null && ignoreCommand.isIgnored(realAbsoluteFile)) {
      LOG.debug("File '{}' is excluded by the scm ignore settings.", realAbsoluteFile);
      exclusionCounter.increaseByScmCount();
      return Optional.empty();
    }

    if (Files.exists(realAbsoluteFile) && isFileSizeBiggerThanLimit(realAbsoluteFile)) {
      LOG.warn("File '{}' is bigger than {}MB and as consequence is removed from the analysis scope.", realAbsoluteFile.toAbsolutePath(), properties.fileSizeLimit());
      return Optional.empty();
    }

    languageDetection.language(realAbsoluteFile, projectRelativePath);

    return Optional.of(realAbsoluteFile);
  }

  private boolean isFileIncluded(ModuleExclusionFilters moduleExclusionFilters, Path realAbsoluteFile, Path projectRelativePath,
    Path moduleRelativePath, InputFile.Type type) {
    if (!Arrays.equals(moduleExclusionFilters.getInclusionsConfig(type), projectExclusionFilters.getInclusionsConfig(type))) {
      return moduleExclusionFilters.isIncluded(realAbsoluteFile, moduleRelativePath, type);
    }
    boolean includedByProjectConfiguration = projectExclusionFilters.isIncluded(realAbsoluteFile, projectRelativePath, type);
    if (includedByProjectConfiguration) {
      return true;
    }
    if (moduleExclusionFilters.isIncluded(realAbsoluteFile, moduleRelativePath, type)) {
      moduleRelativePathWarner.warnOnce(
        type == InputFile.Type.MAIN ? CoreProperties.PROJECT_INCLUSIONS_PROPERTY : CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY,
        FilenameUtils.normalize(projectRelativePath.toString(), true));
      return true;
    }
    return false;
  }

  private boolean isFileExcluded(ModuleExclusionFilters moduleExclusionFilters, Path realAbsoluteFile, Path projectRelativePath,
    Path moduleRelativePath, InputFile.Type type) {
    if (!Arrays.equals(moduleExclusionFilters.getExclusionsConfig(type), projectExclusionFilters.getExclusionsConfig(type))) {
      return moduleExclusionFilters.isExcluded(realAbsoluteFile, moduleRelativePath, type);
    }
    boolean includedByProjectConfiguration = projectExclusionFilters.isExcluded(realAbsoluteFile, projectRelativePath, type);
    if (includedByProjectConfiguration) {
      return true;
    }
    if (moduleExclusionFilters.isExcluded(realAbsoluteFile, moduleRelativePath, type)) {
      moduleRelativePathWarner.warnOnce(
        type == InputFile.Type.MAIN ? CoreProperties.PROJECT_EXCLUSIONS_PROPERTY : CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY,
        FilenameUtils.normalize(projectRelativePath.toString(), true));
      return true;
    }
    return false;
  }

  private boolean isFileSizeBiggerThanLimit(Path filePath) throws IOException {
    return Files.size(filePath) > properties.fileSizeLimit() * 1024L * 1024L;
  }

  private boolean isValidSymbolicLink(Path absolutePath, Path moduleBaseDirectory) throws IOException {
    if (!Files.isSymbolicLink(absolutePath)) {
      return true;
    }

    Optional<Path> target = resolvePathToTarget(absolutePath);
    if (target.isEmpty() || !Files.exists(target.get())) {
      LOG.warn("File '{}' is ignored. It is a symbolic link targeting a file that does not exist.", absolutePath);
      return false;
    }

    if (!target.get().startsWith(project.getBaseDir())) {
      LOG.warn("File '{}' is ignored. It is a symbolic link targeting a file not located in project basedir.", absolutePath);
      return false;
    }

    if (!target.get().startsWith(moduleBaseDirectory)) {
      LOG.info("File '{}' is ignored. It is a symbolic link targeting a file not located in module basedir.", absolutePath);
      return false;
    }

    return true;
  }

  private static Optional<Path> resolvePathToTarget(Path symbolicLinkAbsolutePath) throws IOException {
    Path target = Files.readSymbolicLink(symbolicLinkAbsolutePath);
    if (target.isAbsolute()) {
      return Optional.of(target);
    }

    try {
      return Optional.of(symbolicLinkAbsolutePath.getParent().resolve(target).toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize());
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
