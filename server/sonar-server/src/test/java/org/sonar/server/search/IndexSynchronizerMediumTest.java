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
package org.sonar.server.search;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;

public class IndexSynchronizerMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  IndexSynchronizer synchronizer;
  DbClient dbClient;
  IndexClient indexClient;
  Platform platform;
  DbSession dbSession;

  @Before
  public void setUp() throws Exception {
    dbClient = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
    platform = tester.get(Platform.class);
    dbSession = dbClient.openSession(false);
    synchronizer = new IndexSynchronizer(dbClient, indexClient);
    tester.clearDbAndIndexes();
  }

  @After
  public void tearDown() throws Exception {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test
  public void can_synchronize() throws Exception {

    int numberOfRules = 100;

    for (int i = 0; i < numberOfRules; i++) {
      dbClient.ruleDao().insert(dbSession, RuleTesting.newDto(RuleKey.of("test", "x" + i)));
    }
    dbSession.commit();

    assertThat(indexClient.get(RuleIndex.class).countAll()).isEqualTo(numberOfRules);
    tester.clearIndexes();
    assertThat(indexClient.get(RuleIndex.class).countAll()).isEqualTo(0);

    synchronizer.synchronize(dbSession, dbClient.ruleDao(), indexClient.get(RuleIndex.class));
    assertThat(indexClient.get(RuleIndex.class).countAll()).isEqualTo(numberOfRules);
  }
}
