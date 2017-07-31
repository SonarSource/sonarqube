/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.config.Configuration;

public class InputFileBuilder {
  public static final String PRELOAD_FILE_METADATA_KEY = "sonar.preloadFileMetadata";
  private final String moduleKey;
  private final Path moduleBaseDir;
  private final BatchIdGenerator idGenerator;
  private final MetadataGenerator metadataGenerator;
  private final boolean preloadMetadata;
  private final ModuleFileSystemInitializer moduleFileSystemInitializer;

  public InputFileBuilder(DefaultInputModule module, MetadataGenerator metadataGenerator,
    BatchIdGenerator idGenerator, Configuration settings, ModuleFileSystemInitializer moduleFileSystemInitializer) {
    this.moduleFileSystemInitializer = moduleFileSystemInitializer;
    this.moduleKey = module.key();
    this.moduleBaseDir = module.getBaseDir();
    this.metadataGenerator = metadataGenerator;
    this.idGenerator = idGenerator;
    this.preloadMetadata = settings.getBoolean(PRELOAD_FILE_METADATA_KEY).orElse(false);
  }

  DefaultInputFile create(InputFile.Type type, String relativePath, @Nullable String language) {
    DefaultIndexedFile indexedFile = new DefaultIndexedFile(moduleKey, moduleBaseDir, relativePath, type, language, idGenerator.get());
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> metadataGenerator.setMetadata(f, moduleFileSystemInitializer.defaultEncoding()));
    if (language != null) {
      inputFile.setPublished(true);
    }

    return inputFile;
  }

  void checkMetadata(DefaultInputFile inputFile) {
    if (preloadMetadata) {
      inputFile.checkMetadata();
    }
  }
}
