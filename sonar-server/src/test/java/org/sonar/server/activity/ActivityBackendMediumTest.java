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
package org.sonar.server.activity;


import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.activity.Activity;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import java.util.Iterator;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ActivityBackendMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  ActivityService service = tester.get(ActivityService.class);
  ActivityDao dao = tester.get(ActivityDao.class);
  ActivityIndex index = tester.get(ActivityIndex.class);
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
    service.write(dbSession, Activity.Type.QPROFILE, testValue);
    dbSession.commit();
    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Activity activity = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(activity).isNotNull();
    assertThat(activity.message()).isEqualTo(testValue);
  }

  @Test
  public void insert_find_loggable_log() {
    final String testKey = "message";
    final String testValue = "hello world";
    service.write(dbSession, Activity.Type.QPROFILE, new ActivityLog() {

      @Override
      public Map<String, String> getDetails() {
        return ImmutableMap.of(testKey, testValue);
      }

      @Override
      public String getAction() {
        return "myAction";
      }
    });
    dbSession.commit();

    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Activity activity = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(activity).isNotNull();
    assertThat(activity.details().get(testKey)).isEqualTo(testValue);
  }

  @Test
  public void massive_insert() {

    // 0 Assert no logs in DB
    assertThat(dao.findAll(dbSession)).hasSize(0);
    int max = 200;
    final String testValue = "hello world";
    for (int i = 0; i < max; i++) {
      service.write(dbSession, Activity.Type.QPROFILE, testValue + "_" + i);
    }
    dbSession.commit();

    // 1. assert both backends have all logs
    assertThat(dao.findAll(dbSession)).hasSize(max);
    assertThat(index.findAll().getHits()).hasSize(max);

    // 2. assert scrollable
    int count = 0;
    Iterator<Activity> logs = index.search(new ActivityQuery(), new QueryOptions().setScroll(true)).scroll();
    while (logs.hasNext()) {
      logs.next();
      count++;
    }
    assertThat(count).isEqualTo(max);
  }
}
