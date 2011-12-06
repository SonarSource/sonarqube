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
package org.sonar.duplications.detector.suffixtree;

import java.util.List;

import org.sonar.duplications.block.Block;

import com.google.common.collect.Lists;

public class GeneralisedHashText extends TextSet {

  public GeneralisedHashText(List<Block>... blocks) {
    super(blocks.length);

    for (int i = 0; i < blocks.length; i++) {
      addAll(blocks[i]);
      addTerminator();
    }
    finish();
  }

  private int count;
  private List<Integer> sizes = Lists.newArrayList();

  public void addBlock(Block block) {
    symbols.add(block);
  }

  public void addAll(List<Block> list) {
    symbols.addAll(list);
  }

  public void addTerminator() {
    symbols.add(new Terminator(count));
    sizes.add(symbols.size());
    count++;
  }

  public void finish() {
    super.lens = new int[sizes.size()];
    for (int i = 0; i < sizes.size(); i++) {
      super.lens[i] = sizes.get(i);
    }
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

}
