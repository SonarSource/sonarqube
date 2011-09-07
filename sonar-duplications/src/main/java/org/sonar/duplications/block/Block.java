/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.block;

/**
 * Represents part of source code between two lines.
 * If two blocks have the same {@link #getBlockHash() hash}, then we assume that there is a duplication in a code, which they represent.
 */
public final class Block {

  private final String resourceId;
  private final ByteArray blockHash;
  private final int indexInFile;
  private final int firstLineNumber;
  private final int lastLineNumber;

  /**
   * Cache for hash code.
   */
  private int hash;

  public Block(String resourceId, ByteArray blockHash, int indexInFile, int firstLineNumber, int lastLineNumber) {
    this.resourceId = resourceId;
    this.blockHash = blockHash;
    this.indexInFile = indexInFile;
    this.firstLineNumber = firstLineNumber;
    this.lastLineNumber = lastLineNumber;
  }

  public Block(int indexInFile, int firstLineNumber, int lastLineNumber, String resourceId, String hash) {
    this(resourceId, new ByteArray(hash), indexInFile, firstLineNumber, lastLineNumber);
  }

  public String getHashHex() {
    return getBlockHash().toString();
  }

  public String getResourceId() {
    return resourceId;
  }

  public ByteArray getBlockHash() {
    return blockHash;
  }

  public int getIndexInFile() {
    return indexInFile;
  }

  public int getFirstLineNumber() {
    return firstLineNumber;
  }

  public int getLastLineNumber() {
    return lastLineNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Block)) {
      return false;
    }
    Block other = (Block) obj;
    return resourceId.equals(other.resourceId)
        && blockHash.equals(other.blockHash)
        && indexInFile == other.indexInFile
        && firstLineNumber == other.firstLineNumber
        && lastLineNumber == other.lastLineNumber;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = resourceId.hashCode();
      h = 31 * h + blockHash.hashCode();
      h = 31 * h + indexInFile;
      h = 31 * h + firstLineNumber;
      h = 31 * h + lastLineNumber;
      hash = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return "'" + resourceId + "'[" + indexInFile + "|" + firstLineNumber + "-" + lastLineNumber + "]:" + blockHash;
  }

}
