/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.batch;

import java.io.File;
import java.util.Objects;

public class BatchReportDirectoryHolderImpl implements MutableBatchReportDirectoryHolder {

  private File directory = null;

  @Override
  public void setDirectory(File newDirectory) {
    this.directory = Objects.requireNonNull(newDirectory);
  }

  @Override
  public File getDirectory() {
    if (this.directory == null) {
      throw new IllegalStateException("Directory has not been set yet");
    }
    return this.directory;
  }
}
