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
package org.sonar.duplications.block;

/**
 * Represents part of source code between two lines.
 * If two blocks have the same {@link #getBlockHash() hash}, then we assume that there is a duplication in a code, which they represent.
 */
public final class Block {

  private final String resourceId;
  private final ByteArray blockHash;
  private final int indexInFile;

  private final int startLine;
  private final int endLine;

  private int startUnit;
  private int endUnit;

  /**
   * Cache for hash code.
   */
  private int hash;

  /**
   * @since 2.14
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * <p>Instances can be reused - it is safe to call {@link #build}
   * multiple times to build multiple blocks in series.</p>
   *
   * @since 2.14
   */
  public static final class Builder {

    private String resourceId;
    private ByteArray blockHash;
    private int indexInFile;

    private int startLine;
    private int endLine;

    private int startUnit;
    private int endUnit;

    public Builder setResourceId(String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public Builder setBlockHash(ByteArray blockHash) {
      this.blockHash = blockHash;
      return this;
    }

    public Builder setIndexInFile(int index) {
      this.indexInFile = index;
      return this;
    }

    public Builder setLines(int start, int end) {
      this.startLine = start;
      this.endLine = end;
      return this;
    }

    public Builder setUnit(int start, int end) {
      this.startUnit = start;
      this.endUnit = end;
      return this;
    }

    public Block build() {
      return new Block(this);
    }
  }

  private Block(Builder builder) {
    this.resourceId = builder.resourceId;
    this.blockHash = builder.blockHash;
    this.indexInFile = builder.indexInFile;

    this.startLine = builder.startLine;
    this.endLine = builder.endLine;

    this.startUnit = builder.startUnit;
    this.endUnit = builder.endUnit;
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

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  /**
   * @since 2.14
   */
  public int getStartUnit() {
    return startUnit;
  }

  /**
   * @since 2.14
   */
  public int getEndUnit() {
    return endUnit;
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
      && startLine == other.startLine
      && endLine == other.endLine;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = resourceId.hashCode();
      h = 31 * h + blockHash.hashCode();
      h = 31 * h + indexInFile;
      h = 31 * h + startLine;
      h = 31 * h + endLine;
      hash = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return "'" + resourceId + "'[" + indexInFile + "|" + startLine + "-" + endLine + "]:" + blockHash;
  }

}
