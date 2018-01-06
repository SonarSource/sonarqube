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
package org.sonar.scanner.issue.tracking;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.core.component.ComponentKeys;

public class SourceHashHolder {

  private final DefaultInputModule module;
  private final DefaultInputFile inputFile;
  private final ServerLineHashesLoader lastSnapshots;

  private FileHashes hashedReference;
  private FileHashes hashedSource;

  public SourceHashHolder(DefaultInputModule module, DefaultInputFile inputFile, ServerLineHashesLoader lastSnapshots) {
    this.module = module;
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
        String serverSideKey = ComponentKeys.createEffectiveKey(module.definition().getKeyWithBranch(), inputFile);
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

  public Collection<Integer> getNewLinesMatching(Integer originLine) {
    FileHashes reference = getHashedReference();
    if (reference == null) {
      return Collections.emptySet();
    } else {
      return getHashedSource().getLinesForHash(reference.getHash(originLine));
    }
  }
}
