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
package org.sonar.duplications.detector.suffixtree;

import java.util.ArrayList;
import java.util.List;

import org.sonar.duplications.block.Block;


/**
 * Simplifies construction of <a href="http://en.wikipedia.org/wiki/Generalised_suffix_tree">generalised suffix-tree</a>.
 */
public final class TextSet extends AbstractText {

  public static final class Builder {

    private List<Object> symbols = new ArrayList();
    private Integer lengthOfOrigin;
    private int count;

    private Builder() {
    }

    public void add(List<Block> list) {
      symbols.addAll(list);
      symbols.add(new Terminator(count));
      count++;
      if (lengthOfOrigin == null) {
        lengthOfOrigin = symbols.size();
      }
    }

    public TextSet build() {
      return new TextSet(symbols, lengthOfOrigin);
    }

  }

  public static Builder builder() {
    return new Builder();
  }

  private final int lengthOfOrigin;

  private TextSet(List<Object> symbols, int lengthOfOrigin) {
    super(symbols);
    this.lengthOfOrigin = lengthOfOrigin;
  }

  public boolean isInsideOrigin(int pos) {
    return pos < lengthOfOrigin;
  }

  @Override
  public Object symbolAt(int index) {
    Object obj = super.symbolAt(index);
    if (obj instanceof Block) {
      return ((Block) obj).getBlockHash();
    }
    return obj;
  }

  public Block getBlock(int index) {
    return (Block) super.symbolAt(index);
  }

  public static class Terminator {

    private final int stringNumber;

    public Terminator(int i) {
      this.stringNumber = i;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Terminator) && (((Terminator) obj).stringNumber == stringNumber);
    }

    @Override
    public int hashCode() {
      return stringNumber;
    }

    public int getStringNumber() {
      return stringNumber;
    }

    @Override
    public String toString() {
      return "$" + stringNumber;
    }

  }

}
