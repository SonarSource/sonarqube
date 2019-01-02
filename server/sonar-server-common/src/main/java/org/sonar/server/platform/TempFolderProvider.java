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
package org.sonar.server.platform;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.internal.DefaultTempFolder;

public class TempFolderProvider extends ProviderAdapter {

  private TempFolder tempFolder;

  public TempFolder provide(ServerFileSystem fs) {
    if (tempFolder == null) {
      File tempDir = new File(fs.getTempDir(), "tmp");
      try {
        FileUtils.forceMkdir(tempDir);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create temp directory " + tempDir, e);
      }
      tempFolder = new DefaultTempFolder(tempDir);
    }
    return tempFolder;
  }
}
