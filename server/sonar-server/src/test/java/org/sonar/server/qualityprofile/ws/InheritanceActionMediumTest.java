/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.qualityprofile.ws;

import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.sonar.server.qualityprofile.QProfileTesting.getDefaultOrganization;

public class InheritanceActionMediumTest {

  @ClassRule
  public static final ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  WsTester wsTester;

  DbClient db;
  DbSession session;

  RuleIndexer ruleIndexer;
  ActiveRuleIndexer activeRuleIndexer;

  private OrganizationDto organization;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);

    wsTester = new WsTester(tester.get(QProfilesWs.class));
    organization = getDefaultOrganization(tester, db, session);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void inheritance_nominal() throws Exception {
    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    RuleDto rule3 = createRule("xoo", "rule3");

    /*
     * groupWide (2) <- companyWide (2) <- buWide (2, 1 overriding) <- (forProject1 (2), forProject2 (2))
     */
    QualityProfileDto groupWide = createProfile("xoo", "My Group Profile", "xoo-my-group-profile-01234");
    createActiveRule(rule1, groupWide);
    createActiveRule(rule2, groupWide);

    session.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    QualityProfileDto companyWide = createProfile("xoo", "My Company Profile", "xoo-my-company-profile-12345");
    setParent(groupWide, companyWide);

    QualityProfileDto buWide = createProfile("xoo", "My BU Profile", "xoo-my-bu-profile-23456");
    setParent(companyWide, buWide);
    overrideActiveRuleSeverity(rule1, buWide, Severity.CRITICAL);

    QualityProfileDto forProject1 = createProfile("xoo", "For Project One", "xoo-for-project-one-34567");
    setParent(buWide, forProject1);
    createActiveRule(rule3, forProject1);
    session.commit();
    activeRuleIndexer.index();

    QualityProfileDto forProject2 = createProfile("xoo", "For Project Two", "xoo-for-project-two-45678");
    setParent(buWide, forProject2);
    overrideActiveRuleSeverity(rule2, forProject2, Severity.CRITICAL);

    wsTester.newGetRequest("api/qualityprofiles", "inheritance").setParam("profileKey", buWide.getKee()).setParam("organization", organization.getKey())
      .execute().assertJson(getClass(), "inheritance-buWide.json");
  }

  @Test
  public void inheritance_no_family() throws Exception {
    // Simple profile, no parent, no child
    QualityProfileDto remi = createProfile("xoo", "Nobodys Boy", "xoo-nobody-s-boy-01234");

    wsTester.newGetRequest("api/qualityprofiles", "inheritance").setParam("profileKey", remi.getKee()).execute().assertJson(getClass(), "inheritance-simple.json");
  }

  @Test(expected = NotFoundException.class)
  public void fail_if_not_found() throws Exception {
    wsTester.newGetRequest("api/qualityprofiles", "inheritance").setParam("profileKey", "polop").execute();
  }

  private QualityProfileDto createProfile(String lang, String name, String key) {
    QualityProfileDto profile = QProfileTesting.newQProfileDto(organization, new QProfileName(lang, name), key);
    db.qualityProfileDao().insert(session, profile);
    session.commit();
    return profile;
  }

  private void setParent(QualityProfileDto profile, QualityProfileDto parent) {
    tester.get(RuleActivator.class).setParent(session, parent.getKey(), profile.getKey());
  }

  private RuleDto createRule(String lang, String id) {
    long now = new Date().getTime();
    RuleDto rule = RuleTesting.newDto(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY)
      .setUpdatedAt(now)
      .setCreatedAt(now);
    db.ruleDao().insert(session, rule);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QualityProfileDto profile) {
    long now = new Date().getTime();
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString())
      .setUpdatedAt(now)
      .setCreatedAt(now);
    db.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }

  private void overrideActiveRuleSeverity(RuleDto rule, QualityProfileDto profile, String severity) {
    tester.get(RuleActivator.class).activate(session, new RuleActivation(rule.getKey()).setSeverity(severity), profile.getKey());
    session.commit();
    activeRuleIndexer.index();
  }
}
