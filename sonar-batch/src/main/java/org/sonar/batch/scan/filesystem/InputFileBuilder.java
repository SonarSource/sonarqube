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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.util.DeprecatedKeyUtils;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.List;

class InputFileBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileBuilder.class);

  private final String moduleKey;
  private final PathResolver pathResolver;
  private final LanguageDetection langDetection;
  private final StatusDetection statusDetection;
  private final DefaultModuleFileSystem fs;
  private final AnalysisMode analysisMode;
  private final Settings settings;

  InputFileBuilder(String moduleKey, PathResolver pathResolver, LanguageDetection langDetection,
    StatusDetection statusDetection, DefaultModuleFileSystem fs, AnalysisMode analysisMode, Settings settings) {
    this.moduleKey = moduleKey;
    this.pathResolver = pathResolver;
    this.langDetection = langDetection;
    this.statusDetection = statusDetection;
    this.fs = fs;
    this.analysisMode = analysisMode;
    this.settings = settings;
  }

  String moduleKey() {
    return moduleKey;
  }

  PathResolver pathResolver() {
    return pathResolver;
  }

  LanguageDetection langDetection() {
    return langDetection;
  }

  StatusDetection statusDetection() {
    return statusDetection;
  }

  FileSystem fs() {
    return fs;
  }

  @CheckForNull
  DeprecatedDefaultInputFile create(File file) {
    String relativePath = pathResolver.relativePath(fs.baseDir(), file);
    if (relativePath == null) {
      LOG.warn("File '{}' is ignored. It is not located in module basedir '{}'.", file.getAbsolutePath(), fs.baseDir());
      return null;
    }
    DeprecatedDefaultInputFile inputFile = new DeprecatedDefaultInputFile(moduleKey, relativePath);
    inputFile.setBasedir(fs.baseDir());
    inputFile.setFile(file);
    return inputFile;
  }

  /**
   * Optimization to not set all InputFile data if the file is excluded from analysis.
   */
  @CheckForNull
  DeprecatedDefaultInputFile complete(DeprecatedDefaultInputFile inputFile, InputFile.Type type) {
    inputFile.setType(type);
    inputFile.setBasedir(fs.baseDir());
    inputFile.setEncoding(fs.encoding().name());

    String lang = langDetection.language(inputFile);
    if (lang == null && !settings.getBoolean(CoreProperties.IMPORT_UNKNOWN_FILES_KEY)) {
      return null;
    }
    inputFile.setLanguage(lang);

    FileMetadata.Metadata metadata = new FileMetadata().read(inputFile.file(), fs.encoding());
    inputFile.setLines(metadata.lines);
    inputFile.setHash(metadata.hash);
    inputFile.setOriginalLineOffsets(metadata.originalLineOffsets);
    inputFile.setLineHashes(metadata.lineHashes);
    inputFile.setEmpty(metadata.empty);
    inputFile.setStatus(statusDetection.status(inputFile.moduleKey(), inputFile.relativePath(), metadata.hash));
    if (analysisMode.isIncremental() && inputFile.status() == InputFile.Status.SAME) {
      return null;
    }
    fillDeprecatedData(inputFile);
    return inputFile;
  }

  private void fillDeprecatedData(DeprecatedDefaultInputFile inputFile) {
    List<File> sourceDirs = InputFile.Type.MAIN == inputFile.type() ? fs.sourceDirs() : fs.testDirs();
    for (File sourceDir : sourceDirs) {
      String sourceRelativePath = pathResolver.relativePath(sourceDir, inputFile.file());
      if (sourceRelativePath != null) {
        inputFile.setPathRelativeToSourceDir(sourceRelativePath);
        inputFile.setSourceDirAbsolutePath(FilenameUtils.normalize(sourceDir.getAbsolutePath(), true));

        if ("java".equals(inputFile.language())) {
          inputFile.setDeprecatedKey(new StringBuilder()
            .append(moduleKey).append(":").append(DeprecatedKeyUtils.getJavaFileDeprecatedKey(sourceRelativePath)).toString());
        } else {
          inputFile.setDeprecatedKey(new StringBuilder().append(moduleKey).append(":").append(sourceRelativePath).toString());
        }
      }
    }
  }
}
