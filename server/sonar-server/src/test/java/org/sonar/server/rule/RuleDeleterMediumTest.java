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

package org.sonar.server.rule;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleDeleterMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db = tester.get(DbClient.class);
  RuleDao dao = tester.get(RuleDao.class);
  BaseIndex<Rule, RuleDto, RuleKey> index = tester.get(RuleIndex.class);
  RuleDeleter deleter = tester.get(RuleDeleter.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void delete_custom_rule() throws Exception {
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
    Rule customRuleReloaded = index.getByKey(customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.status()).isEqualTo(RuleStatus.REMOVED);

    // Verify there's no more active rule from custom rule
    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(ActiveRuleIndex.class).findByProfile(profileDto.getKey()));
    assertThat(activeRules).isEmpty();
  }

  @Test
  public void delete_manual_rule() throws Exception {
    // Create manual rule
    RuleDto manualRule = RuleTesting.newManualRule("Manual_Rule");
    dao.insert(dbSession, manualRule);

    dbSession.commit();

    // Delete manual rule
    deleter.delete(manualRule.getKey());

    // Verify custom rule have status REMOVED
    Rule result = index.getByKey(manualRule.getKey());
    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo(RuleStatus.REMOVED);
  }

  @Test
  public void fail_to_delete_if_not_custom_or_not_manual() throws Exception {
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
