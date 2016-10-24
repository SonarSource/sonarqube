/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;

class InputFileBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileBuilder.class);

  @VisibleForTesting
  static final Charset UTF_32BE = Charset.forName("UTF-32BE");

  @VisibleForTesting
  static final Charset UTF_32LE = Charset.forName("UTF-32LE");

  private final String moduleKey;
  private final PathResolver pathResolver;
  private final LanguageDetection langDetection;
  private final StatusDetection statusDetection;
  private final DefaultModuleFileSystem fs;
  private final Settings settings;
  private final FileMetadata fileMetadata;

  InputFileBuilder(String moduleKey, PathResolver pathResolver, LanguageDetection langDetection,
    StatusDetection statusDetection, DefaultModuleFileSystem fs, Settings settings, FileMetadata fileMetadata) {
    this.moduleKey = moduleKey;
    this.pathResolver = pathResolver;
    this.langDetection = langDetection;
    this.statusDetection = statusDetection;
    this.fs = fs;
    this.settings = settings;
    this.fileMetadata = fileMetadata;
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
      LOG.warn("File '{}' is ignored. It is not located in module basedir '{}'.", file.getAbsolutePath(), fs.baseDir());
      return null;
    }
    return new DefaultInputFile(moduleKey, relativePath);
  }

  /**
   * Optimization to not compute InputFile metadata if the file is excluded from analysis.
   */
  @CheckForNull
  DefaultInputFile completeAndComputeMetadata(DefaultInputFile inputFile, InputFile.Type type) {
    inputFile.setType(type);
    inputFile.setModuleBaseDir(fs.baseDir().toPath());

    String lang = langDetection.language(inputFile);
    if (lang == null && !settings.getBoolean(CoreProperties.IMPORT_UNKNOWN_FILES_KEY)) {
      return null;
    }
    inputFile.setLanguage(lang);

    Charset charset = detectCharset(inputFile.file(), fs.encoding());
    inputFile.setCharset(charset);

    inputFile.initMetadata(fileMetadata.readMetadata(inputFile.file(), charset));

    inputFile.setStatus(statusDetection.status(inputFile.moduleKey(), inputFile.relativePath(), inputFile.hash()));

    return inputFile;
  }

  /**
   * @return charset detected from BOM in given file or given defaultCharset
   * @throws IllegalStateException if an I/O error occurs
   */
  private static Charset detectCharset(File file, Charset defaultCharset) {
    try (FileInputStream inputStream = new FileInputStream(file)) {
      byte[] bom = new byte[4];
      int n = inputStream.read(bom, 0, bom.length);
      if ((n >= 3) && (bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
        return StandardCharsets.UTF_8;
      } else if ((n >= 4) && (bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
        return UTF_32BE;
      } else if ((n >= 4) && (bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
        return UTF_32LE;
      } else if ((n >= 2) && (bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
        return StandardCharsets.UTF_16BE;
      } else if ((n >= 2) && (bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
        return StandardCharsets.UTF_16LE;
      } else {
        return defaultCharset;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file " + file.getAbsolutePath(), e);
    }
  }
}
