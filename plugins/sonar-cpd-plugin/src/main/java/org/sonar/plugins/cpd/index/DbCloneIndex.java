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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonar.api.database.DatabaseSession;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.AbstractCloneIndex;
import org.sonar.jpa.entity.CloneBlock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DbCloneIndex extends AbstractCloneIndex {

  private final Map<ByteArray, List<Block>> cache = Maps.newHashMap();

  private DatabaseSession session;
  private int currentSnapshotId;
  private Integer lastSnapshotId;

  public DbCloneIndex(DatabaseSession session, Integer currentSnapshotId, Integer lastSnapshotId) {
    this.session = session;
    this.currentSnapshotId = currentSnapshotId;
    this.lastSnapshotId = lastSnapshotId;
  }

  public void prepareCache(String resourceKey) {
    String sql = "SELECT block.id, hash, block.snapshot_id, resource_key, index_in_file, start_line, end_line FROM clone_blocks AS block, snapshots AS snapshot" +
        " WHERE block.snapshot_id=snapshot.id AND snapshot.islast=true" +
        " AND hash IN ( SELECT hash FROM clone_blocks WHERE resource_key = :resource_key AND snapshot_id = :current_snapshot_id )";
    if (lastSnapshotId != null) {
      // Filter for blocks from previous snapshot of current project
      sql += " AND snapshot.id != " + lastSnapshotId;
    }
    List<CloneBlock> blocks = session.getEntityManager()
        .createNativeQuery(sql, CloneBlock.class)
        .setParameter("resource_key", resourceKey)
        .setParameter("current_snapshot_id", currentSnapshotId)
        .getResultList();

    cache.clear();
    for (CloneBlock dbBlock : blocks) {
      Block block = new Block(dbBlock.getResourceKey(), new ByteArray(dbBlock.getHash()), dbBlock.getIndexInFile(), dbBlock.getStartLine(), dbBlock.getEndLine());

      List<Block> sameHash = cache.get(block.getBlockHash());
      if (sameHash == null) {
        sameHash = Lists.newArrayList();
        cache.put(block.getBlockHash(), sameHash);
      }
      sameHash.add(block);
    }
  }

  public Collection<Block> getByResourceId(String resourceId) {
    throw new UnsupportedOperationException();
  }

  public Collection<Block> getBySequenceHash(ByteArray sequenceHash) {
    List<Block> result = cache.get(sequenceHash);
    if (result != null) {
      return result;
    } else {
      // not in cache
      return Collections.emptyList();
    }
  }

  public void insert(Block block) {
    CloneBlock dbBlock = new CloneBlock(currentSnapshotId,
        block.getBlockHash().toString(),
        block.getResourceId(),
        block.getIndexInFile(),
        block.getFirstLineNumber(),
        block.getLastLineNumber());
    session.save(dbBlock);
  }

}
