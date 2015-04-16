/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.index;

import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.test.DbTests;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueAuthorizationIndexerTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    esTester.truncateIndices();
  }

  @Test
  public void index_nothing() throws Exception {
    IssueAuthorizationIndexer indexer = createIndexer();
    indexer.index();

    assertThat(esTester.countDocuments("issues", "authorization")).isZero();
  }

  @Test
  public void index() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    IssueAuthorizationIndexer indexer = createIndexer();
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
  public void do_not_fail_when_deleting_unindexed_project() throws Exception {
    IssueAuthorizationIndexer indexer = createIndexer();
    indexer.deleteProject("UNKNOWN", true);
    assertThat(esTester.countDocuments("issues", "authorization")).isZero();
  }

  @Test
  public void delete_permissions() throws Exception {
    IssueAuthorizationIndexer indexer = createIndexer();
    IssueAuthorizationDao.Dto authorization = new IssueAuthorizationDao.Dto("ABC", System.currentTimeMillis());
    authorization.addUser("guy");
    authorization.addGroup("dev");
    indexer.index(Arrays.asList(authorization));

    // has permissions
    assertThat(esTester.countDocuments("issues", "authorization")).isEqualTo(1);

    // remove permissions -> dto has no users nor groups
    authorization = new IssueAuthorizationDao.Dto("ABC", System.currentTimeMillis());
    indexer.index(Arrays.asList(authorization));

    List<SearchHit> docs = esTester.getDocuments("issues", "authorization");
    assertThat(docs).hasSize(1);
    assertThat((Collection)docs.get(0).sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_USERS)).hasSize(0);
    assertThat((Collection)docs.get(0).sourceAsMap().get(IssueIndexDefinition.FIELD_AUTHORIZATION_GROUPS)).hasSize(0);
  }

  private IssueAuthorizationIndexer createIndexer() {
    return new IssueAuthorizationIndexer(new DbClient(dbTester.database(), dbTester.myBatis()), esTester.client());
  }
}
