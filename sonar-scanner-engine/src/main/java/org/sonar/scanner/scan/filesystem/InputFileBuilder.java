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

import java.nio.file.Path;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.scanner.scan.ScanProperties;

public class InputFileBuilder {
  private final DefaultInputModule module;
  private final ScannerComponentIdGenerator idGenerator;
  private final MetadataGenerator metadataGenerator;
  private final boolean preloadMetadata;
  private final Path projectBaseDir;
  private final SensorStrategy sensorStrategy;

  public InputFileBuilder(DefaultInputProject project, DefaultInputModule module, MetadataGenerator metadataGenerator,
                          ScannerComponentIdGenerator idGenerator, ScanProperties properties,
                          SensorStrategy sensorStrategy) {
    this.sensorStrategy = sensorStrategy;
    this.projectBaseDir = project.getBaseDir();
    this.module = module;
    this.metadataGenerator = metadataGenerator;
    this.idGenerator = idGenerator;
    this.preloadMetadata = properties.preloadFileMetadata();
  }

  DefaultInputFile create(InputFile.Type type, Path absolutePath, @Nullable String language) {
    DefaultIndexedFile indexedFile = new DefaultIndexedFile(absolutePath, module.key(),
      projectBaseDir.relativize(absolutePath).toString(),
      module.getBaseDir().relativize(absolutePath).toString(),
      type, language, idGenerator.getAsInt(), sensorStrategy);
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> metadataGenerator.setMetadata(f, module.getEncoding()));
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
