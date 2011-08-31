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
package org.sonar.plugins.cpd.index;

import java.util.Collection;
import java.util.List;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.AbstractCloneIndex;
import org.sonar.duplications.index.CloneIndex;

import com.google.common.collect.Lists;

public class CombinedCloneIndex extends AbstractCloneIndex {

  private final CloneIndex mem;
  private final DbCloneIndex db;

  public CombinedCloneIndex(CloneIndex mem, DbCloneIndex db) {
    this.mem = mem;
    this.db = db;
  }

  public Collection<Block> getByResourceId(String resourceId) {
    db.prepareCache(resourceId);
    return mem.getByResourceId(resourceId);
  }

  public Collection<Block> getBySequenceHash(ByteArray hash) {
    List<Block> result = Lists.newArrayList();
    result.addAll(mem.getBySequenceHash(hash));
    result.addAll(db.getBySequenceHash(hash));
    return result;
  }

  public void insert(Block block) {
    mem.insert(block);
    db.insert(block);
  }

}
