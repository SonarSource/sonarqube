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

import com.google.common.collect.Multimap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.activity.Activity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;

public class QProfileServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession dbSession;
  QProfileService service;
  RuleActivator activator;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    service = tester.get(QProfileService.class);
    activator = tester.get(RuleActivator.class);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    db.ruleDao().insert(dbSession, xooRule1);

    // create pre-defined profiles P1 and P2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(), QProfileTesting.newXooP2());
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void count_by_all_profiles() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    service.activate(XOO_P2_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));

    dbSession.clearCache();

    Map<String, Long> counts = service.countAllActiveRules();
    assertThat(counts).hasSize(2);
    assertThat(counts.keySet()).containsOnly(XOO_P1_KEY, XOO_P2_KEY);
    assertThat(counts.values()).containsOnly(1L, 1L);
  }

  @Test
  public void stat_for_all_profiles() {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("MINOR"));
    service.activate(XOO_P2_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    dbSession.clearCache();

    Map<String, Multimap<String, FacetValue>> stats = service.getAllProfileStats();

    assertThat(stats.size()).isEqualTo(2);
    assertThat(stats.get(XOO_P1_KEY).size()).isEqualTo(3);
    assertThat(stats.get(XOO_P1_KEY).get(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field()).size()).isEqualTo(1);
    assertThat(stats.get(XOO_P1_KEY).get(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field()).size()).isEqualTo(1);
    assertThat(stats.get(XOO_P1_KEY).get("countActiveRules").size()).isEqualTo(1);
  }

  @Test
  public void count_by_deprecated() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    // create deprecated rule
    RuleDto deprecatedXooRule = RuleTesting.newDto(RuleKey.of("xoo", "deprecated1"))
      .setSeverity("MINOR").setLanguage("xoo").setStatus(RuleStatus.DEPRECATED);
    db.ruleDao().insert(dbSession, deprecatedXooRule);
    dbSession.commit();

    // active some rules
    service.activate(XOO_P1_KEY, new RuleActivation(deprecatedXooRule.getKey()).setSeverity("BLOCKER"));
    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    dbSession.commit();

    assertThat(service.countDeprecatedActiveRulesByProfile(XOO_P1_KEY)).isEqualTo(1);
  }

  @Test
  public void search_qprofile_activity() throws InterruptedException {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    // We need an actual rule in DB to test RuleName in Activity
    RuleDto rule = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);

    tester.get(ActivityService.class).write(dbSession, Activity.Type.QPROFILE,
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
        .setSeverity(Severity.MAJOR)
        .setParameter("max", "10")
    );
    dbSession.commit();

    List<QProfileActivity> activities = service.findActivities(new QProfileActivityQuery(), new QueryOptions());
    assertThat(activities).hasSize(1);

    QProfileActivity activity = activities.get(0);
    assertThat(activity.type()).isEqualTo(Activity.Type.QPROFILE);
    assertThat(activity.action()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    assertThat(activity.ruleKey()).isEqualTo(RuleTesting.XOO_X1);
    assertThat(activity.profileKey()).isEqualTo(XOO_P1_KEY);
    assertThat(activity.parameters().get("max")).isEqualTo("10");
    assertThat(activity.severity()).isEqualTo(Severity.MAJOR);
    assertThat(activity.ruleName()).isEqualTo(rule.getName());
    assertThat(activity.login()).isEqualTo("me");
    assertThat(activity.authorName()).isNull();
  }

  @Test
  public void search_qprofile_activity_with_rule_not_found() throws InterruptedException {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    RuleKey ruleKey = RuleKey.of("xoo", "deleted_rule");

    tester.get(ActivityService.class).write(dbSession, Activity.Type.QPROFILE,
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, ruleKey))
        .setSeverity(Severity.MAJOR)
        .setParameter("max", "10")
    );
    dbSession.commit();

    List<QProfileActivity> activities = service.findActivities(new QProfileActivityQuery(), new QueryOptions());
    assertThat(activities).hasSize(1);

    QProfileActivity activity = activities.get(0);
    assertThat(activity.ruleKey()).isEqualTo(ruleKey);
    assertThat(activity.ruleName()).isNull();
  }

}
