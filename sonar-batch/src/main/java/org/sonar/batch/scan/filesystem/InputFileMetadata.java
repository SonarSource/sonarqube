/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.scan.filesystem;

import java.io.Serializable;

/**
 * Additional input file metadata that are stored in a disk storage to save memory
 */
public class InputFileMetadata implements Serializable {

  private String hash;
  private int nonBlankLines;
  private int[] originalLineOffsets;
  private boolean empty;

  /**
   * Digest hash of the file.
   */
  public String hash() {
    return hash;
  }

  public int nonBlankLines() {
    return nonBlankLines;
  }

  public int[] originalLineOffsets() {
    return originalLineOffsets;
  }

  public InputFileMetadata setHash(String hash) {
    this.hash = hash;
    return this;
  }

  public InputFileMetadata setNonBlankLines(int nonBlankLines) {
    this.nonBlankLines = nonBlankLines;
    return this;
  }

  public InputFileMetadata setOriginalLineOffsets(int[] originalLineOffsets) {
    this.originalLineOffsets = originalLineOffsets;
    return this;
  }

  public boolean isEmpty() {
    return this.empty;
  }

  public InputFileMetadata setEmpty(boolean empty) {
    this.empty = empty;
    return this;
  }

}
