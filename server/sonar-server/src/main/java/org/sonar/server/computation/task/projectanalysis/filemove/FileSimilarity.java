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
package org.sonar.server.computation.task.projectanalysis.filemove;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public interface FileSimilarity {

  final class File {
    private final String path;
    private final String srcHash;
    private final List<String> lineHashes;

    public File(String path, @Nullable String srcHash, @Nullable List<String> lineHashes) {
      this.path = requireNonNull(path, "path can not be null");
      this.srcHash = srcHash;
      this.lineHashes = lineHashes;
    }

    public String getPath() {
      return path;
    }

    @CheckForNull
    public String getSrcHash() {
      return srcHash;
    }

    @CheckForNull
    public List<String> getLineHashes() {
      return lineHashes;
    }
  }

  int score(File file1, File file2);
}
