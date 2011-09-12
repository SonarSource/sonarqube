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

import javax.persistence.Query;

import org.hibernate.ejb.HibernateQuery;
import org.hibernate.transform.Transformers;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.jpa.entity.DuplicationBlock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DbDuplicationsIndex {

  private final Map<ByteArray, Collection<Block>> cache = Maps.newHashMap();

  private final DatabaseSession session;
  private final ResourcePersister resourcePersister;
  private final int currentProjectSnapshotId;
  private final Integer lastSnapshotId;

  public DbDuplicationsIndex(DatabaseSession session, ResourcePersister resourcePersister, Project currentProject) {
    this.session = session;
    this.resourcePersister = resourcePersister;
    Snapshot currentSnapshot = resourcePersister.getSnapshotOrFail(currentProject);
    Snapshot lastSnapshot = resourcePersister.getLastSnapshot(currentSnapshot, false);
    this.currentProjectSnapshotId = currentSnapshot.getId();
    this.lastSnapshotId = lastSnapshot == null ? null : lastSnapshot.getId();
  }

  /**
   * For tests.
   */
  DbDuplicationsIndex(DatabaseSession session, ResourcePersister resourcePersister, Integer currentProjectSnapshotId, Integer prevSnapshotId) {
    this.session = session;
    this.resourcePersister = resourcePersister;
    this.currentProjectSnapshotId = currentProjectSnapshotId;
    this.lastSnapshotId = prevSnapshotId;
  }

  int getSnapshotIdFor(Resource resource) {
    return resourcePersister.getSnapshotOrFail(resource).getId();
  }

  public void prepareCache(Resource resource) {
    int resourceSnapshotId = getSnapshotIdFor(resource);

    // Order of columns is important - see code below!
    String sql = "SELECT to_blocks.hash, res.kee, to_blocks.index_in_file, to_blocks.start_line, to_blocks.end_line" +
        " FROM duplications_index to_blocks, duplications_index from_blocks, snapshots snapshot, projects res" +
        " WHERE from_blocks.snapshot_id = :resource_snapshot_id" +
        " AND to_blocks.hash = from_blocks.hash" +
        " AND to_blocks.snapshot_id = snapshot.id" +
        " AND snapshot.islast = :is_last" +
        " AND snapshot.project_id = res.id";
    if (lastSnapshotId != null) {
      // Filter for blocks from previous snapshot of current project
      sql += " AND to_blocks.project_snapshot_id != :last_project_snapshot_id";
    }
    Query query = session.getEntityManager().createNativeQuery(sql)
        .setParameter("resource_snapshot_id", resourceSnapshotId)
        .setParameter("is_last", Boolean.TRUE);
    if (lastSnapshotId != null) {
      query.setParameter("last_project_snapshot_id", lastSnapshotId);
    }
    // Ugly hack for mapping results of custom SQL query into plain list (MyBatis is coming soon)
    ((HibernateQuery) query).getHibernateQuery().setResultTransformer(Transformers.TO_LIST);
    List<List<Object>> blocks = query.getResultList();

    cache.clear();
    for (List<Object> dbBlock : blocks) {
      String hash = (String) dbBlock.get(0);
      String resourceKey = (String) dbBlock.get(1);
      int indexInFile = ((Number) dbBlock.get(2)).intValue();
      int startLine = ((Number) dbBlock.get(3)).intValue();
      int endLine = ((Number) dbBlock.get(4)).intValue();

      Block block = new Block(resourceKey, new ByteArray(hash), indexInFile, startLine, endLine);

      // Group blocks by hash
      Collection<Block> sameHash = cache.get(block.getBlockHash());
      if (sameHash == null) {
        sameHash = Lists.newArrayList();
        cache.put(block.getBlockHash(), sameHash);
      }
      sameHash.add(block);
    }
  }

  public Collection<Block> getByHash(ByteArray hash) {
    Collection<Block> result = cache.get(hash);
    if (result != null) {
      return result;
    } else {
      return Collections.emptyList();
    }
  }

  public void insert(Resource resource, Collection<Block> blocks) {
    int resourceSnapshotId = getSnapshotIdFor(resource);
    for (Block block : blocks) {
      DuplicationBlock dbBlock = new DuplicationBlock(
          currentProjectSnapshotId,
          resourceSnapshotId,
          block.getBlockHash().toString(),
          block.getIndexInFile(),
          block.getFirstLineNumber(),
          block.getLastLineNumber());
      session.save(dbBlock);
    }
    session.commit();
  }

}
