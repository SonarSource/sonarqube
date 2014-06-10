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
package org.sonar.server.log;


import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.log.Log;
import org.sonar.core.log.Loggable;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.log.db.LogDao;
import org.sonar.server.log.index.LogIndex;
import org.sonar.server.tester.ServerTester;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class LogBackendMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  LogService service = tester.get(LogService.class);
  LogDao dao = tester.get(LogDao.class);
  LogIndex index = tester.get(LogIndex.class);
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_find_text_log() throws InterruptedException {
    final String testValue = "hello world";
    service.write(dbSession, Log.Type.ACTIVE_RULE, testValue);
    dbSession.commit();
    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Log log = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(log).isNotNull();
    assertThat(log.message()).isEqualTo(testValue);
  }

  @Test
  public void insert_find_loggable_log() {
    final String testKey = "message";
    final String testValue = "hello world";
    service.write(dbSession, Log.Type.ACTIVE_RULE, new Loggable() {

      @Override
      public Map<String, String> getDetails() {
        return ImmutableMap.of(testKey, testValue);
      }

      @Override
      public Integer getExecutionTime() {
        return 12;
      }
    });
    dbSession.commit();

    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Log log = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(log).isNotNull();
    assertThat(log.details().get(testKey)).isEqualTo(testValue);
    assertThat(log.executionTime()).isEqualTo(12);
  }
}