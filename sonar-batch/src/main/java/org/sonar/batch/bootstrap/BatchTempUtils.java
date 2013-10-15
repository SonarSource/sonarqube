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
package org.sonar.batch.bootstrap;

import org.apache.commons.io.FileUtils;
import org.sonar.api.CoreProperties;
import org.sonar.core.utils.AbstractTempUtils;

import java.io.File;
import java.io.IOException;

public class BatchTempUtils extends AbstractTempUtils {

  public BatchTempUtils(BootstrapSettings bootstrapSettings) {
    String workingDirPath = bootstrapSettings.property(CoreProperties.WORKING_DIRECTORY, CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE);
    File workingDir = new File(workingDirPath);
    File tempDir = new File(workingDir, "tmp");
    try {
      FileUtils.forceMkdir(tempDir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create root temp directory " + tempDir, e);
    }
    setTempDir(tempDir);
  }

}
