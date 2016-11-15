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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings()));

  @Test
  public void index_nothing() {
    UserIndexer indexer = createIndexer();
    indexer.index();
    assertThat(esTester.countDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER)).isEqualTo(0L);
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    UserIndexer indexer = createIndexer();
    indexer.index();

    List<UserDoc> docs = esTester.getDocuments("users", "user", UserDoc.class);
    assertThat(docs).hasSize(1);
    UserDoc doc = docs.get(0);
    assertThat(doc.login()).isEqualTo("user1");
    assertThat(doc.name()).isEqualTo("User1");
    assertThat(doc.email()).isEqualTo("user1@mail.com");
    assertThat(doc.active()).isTrue();
    assertThat(doc.scmAccounts()).containsOnly("user_1", "u1");
    assertThat(doc.createdAt()).isEqualTo(1500000000000L);
    assertThat(doc.updatedAt()).isEqualTo(1500000000000L);
  }

  private UserIndexer createIndexer() {
    return new UserIndexer(system2, new DbClient(dbTester.database(), dbTester.myBatis()), esTester.client());
  }
}
