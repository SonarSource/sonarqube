/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.es;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

import static java.util.Arrays.asList;

/**
 * Delegates events to indexers, that may want to reindex something based on the event.
 */
public class IndexersImpl implements Indexers {

  private final List<EventIndexer> indexers;

  public IndexersImpl(EventIndexer... indexers) {
    this.indexers = asList(indexers);
  }

  /**
   * Asks all indexers to queue an indexation request in the DB to index the specified entities, if needed (according to
   * "cause" parameter), then call all indexers to index the requests.
   * The idea is that the indexation requests are committed into the DB at the same time as the data that caused those requests
   * to be created, for consistency.
   * If the indexation fails, the indexation requests will still be in the DB and can be processed again later.
   */
  @Override
  public void commitAndIndexOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, EntityEvent cause) {
    indexOnEvent(dbSession, indexer -> indexer.prepareForRecoveryOnEntityEvent(dbSession, entityUuids, cause));
  }

  /**
   * Asks all indexers to queue an indexation request in the DB to index the specified branches, if needed (according to
   * "cause" parameter), then call all indexers to index the requests.
   * The idea is that the indexation requests are committed into the DB at the same time as the data that caused those requests
   * to be created, for consistency.
   * If the indexation fails, the indexation requests will still be in the DB and can be processed again later.
   */
  @Override
  public void commitAndIndexOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, BranchEvent cause) {
    indexOnEvent(dbSession, indexer -> indexer.prepareForRecoveryOnBranchEvent(dbSession, branchUuids, cause));
  }


  private void indexOnEvent(DbSession dbSession, Function<EventIndexer, Collection<EsQueueDto>> esQueueSupplier) {
    Map<EventIndexer, Collection<EsQueueDto>> itemsByIndexer = new IdentityHashMap<>();
    indexers.forEach(i -> itemsByIndexer.put(i, esQueueSupplier.apply(i)));
    dbSession.commit();

    // ensure that indexer#index() is called only with the item type that it supports
    itemsByIndexer.forEach((indexer, items) -> indexer.index(dbSession, items));
  }
}
