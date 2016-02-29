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
package org.sonar.db.qualityprofile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO add missing tests

public class ActiveRuleDaoTest {

  static final long NOW = 10000000L;

  static final QualityProfileDto QPROFILE_1 = QualityProfileDto.createFor("qp1").setName("QProile1");
  static final QualityProfileDto QPROFILE_2 = QualityProfileDto.createFor("qp2").setName("QProile2");

  static final RuleDto RULE_1 = RuleTesting.newDto(RuleTesting.XOO_X1);
  static final RuleDto RULE_2 = RuleTesting.newDto(RuleTesting.XOO_X2);

  System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  DbClient dbClient = dbTester.getDbClient();
  DbSession dbSession = dbTester.getSession();

  ActiveRuleDao underTest = dbTester.getDbClient().activeRuleDao();

  @Before
  public void createDao() {
    when(system.now()).thenReturn(NOW);

    dbClient.qualityProfileDao().insert(dbTester.getSession(), QPROFILE_1);
    dbClient.qualityProfileDao().insert(dbTester.getSession(), QPROFILE_2);
    dbClient.ruleDao().insert(dbTester.getSession(), RULE_1);
    dbClient.ruleDao().insert(dbTester.getSession(), RULE_2);
    dbSession.commit();
  }

  @Test
  public void select_by_keys() throws Exception {
    ActiveRuleDto activeRule1 = ActiveRuleDto.createFor(QPROFILE_1, RULE_1).setSeverity(Severity.BLOCKER);
    ActiveRuleDto activeRule2 = ActiveRuleDto.createFor(QPROFILE_1, RULE_2).setSeverity(Severity.BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insert(dbTester.getSession(), activeRule2);
    dbSession.commit();

    assertThat(underTest.selectByKeys(dbSession, asList(activeRule1.getKey(), activeRule2.getKey()))).hasSize(2);
    assertThat(underTest.selectByKeys(dbSession, asList(activeRule1.getKey()))).hasSize(1);
    assertThat(underTest.selectByKeys(dbSession, asList(ActiveRuleKey.of(QPROFILE_2.getKey(), RULE_1.getKey())))).isEmpty();
  }
}
