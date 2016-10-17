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
package org.sonar.server.permission.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;


public class AuthorizationIndexerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Test
  public void index_nothing() {
    AuthorizationIndexer indexer = createIndexer();
    indexer.index();

    assertThat(esTester.countDocuments("issues", "authorization")).isZero();
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    AuthorizationIndexer indexer = createIndexer();
    indexer.doIndex(0L);

    List<SearchHit> docs = esTester.getDocuments("issues", "authorization");
    assertThat(docs).hasSize(1);
    SearchHit doc = docs.get(0);
    assertThat(doc.getSource().get("project")).isEqualTo("ABC");
    assertThat((Collection) doc.getSource().get("groups")).containsOnly("devs", "Anyone");
    assertThat((Collection) doc.getSource().get("users")).containsOnly("user1");

    // delete project
    indexer.deleteProject("ABC", true);

    assertThat(esTester.countDocuments("issues", "issueAuthorization")).isZero();
  }

  @Test
  public void do_not_fail_when_deleting_unindexed_project() {
    AuthorizationIndexer indexer = createIndexer();
    indexer.deleteProject("UNKNOWN", true);
    assertThat(esTester.countDocuments("issues", "authorization")).isZero();
  }

  @Test
  public void delete_permissions() {
    AuthorizationIndexer indexer = createIndexer();
    AuthorizationDao.Dto authorization = new AuthorizationDao.Dto("ABC", System.currentTimeMillis());
    authorization.addUser("guy");
    authorization.addGroup("dev");
    indexer.index(Arrays.asList(authorization));

    // has permissions
    assertThat(esTester.countDocuments("issues", "authorization")).isEqualTo(1);

    // remove permissions -> dto has no users nor groups
    authorization = new AuthorizationDao.Dto("ABC", System.currentTimeMillis());
    indexer.index(Arrays.asList(authorization));

    List<SearchHit> docs = esTester.getDocuments("issues", "authorization");
    assertThat(docs).hasSize(1);
    assertThat((Collection)docs.get(0).sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_USERS)).hasSize(0);
    assertThat((Collection)docs.get(0).sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS)).hasSize(0);
  }

  private AuthorizationIndexer createIndexer() {
    return new AuthorizationIndexer(new DbClient(dbTester.database(), dbTester.myBatis()), esTester.client());
  }
}
