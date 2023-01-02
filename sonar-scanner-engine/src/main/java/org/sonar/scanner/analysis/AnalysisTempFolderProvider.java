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
package org.sonar.scanner.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.impl.utils.DefaultTempFolder;
import org.sonar.api.utils.TempFolder;
import org.springframework.context.annotation.Bean;

public class AnalysisTempFolderProvider {
  static final String TMP_NAME = ".sonartmp";

  @Bean("AnalysisTempFolder")
  public TempFolder provide(DefaultInputProject project) {
    Path workingDir = project.getWorkDir();
    Path tempDir = workingDir.normalize().resolve(TMP_NAME);
    try {
      Files.deleteIfExists(tempDir);
      Files.createDirectories(tempDir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create root temp directory " + tempDir, e);
    }

    return new DefaultTempFolder(tempDir.toFile(), true);
  }
}
