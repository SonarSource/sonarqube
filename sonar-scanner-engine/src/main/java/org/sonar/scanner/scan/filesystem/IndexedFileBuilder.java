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

import java.nio.file.Path;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;

public class IndexedFileBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(IndexedFileBuilder.class);
  private final String moduleKey;
  private final PathResolver pathResolver;
  private final LanguageDetection langDetection;
  private final Settings settings;
  private final BatchIdGenerator idGenerator;

  IndexedFileBuilder(String moduleKey, PathResolver pathResolver, Settings settings, LanguageDetection langDetection, BatchIdGenerator idGenerator) {
    this.moduleKey = moduleKey;
    this.pathResolver = pathResolver;
    this.settings = settings;
    this.langDetection = langDetection;
    this.idGenerator = idGenerator;
  }

  @CheckForNull
  DefaultIndexedFile create(Path file, InputFile.Type type, Path moduleBaseDir) {
    String relativePath = pathResolver.relativePath(moduleBaseDir, file);
    if (relativePath == null) {
      LOG.warn("File '{}' is ignored. It is not located in module basedir '{}'.", file.toAbsolutePath(), moduleBaseDir);
      return null;
    }
    DefaultIndexedFile indexedFile = new DefaultIndexedFile(moduleKey, moduleBaseDir, relativePath, type, idGenerator.get());
    String language = langDetection.language(indexedFile);
    if (language == null && !settings.getBoolean(CoreProperties.IMPORT_UNKNOWN_FILES_KEY)) {
      LOG.debug("'{}' language is not supported by any analyzer. Skipping it.", relativePath);
      return null;
    }

    indexedFile.setLanguage(language);
    return indexedFile;
  }
}
