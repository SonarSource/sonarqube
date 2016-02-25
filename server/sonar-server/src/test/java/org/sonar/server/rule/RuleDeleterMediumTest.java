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
package org.sonar.server.rule;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleDoc;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex2;
import org.sonar.server.rule.index.RuleIndex2;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

// TODO replace ServerTester by EsTester / DbTester
public class RuleDeleterMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db = tester.get(DbClient.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex2 index = tester.get(RuleIndex2.class);
  RuleDeleter deleter = tester.get(RuleDeleter.class);
  DbSession dbSession = tester.get(DbClient.class).openSession(false);

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
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("xoo", "T1")).setLanguage("xoo");
    dao.insert(dbSession, templateRule);

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule).setLanguage("xoo");
    dao.insert(dbSession, customRule);

    // Create a quality profile
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();
    dbSession.clearCache();

    // Activate the custom rule
    activate(new RuleActivation(customRule.getKey()).setSeverity(Severity.BLOCKER), QProfileTesting.XOO_P1_KEY);

    // Delete custom rule
    deleter.delete(customRule.getKey());

    // Verify custom rule have status REMOVED
    RuleDto customRuleReloaded = dao.selectOrFailByKey(dbSession, customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.REMOVED);

    // Verify there's no more active rule from custom rule
    List<ActiveRuleDoc> activeRules = Lists.newArrayList(tester.get(ActiveRuleIndex2.class).findByProfile(profileDto.getKey()));
    assertThat(activeRules).isEmpty();

    // Verify in index
    assertThat(index.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(templateRule.getKey());
  }

  @Test
  public void delete_manual_rule() {
    // Create manual rule
    RuleDto manualRule = RuleTesting.newManualRule("Manual_Rule");
    dao.insert(dbSession, manualRule);

    dbSession.commit();

    // Delete manual rule
    deleter.delete(manualRule.getKey());

    // Verify custom rule have status REMOVED
    RuleDto result = dao.selectOrFailByKey(dbSession, manualRule.getKey());
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(RuleStatus.REMOVED);

    // Verify in index
    assertThat(index.search(new RuleQuery(), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void fail_to_delete_if_not_custom_or_not_manual() {
    // Create rule
    RuleKey ruleKey = RuleKey.of("java", "S001");
    dao.insert(dbSession, RuleTesting.newDto(ruleKey));
    dbSession.commit();

    try {
      // Delete rule
      deleter.delete(ruleKey);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Only custom rules and manual rules can be deleted");
    }
  }

  private void activate(RuleActivation activation, String profileKey) {
    tester.get(RuleActivator.class).activate(dbSession, activation, profileKey);
    dbSession.commit();
    dbSession.clearCache();
  }

}
