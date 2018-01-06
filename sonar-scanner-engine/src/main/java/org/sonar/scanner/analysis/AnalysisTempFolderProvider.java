/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.picocontainer.ComponentLifecycle;
import org.picocontainer.PicoContainer;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.internal.DefaultTempFolder;

public class AnalysisTempFolderProvider extends ProviderAdapter implements ComponentLifecycle<TempFolder> {
  static final String TMP_NAME = ".sonartmp";
  private DefaultTempFolder projectTempFolder;
  private boolean started = false;

  public TempFolder provide(InputModuleHierarchy moduleHierarchy) {
    if (projectTempFolder == null) {
      Path workingDir = moduleHierarchy.root().getWorkDir();
      Path tempDir = workingDir.normalize().resolve(TMP_NAME);
      try {
        Files.deleteIfExists(tempDir);
        Files.createDirectories(tempDir);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create root temp directory " + tempDir, e);
      }

      projectTempFolder = new DefaultTempFolder(tempDir.toFile(), true);
    }
    return projectTempFolder;
  }

  @Override
  public void start(PicoContainer container) {
    started = true;
  }

  @Override
  public void stop(PicoContainer container) {
    if (projectTempFolder != null) {
      projectTempFolder.stop();
    }
  }

  @Override
  public void dispose(PicoContainer container) {
    // nothing to do
  }

  @Override
  public boolean componentHasLifecycle() {
    return true;
  }

  @Override
  public boolean isStarted() {
    return started;
  }
}
