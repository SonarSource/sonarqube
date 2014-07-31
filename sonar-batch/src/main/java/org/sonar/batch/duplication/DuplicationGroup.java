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
package org.sonar.batch.duplication;

import java.util.ArrayList;
import java.util.List;

public class DuplicationGroup {

  public static class Block {
    private final String resourceKey;
    private final int startLine;
    private final int length;

    public Block(String resourceKey, int startLine, int length) {
      this.resourceKey = resourceKey;
      this.startLine = startLine;
      this.length = length;
    }

    public String resourceKey() {
      return resourceKey;
    }

    public int startLine() {
      return startLine;
    }

    public int length() {
      return length;
    }
  }

  private final Block originBlock;

  private List<Block> duplicates = new ArrayList<DuplicationGroup.Block>();

  public DuplicationGroup(Block originBlock) {
    this.originBlock = originBlock;
  }

  public void setDuplicates(List<Block> duplicates) {
    this.duplicates = duplicates;
  }

  public DuplicationGroup addDuplicate(Block anotherBlock) {
    this.duplicates.add(anotherBlock);
    return this;
  }

  public Block originBlock() {
    return originBlock;
  }

  public List<Block> duplicates() {
    return duplicates;
  }

}
