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
package org.sonar.server.qualityprofile;

import org.elasticsearch.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.activity.Activity;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleChangeMediumTest {


  @ClassRule
  public static ServerTester tester = new ServerTester();

  ActivityService service = tester.get(ActivityService.class);
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
  public void insert_find_active_rule_change() {
    ActiveRuleKey key = ActiveRuleKey.of(
      QualityProfileKey.of("profile", "java"),
      RuleKey.of("repository", "rule"));
    ActiveRuleChange change = ActiveRuleChange
      .createFor(ActiveRuleChange.Type.ACTIVATED, key)
      .setInheritance(ActiveRule.Inheritance.INHERITED)
      .setSeverity("BLOCKER")
      .setParameter("param1", "value1");

    service.write(dbSession, Activity.Type.QPROFILE, change);
    dbSession.commit();

    // 0. AssertBase case
    assertThat(index.findAll().getHits()).hasSize(1);

    Activity activity = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(activity).isNotNull();
    assertThat(activity.details().get("key")).isEqualTo(key.toString());
  }
}