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
package org.sonar.batch.cpd.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.duplication.DuplicationDao;
import org.sonar.core.duplication.DuplicationUnitDto;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

import javax.persistence.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DbDuplicationsIndex {

  private static final String RESOURCE_ID = "resourceId";
  private static final String LAST = "last";

  private final Map<ByteArray, Collection<Block>> cache = Maps.newHashMap();

  private final int currentProjectSnapshotId;
  private final Integer lastSnapshotId;
  private final String languageKey;
  private final DuplicationDao dao;
  private final DatabaseSession session;
  private final ResourceCache resourceCache;

  public DbDuplicationsIndex(Project currentProject, DuplicationDao dao,
    String language, DatabaseSession session, ResourceCache resourceCache) {
    this.dao = dao;
    this.session = session;
    this.resourceCache = resourceCache;
    Snapshot lastSnapshot = getLastSnapshot(currentProject.getId());
    this.currentProjectSnapshotId = resourceCache.get(currentProject.getEffectiveKey()).snapshotId();
    this.lastSnapshotId = lastSnapshot == null ? null : lastSnapshot.getId();
    this.languageKey = language;
  }

  private Snapshot getLastSnapshot(int resourceId) {
    String hql = "SELECT s FROM " + Snapshot.class.getSimpleName() + " s WHERE s.last=:last AND s.resourceId=:resourceId";
    Query query = session.createQuery(hql);
    query.setParameter(LAST, true);
    query.setParameter(RESOURCE_ID, resourceId);
    return session.getSingleResult(query, null);
  }

  int getSnapshotIdFor(InputFile inputFile) {
    return resourceCache.get(((DefaultInputFile) inputFile).key()).snapshotId();
  }

  public void prepareCache(InputFile inputFile) {
    int resourceSnapshotId = getSnapshotIdFor(inputFile);
    List<DuplicationUnitDto> units = dao.selectCandidates(resourceSnapshotId, lastSnapshotId, languageKey);
    cache.clear();
    // TODO Godin: maybe remove conversion of units to blocks?
    for (DuplicationUnitDto unit : units) {
      String hash = unit.getHash();
      String resourceKey = unit.getResourceKey();
      int indexInFile = unit.getIndexInFile();
      int startLine = unit.getStartLine();
      int endLine = unit.getEndLine();

      // TODO Godin: in fact we could work directly with id instead of key - this will allow to decrease memory consumption
      Block block = Block.builder()
        .setResourceId(resourceKey)
        .setBlockHash(new ByteArray(hash))
        .setIndexInFile(indexInFile)
        .setLines(startLine, endLine)
        .build();

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

  public void insert(InputFile inputFile, Collection<Block> blocks) {
    int resourceSnapshotId = getSnapshotIdFor(inputFile);

    // TODO Godin: maybe remove conversion of blocks to units?
    List<DuplicationUnitDto> units = Lists.newArrayList();
    for (Block block : blocks) {
      DuplicationUnitDto unit = new DuplicationUnitDto(
        currentProjectSnapshotId,
        resourceSnapshotId,
        block.getBlockHash().toString(),
        block.getIndexInFile(),
        block.getStartLine(),
        block.getEndLine());
      units.add(unit);
    }

    dao.insert(units);
  }

}
