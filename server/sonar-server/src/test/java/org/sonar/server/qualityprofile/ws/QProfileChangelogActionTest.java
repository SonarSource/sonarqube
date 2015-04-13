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

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityIndexDefinition;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleChange.Type;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;

public class QProfileChangelogActionTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new ActivityIndexDefinition(new Settings()));

  private DbClient db;
  private DbSession dbSession;
  private WsTester wsTester;
  private String login;

  @Before
  public void before() {
    dbTester.truncateTables();
    esTester.truncateIndices();

    System2 system = mock(System2.class);

    db = new DbClient(dbTester.database(), dbTester.myBatis(), new RuleDao(system), new QualityProfileDao(dbTester.myBatis(), system), new UserDao(dbTester.myBatis(), system));
    dbSession = db.openSession(false);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    db.ruleDao().insert(dbSession, xooRule1);

    // create pre-defined profiles P1 and P2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(), QProfileTesting.newXooP2());

    login = "david";
    UserDto user = new UserDto().setLogin(login).setName("David").setEmail("dav@id.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    db.userDao().insert(dbSession, user);

    dbSession.commit();
    dbSession.clearCache();

    wsTester = new WsTester(new QProfilesWs(mock(RuleActivationActions.class), mock(BulkRuleActivationActions.class), mock(ProjectAssociationActions.class),
      new QProfileChangelogAction(db, new ActivityIndex(esTester.client()), new QProfileFactory(db), LanguageTesting.newLanguages("xoo"))));
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void changelog_empty() throws Exception {
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_empty.json");
  }

  @Test
  public void changelog_nominal() throws Exception {
    createActivity(login, ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1), Severity.MAJOR, "max", "10");

    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_nominal.json");
  }

  @Test
  public void changelog_no_param() throws Exception {
    createActivity(login, ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1), Severity.MAJOR);

    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_no_param.json");
  }

  @Test
  public void changelog_system_user() throws Exception {
    createActivity(null, ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1), Severity.MAJOR);

    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_no_login.json");
  }

  @Test
  public void changelog_with_dates() throws Exception {
    Date yesterday = DateTime.now().minusDays(1).toDate();
    Date tomorrow = DateTime.now().plusDays(1).toDate();

    createActivity(login, ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1), Severity.MAJOR, "max", "10");

    // Tests with "since"
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("since",
      DateUtils.formatDateTime(yesterday))
      .execute().assertJson(getClass(), "changelog_nominal.json");
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("since",
      DateUtils.formatDateTime(tomorrow))
      .execute().assertJson(getClass(), "changelog_empty.json");

    // Tests with "to"
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("to",
      DateUtils.formatDateTime(yesterday))
      .execute().assertJson(getClass(), "changelog_empty.json");
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY).setParam("to",
      DateUtils.formatDateTime(tomorrow))
      .execute().assertJson(getClass(), "changelog_nominal.json");

    // Test with both bounds set
    wsTester.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam("profileKey", XOO_P1_KEY)
      .setParam("since", DateUtils.formatDateTime(yesterday))
      .setParam("to", DateUtils.formatDateTime(tomorrow))
      .execute().assertJson(getClass(), "changelog_nominal.json");
  }

  @Test
  public void changelog_with_pagination() throws Exception {
    createActivity(login, ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1), Severity.MAJOR, "max", "10");
    createActivity(login, ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1), Severity.CRITICAL, "max", "20");

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

  private void createActivity(String login, Type type, ActiveRuleKey activeRuleKey, String severity, String... params) throws Exception {
    Map<String, String> details = Maps.newHashMap();
    details.put("key", activeRuleKey.toString());
    details.put("ruleKey", activeRuleKey.ruleKey().toString());
    details.put("profileKey", activeRuleKey.qProfile());
    details.put("severity", severity);
    for (int i = 0; i < params.length; i += 2) {
      details.put("param_" + params[i], params[i + 1]);
    }
    ActivityDoc doc = new ActivityDoc(Maps.<String, Object>newHashMap());
    doc.setAction(type.toString());
    doc.setCreatedAt(new Date());
    doc.setDetails(details);
    doc.setKey(Uuids.create());
    doc.setLogin(login);
    doc.setType(Activity.Type.QPROFILE.toString());

    esTester.putDocuments(ActivityIndexDefinition.INDEX, ActivityIndexDefinition.TYPE, doc);
  }
}
