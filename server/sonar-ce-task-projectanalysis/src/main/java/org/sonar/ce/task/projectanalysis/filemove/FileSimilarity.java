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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public interface FileSimilarity {

  interface File {
    String getPath();

    List<String> getLineHashes();

    int getLineCount();
  }

  final class FileImpl implements File {
    private final String path;
    private final List<String> lineHashes;
    private final int lineCount;

    FileImpl(String path, List<String> lineHashes) {
      this.path = requireNonNull(path, "path can not be null");
      this.lineHashes = requireNonNull(lineHashes, "lineHashes can not be null");
      this.lineCount = lineHashes.size();
    }

    public String getPath() {
      return path;
    }

    /**
     * List of hash of each line. An empty list is returned
     * if file content is empty.
     */
    public List<String> getLineHashes() {
      return lineHashes;
    }

    public int getLineCount() {
      return lineCount;
    }
  }

  final class LazyFileImpl implements File {
    private final String path;
    private final Supplier<List<String>> supplier;
    private final int lineCount;
    private List<String> lineHashes;

    LazyFileImpl(String path, Supplier<List<String>> supplier, int lineCount) {
      this.path = requireNonNull(path, "path can not be null");
      this.supplier = requireNonNull(supplier, "supplier can not be null");
      this.lineCount = lineCount;
    }

    public String getPath() {
      return path;
    }

    /**
     * List of hash of each line. An empty list is returned
     * if file content is empty.
     */
    public List<String> getLineHashes() {
      ensureSupplierCalled();
      return lineHashes;
    }

    private void ensureSupplierCalled() {
      if (lineHashes == null) {
        lineHashes = Optional.ofNullable(supplier.get()).orElse(emptyList());
      }
    }

    public int getLineCount() {
      return lineCount;
    }
  }

  int score(File file1, File file2);
}
