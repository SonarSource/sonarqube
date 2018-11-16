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
package org.sonar.scanner.issue.tracking;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.core.component.ComponentKeys;

public class SourceHashHolder {

  private final DefaultInputProject project;
  private final DefaultInputFile inputFile;
  private final ServerLineHashesLoader lastSnapshots;

  private FileHashes hashedReference;
  private FileHashes hashedSource;

  public SourceHashHolder(DefaultInputProject project, DefaultInputFile inputFile, ServerLineHashesLoader lastSnapshots) {
    this.project = project;
    this.inputFile = inputFile;
    this.lastSnapshots = lastSnapshots;
  }

  private void initHashes() {
    if (hashedSource == null) {
      hashedSource = FileHashes.create(inputFile);
      Status status = inputFile.status();
      if (status == Status.ADDED) {
        hashedReference = null;
      } else if (status == Status.SAME) {
        hashedReference = hashedSource;
      } else {
        // Need key with branch
        String serverSideKey = ComponentKeys.createEffectiveKey(project.getKeyWithBranch(), inputFile);
        String[] lineHashes = lastSnapshots.getLineHashes(serverSideKey);
        hashedReference = lineHashes != null ? FileHashes.create(lineHashes) : null;
      }
    }
  }

  @CheckForNull
  public FileHashes getHashedReference() {
    initHashes();
    return hashedReference;
  }

  public FileHashes getHashedSource() {
    initHashes();
    return hashedSource;
  }
}
