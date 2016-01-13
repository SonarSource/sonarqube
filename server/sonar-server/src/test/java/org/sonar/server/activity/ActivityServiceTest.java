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
package org.sonar.server.activity;

import java.util.List;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityIndexDefinition;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActivityServiceTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new ActivityIndexDefinition(new Settings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().login();

  System2 system = mock(System2.class);
  ActivityService service;

  @Before
  public void before() {
    DbClient dbClient = db.getDbClient();
    ActivityIndexer indexer = new ActivityIndexer(dbClient, es.client());
    // indexers are disabled by default
    indexer.setEnabled(true);
    service = new ActivityService(dbClient, indexer, userSession);
  }

  @Test
  public void insert_and_index() {
    when(system.now()).thenReturn(1_500_000_000_000L);

    Activity activity = new Activity();
    activity.setType(Activity.Type.ANALYSIS_REPORT);
    activity.setAction("THE_ACTION");
    activity.setMessage("THE_MSG");
    activity.setData("foo", "bar");
    service.save(activity);

    Map<String, Object> dbMap = db.selectFirst("select log_type as \"type\", log_action as \"action\", log_message as \"msg\", data_field as \"data\" from activities");
    assertThat(dbMap).containsEntry("type", "ANALYSIS_REPORT");
    assertThat(dbMap).containsEntry("action", "THE_ACTION");
    assertThat(dbMap).containsEntry("msg", "THE_MSG");
    assertThat(dbMap.get("data")).isEqualTo("foo=bar");

    List<ActivityDoc> docs = es.getDocuments("activities", "activity", ActivityDoc.class);
    assertThat(docs).hasSize(1);
    assertThat(docs.get(0).getKey()).isNotEmpty();
    assertThat(docs.get(0).getAction()).isEqualTo("THE_ACTION");
    assertThat(docs.get(0).getMessage()).isEqualTo("THE_MSG");
    assertThat(docs.get(0).getDetails()).containsOnly(MapEntry.entry("foo", "bar"));
  }

}
