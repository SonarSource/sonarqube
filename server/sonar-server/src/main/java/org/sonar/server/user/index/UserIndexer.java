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
import java.util.List;
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

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
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
    doIndex(newBulkIndexer(Size.LARGE), null);
  }

  public void index(String login) {
    requireNonNull(login);
    doIndex(newBulkIndexer(Size.REGULAR), singletonList(login));
  }

  public void index(List<String> logins) {
    requireNonNull(logins);
    if (logins.isEmpty()) {
      return;
    }

    doIndex(newBulkIndexer(Size.REGULAR), logins);
  }

  private void doIndex(BulkIndexer bulk, @Nullable List<String> logins) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (logins == null) {
        processLogins(bulk, dbSession, null);
      } else {
        executeLargeInputsWithoutOutput(logins, l -> processLogins(bulk, dbSession, l));
      }
    }
  }

  private void processLogins(BulkIndexer bulk, DbSession dbSession, @Nullable List<String> logins) {
    try (UserResultSetIterator rowIt = UserResultSetIterator.create(dbClient, dbSession, logins)) {
      processResultSet(bulk, rowIt);
    }
  }

  private static void processResultSet(BulkIndexer bulk, Iterator<UserDoc> users) {
    bulk.start();
    while (users.hasNext()) {
      UserDoc user = users.next();
      bulk.add(newIndexRequest(user));
    }
    bulk.stop();
  }

  private BulkIndexer newBulkIndexer(Size bulkSize) {
    return new BulkIndexer(esClient, UserIndexDefinition.INDEX_TYPE_USER.getIndex(), bulkSize);
  }

  private static IndexRequest newIndexRequest(UserDoc user) {
    return new IndexRequest(UserIndexDefinition.INDEX_TYPE_USER.getIndex(), UserIndexDefinition.INDEX_TYPE_USER.getType(), user.login())
      .source(user.getFields());
  }

}
