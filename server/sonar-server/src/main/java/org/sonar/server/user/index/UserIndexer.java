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
package org.sonar.server.user.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ResilientIndexer;

import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.server.user.index.UserIndexDefinition.INDEX_TYPE_USER;

public class UserIndexer implements ResilientIndexer {

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
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ListMultimap<String, String> organizationUuidsByLogin = ArrayListMultimap.create();
      dbClient.organizationMemberDao().selectAllForUserIndexing(dbSession, organizationUuidsByLogin::put);

      BulkIndexer bulkIndexer = newBulkIndexer(Size.LARGE, IndexingListener.FAIL_ON_ERROR);
      bulkIndexer.start();
      dbClient.userDao().scrollAll(dbSession,
        // only index requests, no deletion requests.
        // Deactivated users are not deleted but updated.
        u -> bulkIndexer.add(newIndexRequest(u, organizationUuidsByLogin)));
      bulkIndexer.stop();
    }
  }

  public void commitAndIndex(DbSession dbSession, UserDto user) {
    commitAndIndexByLogins(dbSession, singletonList(user.getLogin()));
  }

  public void commitAndIndex(DbSession dbSession, Collection<UserDto> users) {
    commitAndIndexByLogins(dbSession, Collections2.transform(users, UserDto::getLogin));
  }

  public void commitAndIndexByLogins(DbSession dbSession, Collection<String> logins) {
    List<EsQueueDto> items = logins.stream()
      .map(l -> EsQueueDto.create(INDEX_TYPE_USER.format(), l))
      .collect(MoreCollectors.toArrayList());

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, logins, items);
  }

  /**
   * Entry point for Byteman tests. See directory tests/resilience.
   * The parameter "logins" is used only by the Byteman script.
   */
  private void postCommit(DbSession dbSession, Collection<String> logins, Collection<EsQueueDto> items) {
    index(dbSession, items);
  }

  /**
   * @return the number of items that have been successfully indexed
   */
  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }
    Set<String> logins = items
      .stream()
      .map(EsQueueDto::getDocId)
      .collect(toHashSet(items.size()));

    ListMultimap<String, String> organizationUuidsByLogin = ArrayListMultimap.create();
    dbClient.organizationMemberDao().selectForUserIndexing(dbSession, logins, organizationUuidsByLogin::put);

    BulkIndexer bulkIndexer = newBulkIndexer(Size.REGULAR, new OneToOneResilientIndexingListener(dbClient, dbSession, items));
    bulkIndexer.start();
    dbClient.userDao().scrollByLogins(dbSession, logins,
      // only index requests, no deletion requests.
      // Deactivated users are not deleted but updated.
      u -> {
        logins.remove(u.getLogin());
        bulkIndexer.add(newIndexRequest(u, organizationUuidsByLogin));
      });

    // the remaining logins reference rows that don't exist in db. They must
    // be deleted from index.
    logins.forEach(l -> bulkIndexer.addDeletion(INDEX_TYPE_USER, l));
    return bulkIndexer.stop();
  }

  private BulkIndexer newBulkIndexer(Size bulkSize, IndexingListener listener) {
    return new BulkIndexer(esClient, INDEX_TYPE_USER, bulkSize, listener);
  }

  private static IndexRequest newIndexRequest(UserDto user, ListMultimap<String, String> organizationUuidsByLogins) {
    UserDoc doc = new UserDoc(Maps.newHashMapWithExpectedSize(8));
    // all the keys must be present, even if value is null
    doc.setLogin(user.getLogin());
    doc.setName(user.getName());
    doc.setEmail(user.getEmail());
    doc.setActive(user.isActive());
    doc.setScmAccounts(UserDto.decodeScmAccounts(user.getScmAccounts()));
    doc.setOrganizationUuids(organizationUuidsByLogins.get(user.getLogin()));

    return new IndexRequest(INDEX_TYPE_USER.getIndex(), INDEX_TYPE_USER.getType())
      .id(doc.getId())
      .routing(doc.getRouting())
      .source(doc.getFields());
  }
}
