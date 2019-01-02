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

import java.util.Collection;
import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

/**
 * Clean-up the db table "es_queue" when documents
 * are successfully indexed so that the recovery
 * daemon does not re-index them.
 *
 * This implementation assumes that one row in table es_queue
 * is associated to multiple index documents. The column
 * es_queue.doc_id is not equal to ids of documents.
 *
 * Important. All the provided EsQueueDto instances must
 * reference documents involved in the BulkIndexer call, otherwise
 * some items will be marked as successfully processed, even
 * if not processed at all.
 */
public class OneToManyResilientIndexingListener implements IndexingListener {

  private final DbClient dbClient;
  private final DbSession dbSession;
  private final Collection<EsQueueDto> items;

  public OneToManyResilientIndexingListener(DbClient dbClient, DbSession dbSession, Collection<EsQueueDto> items) {
    this.dbClient = dbClient;
    this.dbSession = dbSession;
    this.items = items;
  }

  @Override
  public void onSuccess(List<DocId> successDocIds) {
    // it's not possible to deduce which ES_QUEUE row
    // must be deleted. For example:
    // items: project P1
    // successDocIds: issue 1 and issue 2
    // --> no relationship between items and successDocIds
  }

  @Override
  public void onFinish(IndexingResult result) {
    if (result.isSuccess()) {
      dbClient.esQueueDao().delete(dbSession, items);
      dbSession.commit();
    }
  }
}
