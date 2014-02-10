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

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.utils.MessageException;

import javax.annotation.CheckForNull;
import java.io.File;
import java.util.List;

class InputFileBuilder {

  private final String moduleKey;
  private final PathResolver pathResolver;
  private final LanguageDetection langDetection;
  private final StatusDetection statusDetection;
  private final DefaultModuleFileSystem fs;

  InputFileBuilder(String moduleKey, PathResolver pathResolver, LanguageDetection langDetection,
                   StatusDetection statusDetection, DefaultModuleFileSystem fs) {
    this.moduleKey = moduleKey;
    this.pathResolver = pathResolver;
    this.langDetection = langDetection;
    this.statusDetection = statusDetection;
    this.fs = fs;
  }

  @CheckForNull
  DefaultInputFile create(File file, String type) {
    String relativePath = pathResolver.relativePath(fs.baseDir(), file);
    if (relativePath == null) {
      throw MessageException.of(String.format("File '%s' is ignored. It is not in module basedir '%s'.", file.getAbsolutePath(), fs.baseDir()));
    }
    DefaultInputFile inputFile = DefaultInputFile.create(file, fs.sourceCharset(), relativePath, Maps.<String, String>newHashMap());
    inputFile.setType(type);
    inputFile.setKey(moduleKey + ":" + inputFile.path());
    String lang = langDetection.language(inputFile);
    if (lang == null) {
      // TODO use a default plain-text language ?
      return null;
    }
    inputFile.setLanguage(lang);
    FileMetadata.Metadata metadata = FileMetadata.INSTANCE.read(inputFile.file(), fs.sourceCharset());
    inputFile.setLines(metadata.lines);
    inputFile.setHash(metadata.hash);
    inputFile.setStatus(statusDetection.status(inputFile.path(), metadata.hash));
    fillDeprecatedData(inputFile);
    return inputFile;
  }

  private void fillDeprecatedData(DefaultInputFile inputFile) {
    List<File> sourceDirs = InputFile.TYPE_MAIN.equals(inputFile.type()) ? fs.sourceDirs() : fs.testDirs();
    for (File sourceDir : sourceDirs) {
      String sourceRelativePath = pathResolver.relativePath(sourceDir, inputFile.file());
      if (sourceRelativePath != null) {
        inputFile.setPathRelativeToSourceDir(sourceRelativePath);
        inputFile.setSourceDirAbsolutePath(FilenameUtils.normalize(sourceDir.getAbsolutePath(), true));

        if (Java.KEY.equals(inputFile.language())) {
          inputFile.setDeprecatedKey(moduleKey + ":" + JavaFile.fromRelativePath(sourceRelativePath, false).getDeprecatedKey());
        } else {
          inputFile.setDeprecatedKey(moduleKey + ":" + sourceRelativePath);
        }
      }
    }
  }
}
