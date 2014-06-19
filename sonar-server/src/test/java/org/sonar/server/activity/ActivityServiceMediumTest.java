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
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.collect.Iterables;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.activity.Activity;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;

import java.util.Iterator;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ActivityServiceMediumTest {

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
  public void find_all() throws InterruptedException {

    service.write(dbSession, Activity.Type.ACTIVE_RULE, testValue);
    dbSession.commit();
    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Activity activity = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(activity).isNotNull();
    assertThat(activity.message()).isEqualTo(testValue);
  }

  @Test
  public void search_message_log() throws InterruptedException {
    service.write(dbSession, Activity.Type.ACTIVE_RULE, testValue);
    dbSession.commit();
    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Result<Activity> result = index.search(service.newActivityQuery(), new QueryOptions());
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getHits().get(0).message()).isEqualTo(testValue);
  }


  @Test
  public void search_activity_log() throws InterruptedException {

    service.write(dbSession, Activity.Type.ACTIVE_RULE, getActivity());
    dbSession.commit();
    assertThat(index.findAll().getTotal()).isEqualTo(1);

    Result<Activity> result = index.search(service.newActivityQuery(), new QueryOptions());
    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getHits().get(0).details().get(test_key)).isEqualTo(test_value);
  }


  @Test
  public void filter_by_type() {
    service.write(dbSession, Activity.Type.NONE, getActivity());
    service.write(dbSession, Activity.Type.SERVER, getActivity());
    service.write(dbSession, Activity.Type.SERVER, testValue);
    service.write(dbSession, Activity.Type.ACTIVE_RULE, getActivity());
    dbSession.commit();

    assertThat(service.search(new ActivityQuery(),
      new QueryOptions()).getHits()).hasSize(4);

    assertThat(service.search(new ActivityQuery()
        .setTypes(ImmutableSet.of(Activity.Type.SERVER)),
      new QueryOptions()).getHits()).hasSize(2);

    assertThat(service.search(new ActivityQuery()
        .setTypes(ImmutableSet.of(Activity.Type.ACTIVE_RULE)),
      new QueryOptions()).getHits()).hasSize(1);
  }

  @Test
  public void filter_by_date() throws InterruptedException {

    ActivityDto activity = ActivityDto.createFor(testValue)
      .setType(Activity.Type.NONE).setAuthor("testing");
    DateTime t0 = new DateTime().minusHours(1);
    activity.setCreatedAt(t0.toDate());
    dao.insert(dbSession, activity);
    dao.insert(dbSession, activity);
    dbSession.commit();
    DateTime t1 = new DateTime();
    activity.setCreatedAt(t1.toDate());
    dao.insert(dbSession, activity);
    dbSession.commit();
    DateTime t2 = new DateTime().plusHours(1);


    assertThat(service.search(new ActivityQuery()
        .setSince(t0.minusSeconds(5).toDate()),
      new QueryOptions()).getHits()).hasSize(3);

    assertThat(service.search(new ActivityQuery()
        .setSince(t1.minusSeconds(5).toDate()),
      new QueryOptions()).getHits()).hasSize(1);

    assertThat(service.search(new ActivityQuery()
        .setSince(t2.minusSeconds(5).toDate()),
      new QueryOptions()).getHits()).hasSize(0);

    assertThat(service.search(new ActivityQuery()
        .setTo(t1.minusSeconds(5).toDate()),
      new QueryOptions()).getHits()).hasSize(2);

    assertThat(service.search(new ActivityQuery()
        .setSince(t1.minusSeconds(5).toDate())
        .setTo(t2.plusSeconds(5).toDate()),
      new QueryOptions()).getHits()).hasSize(1);
  }

  @Test
  public void iterate_all() throws InterruptedException {
    int max = QueryOptions.DEFAULT_LIMIT + 3;
    final String testValue = "hello world";
    for (int i = 0; i < max; i++) {
      service.write(dbSession, Activity.Type.ACTIVE_RULE, testValue + "_" + i);
    }
    dbSession.commit();

    // 0. assert Base case
    assertThat(dao.findAll(dbSession)).hasSize(max);

    Result<Activity> result = index.search(service.newActivityQuery(), new QueryOptions().setScroll(true));
    assertThat(result.getTotal()).isEqualTo(max);
    assertThat(result.getHits()).hasSize(0);
    int count = 0;
    Iterator<Activity> logIterator = result.scroll();
    while (logIterator.hasNext()) {
      count++;
      logIterator.next();
    }
    assertThat(count).isEqualTo(max);
  }


  final String test_key = "hello";
  final String test_value = "world";
  final String testValue = "hello world";

  private ActivityLog getActivity() {
    return new ActivityLog() {
      @Override
      public Map<String, String> getDetails() {
        return ImmutableMap.of(test_key, test_value);
      }

      @Override
      public String getAction() {
        return "myAction";
      }
    };
  }
}
