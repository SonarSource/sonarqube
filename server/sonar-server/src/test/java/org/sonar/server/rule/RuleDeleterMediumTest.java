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
package org.sonar.server.rule;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

// TODO replace ServerTester by EsTester / DbTester
public class RuleDeleterMediumTest {

  static final long PAST = 10000L;

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db = tester.get(DbClient.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  RuleIndexer ruleIndexer = tester.get(RuleIndexer.class);
  ActiveRuleIndexer activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
  RuleDeleter deleter = tester.get(RuleDeleter.class);
  DbSession dbSession = tester.get(DbClient.class).openSession(false);
  String defaultOrganizationUuid = tester.get(DefaultOrganizationProvider.class).get().getUuid();
  OrganizationDto organization = db.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid)
    .orElseThrow(() -> new IllegalStateException(String.format("Cannot find default organization '%s'", defaultOrganizationUuid)));

  @Before
  public void before() {
    tester.clearDbAndIndexes();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void delete_custom_rule() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("xoo", "T1"))
      .setLanguage("xoo")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    dao.insert(dbSession, templateRule.getDefinition());
    dbSession.commit();
    ruleIndexer.indexRuleDefinition(templateRule.getDefinition().getKey());

    // Verify in index
    assertThat(index.searchAll(new RuleQuery())).containsOnly(templateRule.getKey());

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule)
      .setLanguage("xoo")
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    dao.insert(dbSession, customRule.getDefinition());
    dbSession.commit();
    ruleIndexer.indexRuleDefinition(customRule.getDefinition().getKey());

    // Verify in index
    assertThat(index.searchAll(new RuleQuery())).containsOnly(templateRule.getKey(), customRule.getKey());

    // Create a quality profile
    QualityProfileDto profileDto = QProfileTesting.newXooP1("org-123");
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();
    activeRuleIndexer.index();

    // Activate the custom rule
    activate(new RuleActivation(customRule.getKey()).setSeverity(Severity.BLOCKER), QProfileTesting.XOO_P1_KEY);

    // Delete custom rule
    deleter.delete(customRule.getKey());

    // Verify custom rule have status REMOVED
    RuleDefinitionDto customRuleReloaded = dao.selectOrFailDefinitionByKey(dbSession, customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(customRuleReloaded.getUpdatedAt()).isNotEqualTo(PAST);

    // Verify there's no more active rule from custom rule
    assertThat(index.searchAll(new RuleQuery().setQProfileKey(profileDto.getKey()).setActivation(true))).isEmpty();

    // Verify in index
    assertThat(index.searchAll(new RuleQuery())).containsOnly(templateRule.getKey());
  }

  @Test
  public void fail_to_delete_if_not_custom() {
    // Create rule
    RuleKey ruleKey = RuleKey.of("java", "S001");
    dao.insert(dbSession, RuleTesting.newRule(ruleKey));
    dbSession.commit();

    try {
      // Delete rule
      deleter.delete(ruleKey);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Only custom rules can be deleted");
    }
  }

  private void activate(RuleActivation activation, String profileKey) {
    tester.get(RuleActivator.class).activate(dbSession, activation, profileKey);
    dbSession.commit();
    dbSession.clearCache();
  }

}
