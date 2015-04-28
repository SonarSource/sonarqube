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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class UserServiceTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));

  EsClient esClient;
  DbClient dbClient;

  UserService service;

  @Before
  public void setUp() throws Exception {
    service = new UserService(new UserIndex(esTester.client()));
    esTester.truncateIndices();
  }

  @Test
  public void get_nullable_by_login() throws Exception {
    createSampleUser();

    assertThat(service.getNullableByLogin("user")).isNotNull();
  }

  @Test
  public void get_by_login() throws Exception {
    createSampleUser();

    assertThat(service.getByLogin("user")).isNotNull();
  }

  private void createSampleUser() throws Exception {
    esTester.putDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, new UserDoc()
      .setLogin("user")
      .setName("User")
      .setEmail("user@mail.com")
      .setScmAccounts(newArrayList("u1", "u_1")));
  }

}
