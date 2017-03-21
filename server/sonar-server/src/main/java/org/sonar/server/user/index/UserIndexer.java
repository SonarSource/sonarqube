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
package org.sonar.server.user.index;

import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.StartupIndexer;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.user.index.UserIndexDefinition.INDEX_TYPE_USER;

public class UserIndexer implements StartupIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public UserIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_USER);
  }

  @Override
  public void indexOnStartup(Set<IndexType> emptyIndexTypes) {
    doIndex(null, Size.LARGE);
  }

  public void index(String login) {
    requireNonNull(login);
    doIndex(login, Size.REGULAR);
  }

  private void doIndex(@Nullable String login, Size bulkSize) {
    final BulkIndexer bulk = new BulkIndexer(esClient, UserIndexDefinition.INDEX_TYPE_USER.getIndex());
    bulk.setSize(bulkSize);

    try (DbSession dbSession = dbClient.openSession(false)) {
      try (UserResultSetIterator rowIt = UserResultSetIterator.create(dbClient, dbSession, login)) {
        doIndex(bulk, rowIt);
      }
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
    return new IndexRequest(UserIndexDefinition.INDEX_TYPE_USER.getIndex(), UserIndexDefinition.INDEX_TYPE_USER.getType(), user.login())
      .source(user.getFields());
  }

}
