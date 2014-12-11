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

package org.sonar.server.user;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;

import static org.fest.assertions.Assertions.assertThat;

public class UserServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  EsClient esClient;
  DbClient dbClient;
  DbSession session;

  UserIndexer indexer;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    dbClient = tester.get(DbClient.class);
    esClient = tester.get(EsClient.class);
    session = dbClient.openSession(false);
    indexer = tester.get(UserIndexer.class);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void index() throws Exception {
    UserDto userDto = new UserDto().setLogin("user").setEmail("user@mail.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    dbClient.userDao().insert(session, userDto);
    session.commit();
    assertThat(esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, "user").get().isExists()).isFalse();

    indexer.index();
    assertThat(esClient.prepareGet(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, "user").get().isExists()).isTrue();
  }

}
