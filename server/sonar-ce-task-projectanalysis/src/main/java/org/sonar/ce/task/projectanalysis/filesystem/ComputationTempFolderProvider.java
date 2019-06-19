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
package org.sonar.ce.task.projectanalysis.filesystem;

import java.io.File;
import java.io.IOException;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.picocontainer.ComponentLifecycle;
import org.picocontainer.PicoContainer;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.impl.utils.DefaultTempFolder;
import org.sonar.api.utils.TempFolder;
import org.sonar.server.platform.ServerFileSystem;

/**
 * Provides a TempFolder instance pointing to a directory dedicated to the processing of a specific item.
 * This directory will be deleted at the end of the processing.
 * This directory is located in the "ce" directory of the temp directory of the SonarQube instance.
 */
public class ComputationTempFolderProvider extends ProviderAdapter implements ComponentLifecycle<TempFolder> {
  private boolean started = false;
  @CheckForNull
  private DefaultTempFolder tempFolder;

  public TempFolder provide(ServerFileSystem fs) {
    if (this.tempFolder == null) {
      File tempDir = new File(fs.getTempDir(), "ce");
      try {
        FileUtils.forceMkdir(tempDir);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create computation temp directory " + tempDir, e);
      }
      File computationDir = new DefaultTempFolder(tempDir).newDir();
      this.tempFolder = new DefaultTempFolder(computationDir, true);
    }
    return this.tempFolder;
  }

  @Override
  public void start(PicoContainer container) {
    this.started = true;
  }

  @Override
  public void stop(PicoContainer container) {
    if (tempFolder != null) {
      tempFolder.stop();
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
