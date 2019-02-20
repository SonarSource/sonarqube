/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

/**
 * Clean-up the db table "es_queue" when documents
 * are successfully indexed so that the recovery
 * daemon does not re-index them.
 *
 * This implementation assumes that one row in table es_queue
 * is associated to one index document and that es_queue.doc_id
 * equals document id.
 */
public class OneToOneResilientIndexingListener implements IndexingListener {

  private final DbClient dbClient;
  private final DbSession dbSession;
  private final Multimap<DocId, EsQueueDto> itemsById;

  public OneToOneResilientIndexingListener(DbClient dbClient, DbSession dbSession, Collection<EsQueueDto> items) {
    this.dbClient = dbClient;
    this.dbSession = dbSession;
    this.itemsById = items.stream()
      .collect(MoreCollectors.index(i -> {
        IndexType.SimpleIndexMainType mainType = IndexType.parseMainType(i.getDocType());
        return new DocId(mainType.getIndex(), mainType.getType(), i.getDocId());
      }, Function.identity()));
  }

  @Override
  public void onSuccess(List<DocId> successDocIds) {
    if (!successDocIds.isEmpty()) {
      Collection<EsQueueDto> itemsToDelete = successDocIds.stream()
        .map(itemsById::get)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .collect(MoreCollectors.toArrayList());
      dbClient.esQueueDao().delete(dbSession, itemsToDelete);
      dbSession.commit();
    }
  }

  @Override
  public void onFinish(IndexingResult result) {
    // nothing to do, items that have been successfully indexed
    // are already deleted from db (see method onSuccess())
  }
}
