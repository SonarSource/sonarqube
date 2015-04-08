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
package org.sonar.server.qualityprofile.ws;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;

import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;

public class QProfileChangelogActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private DbClient db;
  private DbSession dbSession;
  private WsTester wsTester;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    db.ruleDao().insert(dbSession, xooRule1);

    // create pre-defined profiles P1 and P2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(), QProfileTesting.newXooP2());

    // create a user for activity author
    UserDto user = new UserDto().setLogin("david").setName("David").setEmail("dav@id.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    db.userDao().insert(dbSession, user);

    dbSession.commit();
    dbSession.clearCache();

    wsTester = new WsTester(tester.get(QProfilesWs.class));
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void changelog_nominal() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("david");
    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
      .setSeverity(Severity.MAJOR)
      .setParameter("max", "10").toActivity());

    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_nominal.json");
  }

  @Test
  public void changelog_with_dates() throws Exception {
    Date yesterday = DateTime.now().minusDays(1).toDate();
    Date tomorrow = DateTime.now().plusDays(1).toDate();

    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("david");
    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
      .setSeverity(Severity.MAJOR)
      .setParameter("max", "10").toActivity());

    // Tests with "since"
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("since", DateUtils.formatDate(yesterday))
      .execute().assertJson(getClass(), "changelog_nominal.json");
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("since", DateUtils.formatDate(tomorrow))
      .execute().assertJson(getClass(), "changelog_empty.json");

    // Tests with "to"
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("to", DateUtils.formatDate(yesterday))
      .execute().assertJson(getClass(), "changelog_empty.json");
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("to", DateUtils.formatDate(tomorrow))
      .execute().assertJson(getClass(), "changelog_nominal.json");
  }

  @Test
  public void changelog_with_pagination() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("david");
    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
      .setSeverity(Severity.MAJOR)
      .setParameter("max", "10").toActivity());
    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
      .setSeverity(Severity.CRITICAL)
      .setParameter("max", "20").toActivity());

    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("ps", "1")
      .execute().assertJson(getClass(), "changelog_page1.json");
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("ps", "1").setParam("p", "2")
      .execute().assertJson(getClass(), "changelog_page2.json");
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("ps", "1").setParam("p", "3")
      .execute().assertJson(getClass(), "changelog_page3.json");
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_profile() throws Exception {
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", "unknown-profile").execute();
  }
}
