/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user.index;

import java.util.Iterator;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

public class UserIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public UserIndexer(System2 system2, DbClient dbClient, EsClient esClient) {
    super(system2, esClient, 300, UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, UserIndexDefinition.FIELD_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    final BulkIndexer bulk = new BulkIndexer(esClient, UserIndexDefinition.INDEX);
    bulk.setLarge(lastUpdatedAt == 0L);

    DbSession dbSession = dbClient.openSession(false);
    try {
      UserResultSetIterator rowIt = UserResultSetIterator.create(dbClient, dbSession, lastUpdatedAt);
      long maxUpdatedAt = doIndex(bulk, rowIt);
      rowIt.close();
      return maxUpdatedAt;
    } finally {
      dbSession.close();
    }
  }

  private static long doIndex(BulkIndexer bulk, Iterator<UserDoc> users) {
    long maxUpdatedAt = 0L;
    bulk.start();
    while (users.hasNext()) {
      UserDoc user = users.next();
      bulk.add(newIndexRequest(user));
      maxUpdatedAt = Math.max(maxUpdatedAt, user.updatedAt());
    }
    bulk.stop();
    return maxUpdatedAt;
  }

  private static IndexRequest newIndexRequest(UserDoc user) {
    return new IndexRequest(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, user.login())
      .source(user.getFields());
  }

}
