/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.issue.tracking;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import javax.annotation.CheckForNull;

import java.util.Collection;

public class SourceHashHolder {

  private final ServerLineHashesLoader lastSnapshots;

  private FileHashes hashedReference;
  private FileHashes hashedSource;
  private DefaultInputFile inputFile;

  public SourceHashHolder(DefaultInputFile inputFile, ServerLineHashesLoader lastSnapshots) {
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
        String[] lineHashes = lastSnapshots.getLineHashes(inputFile.key(), null);
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
      return ImmutableSet.of();
    } else {
      return getHashedSource().getLinesForHash(reference.getHash(originLine));
    }
  }
}
