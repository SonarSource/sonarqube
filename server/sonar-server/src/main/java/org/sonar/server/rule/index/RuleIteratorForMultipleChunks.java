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
package org.sonar.server.rule.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;

import static java.util.Optional.ofNullable;

public class RuleIteratorForMultipleChunks implements RuleIterator {

  private final DbClient dbClient;
  private final Iterator<List<RuleKey>> iteratorOverChunks;
  private RuleIteratorForSingleChunk currentChunk;

  public RuleIteratorForMultipleChunks(DbClient dbClient, Collection<RuleKey> keys) {
    this.dbClient = dbClient;
    iteratorOverChunks = DatabaseUtils.toUniqueAndSortedPartitions(keys).iterator();
  }

  @Override
  public boolean hasNext() {
    for (;;) {
      if (currentChunk != null && currentChunk.hasNext()) {
        return true;
      }
      if (iteratorOverChunks.hasNext()) {
        currentChunk = nextChunk();
      } else {
        return false;
      }
    }
  }

  @Override
  public RuleDocWithSystemScope next() {
    for (;;) {
      if (currentChunk != null && currentChunk.hasNext()) {
        return currentChunk.next();
      }
      if (iteratorOverChunks.hasNext()) {
        currentChunk = nextChunk();
      } else {
        throw new NoSuchElementException();
      }
    }
  }

  private RuleIteratorForSingleChunk nextChunk() {
    List<RuleKey> nextInput = iteratorOverChunks.next();
    return new RuleIteratorForSingleChunk(dbClient, nextInput);
  }

  @Override
  public void close() {
    ofNullable(currentChunk).ifPresent(RuleIterator::close);
  }
}
