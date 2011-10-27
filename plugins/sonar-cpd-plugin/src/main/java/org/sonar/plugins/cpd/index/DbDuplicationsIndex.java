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

import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.persistence.dao.DuplicationDao;
import org.sonar.persistence.model.DuplicationUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DbDuplicationsIndex {

  private final Map<ByteArray, Collection<Block>> cache = Maps.newHashMap();

  private final ResourcePersister resourcePersister;
  private final int currentProjectSnapshotId;
  private final Integer lastSnapshotId;

  private DuplicationDao dao;

  public DbDuplicationsIndex(ResourcePersister resourcePersister, Project currentProject, DuplicationDao dao) {
    this.dao = dao;
    this.resourcePersister = resourcePersister;
    Snapshot currentSnapshot = resourcePersister.getSnapshotOrFail(currentProject);
    Snapshot lastSnapshot = resourcePersister.getLastSnapshot(currentSnapshot, false);
    this.currentProjectSnapshotId = currentSnapshot.getId();
    this.lastSnapshotId = lastSnapshot == null ? null : lastSnapshot.getId();
  }

  /**
   * For tests.
   */
  DbDuplicationsIndex(DuplicationDao dao, ResourcePersister resourcePersister, Integer currentProjectSnapshotId, Integer prevSnapshotId) {
    this.dao = dao;
    this.resourcePersister = resourcePersister;
    this.currentProjectSnapshotId = currentProjectSnapshotId;
    this.lastSnapshotId = prevSnapshotId;
  }

  int getSnapshotIdFor(Resource resource) {
    return resourcePersister.getSnapshotOrFail(resource).getId();
  }

  public void prepareCache(Resource resource) {
    int resourceSnapshotId = getSnapshotIdFor(resource);
    List<DuplicationUnit> units = dao.selectCandidates(resourceSnapshotId, lastSnapshotId);
    cache.clear();
    // TODO Godin: maybe remove conversion of units to blocks?
    for (DuplicationUnit unit : units) {
      String hash = unit.getHash();
      String resourceKey = unit.getResourceKey();
      int indexInFile = unit.getIndexInFile();
      int startLine = unit.getStartLine();
      int endLine = unit.getEndLine();

      // TODO Godin: in fact we could work directly with id instead of key - this will allow to decrease memory consumption
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

    // TODO Godin: maybe remove conversion of blocks to units?
    List<DuplicationUnit> units = Lists.newArrayList();
    for (Block block : blocks) {
      DuplicationUnit unit = new DuplicationUnit(
          currentProjectSnapshotId,
          resourceSnapshotId,
          block.getBlockHash().toString(),
          block.getIndexInFile(),
          block.getFirstLineNumber(),
          block.getLastLineNumber());
      units.add(unit);
    }

    dao.insert(units);
  }

}
