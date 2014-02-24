/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.AnalysisMode;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.List;

class InputFileBuilder {

  private final String moduleKey;
  private final PathResolver pathResolver;
  private final LanguageDetection langDetection;
  private final StatusDetection statusDetection;
  private final DefaultModuleFileSystem fs;
  private final AnalysisMode analysisMode;

  InputFileBuilder(String moduleKey, PathResolver pathResolver, LanguageDetection langDetection,
                   StatusDetection statusDetection, DefaultModuleFileSystem fs, AnalysisMode analysisMode) {
    this.moduleKey = moduleKey;
    this.pathResolver = pathResolver;
    this.langDetection = langDetection;
    this.statusDetection = statusDetection;
    this.fs = fs;
    this.analysisMode = analysisMode;
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
  DefaultInputFile create(File file) {
    String relativePath = pathResolver.relativePath(fs.baseDir(), file);
    if (relativePath == null) {
      LoggerFactory.getLogger(getClass()).warn(
        "File '%s' is ignored. It is not located in module basedir '%s'.", file.getAbsolutePath(), fs.baseDir());
      return null;
    }
    DefaultInputFile inputFile = new DefaultInputFile(relativePath);
    inputFile.setBasedir(fs.baseDir());
    inputFile.setFile(file);
    return inputFile;
  }

  /**
   * Optimization to not set all InputFile data if the file is excluded from analysis.
   */
  @CheckForNull
  DefaultInputFile complete(DefaultInputFile inputFile, InputFile.Type type) {
    inputFile.setType(type);
    inputFile.setKey(new StringBuilder().append(moduleKey).append(":").append(inputFile.relativePath()).toString());
    inputFile.setBasedir(fs.baseDir());
    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(inputFile.file(), fs.encoding());
    inputFile.setLines(metadata.lines);
    inputFile.setHash(metadata.hash);
    inputFile.setStatus(statusDetection.status(inputFile.relativePath(), metadata.hash));
    if (analysisMode.isIncremental() && inputFile.status() == InputFile.Status.SAME) {
      return null;
    }
    String lang = langDetection.language(inputFile);
    if (lang == null) {
      return null;
    }
    inputFile.setLanguage(lang);
    fillDeprecatedData(inputFile);
    return inputFile;
  }

  private void fillDeprecatedData(DefaultInputFile inputFile) {
    List<File> sourceDirs = InputFile.Type.MAIN == inputFile.type() ? fs.sourceDirs() : fs.testDirs();
    for (File sourceDir : sourceDirs) {
      String sourceRelativePath = pathResolver.relativePath(sourceDir, inputFile.file());
      if (sourceRelativePath != null) {
        inputFile.setPathRelativeToSourceDir(sourceRelativePath);
        inputFile.setSourceDirAbsolutePath(FilenameUtils.normalize(sourceDir.getAbsolutePath(), true));

        if (Java.KEY.equals(inputFile.language())) {
          inputFile.setDeprecatedKey(new StringBuilder()
            .append(moduleKey).append(":").append(JavaFile.fromRelativePath(sourceRelativePath, false).getDeprecatedKey()).toString());
        } else {
          inputFile.setDeprecatedKey(new StringBuilder().append(moduleKey).append(":").append(sourceRelativePath).toString());
        }
      }
    }
  }
}
