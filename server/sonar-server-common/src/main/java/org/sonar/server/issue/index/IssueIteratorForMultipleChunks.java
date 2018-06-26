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
package org.sonar.server.issue.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;

import static java.util.Optional.ofNullable;

public class IssueIteratorForMultipleChunks implements IssueIterator {

  private final DbClient dbClient;
  private final Iterator<List<String>> iteratorOverChunks;
  private IssueIteratorForSingleChunk currentChunk;

  public IssueIteratorForMultipleChunks(DbClient dbClient, Collection<String> issueKeys) {
    this.dbClient = dbClient;
    iteratorOverChunks = DatabaseUtils.toUniqueAndSortedPartitions(issueKeys).iterator();
  }

  @Override
  public boolean hasNext() {
    if (currentChunk != null && currentChunk.hasNext()) {
      return true;
    }
    return iteratorOverChunks.hasNext();
  }

  @Override
  public IssueDoc next() {
    if (currentChunk == null || !currentChunk.hasNext()) {
      currentChunk = nextChunk();
    }
    return currentChunk.next();
  }

  private IssueIteratorForSingleChunk nextChunk() {
    List<String> nextInput = iteratorOverChunks.next();
    return new IssueIteratorForSingleChunk(dbClient, null, nextInput);
  }

  @Override
  public void close() {
    ofNullable(currentChunk).ifPresent(IssueIterator::close);
  }
}
