/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.util.Optional.ofNullable;

public class ActiveRuleIteratorForMultipleChunks implements ActiveRuleIterator {

  private final DbClient dbClient;
  private final DbSession dbSession;
  private final Iterator<List<Integer>> iteratorOverChunks;
  private ActiveRuleIteratorForSingleChunk currentChunk;

  public ActiveRuleIteratorForMultipleChunks(DbClient dbClient, DbSession dbSession, Collection<Integer> activeRuleIds) {
    this.dbClient = dbClient;
    this.dbSession = dbSession;
    this.iteratorOverChunks = DatabaseUtils.toUniqueAndSortedPartitions(activeRuleIds).iterator();
  }

  @Override
  public boolean hasNext() {
    if (currentChunk != null && currentChunk.hasNext()) {
      return true;
    }
    return iteratorOverChunks.hasNext();
  }

  @Override
  public ActiveRuleDoc next() {
    if (currentChunk == null || !currentChunk.hasNext()) {
      currentChunk = nextChunk();
    }
    return currentChunk.next();
  }

  private ActiveRuleIteratorForSingleChunk nextChunk() {
    List<Integer> nextInput = iteratorOverChunks.next();
    return new ActiveRuleIteratorForSingleChunk(dbClient, dbSession, nextInput);
  }

  @Override
  public void close() {
    ofNullable(currentChunk).ifPresent(ActiveRuleIterator::close);
  }
}
