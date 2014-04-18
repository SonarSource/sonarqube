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

package org.sonar.server.tester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.rule.RuleQuery;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.rule.Rules;
import org.sonar.server.user.MockUserSession;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;

public class MediumTest {

  ServerTester serverTester = new ServerTester();

  @Before
  public void before() throws Exception {
    serverTester.start();
  }

  @After
  public void after() throws Exception {
    serverTester.stop();
  }

  @Test
  public void find_characteristics() throws Exception {
    DebtModelService debtModelService = serverTester.get(DebtModelService.class);

    assertThat(debtModelService.allCharacteristics()).hasSize(39);
  }

  @Test
  public void create_characteristic() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    DebtModelService debtModelService = serverTester.get(DebtModelService.class);
    debtModelService.create("NEW ONE", null);

    assertThat(debtModelService.allCharacteristics()).hasSize(40);
  }

  @Test
  public void create_another_characteristic() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    DebtModelService debtModelService = serverTester.get(DebtModelService.class);
    debtModelService.create("NEW TWO", null);

    assertThat(debtModelService.allCharacteristics()).hasSize(40);
  }

  @Test
  public void create_rule() throws Exception {
    Rules rules = serverTester.get(Rules.class);
    assertThat(rules.find(RuleQuery.builder().build()).results()).isEmpty();

    RuleDto ruleDto = new RuleDto().setRepositoryKey("repo").setRuleKey("key").setSeverity("MAJOR");
    serverTester.get(RuleDao.class).insert(ruleDto);
    serverTester.get(RuleRegistry.class).reindex(ruleDto);

    assertThat(rules.find(RuleQuery.builder().build()).results()).hasSize(1);
  }

  @Test
  public void create_another_rule() throws Exception {
    Rules rules = serverTester.get(Rules.class);
    assertThat(rules.find(RuleQuery.builder().build()).results()).isEmpty();

    RuleDto ruleDto = new RuleDto().setRepositoryKey("repo2").setRuleKey("key2").setSeverity("MAJOR");
    serverTester.get(RuleDao.class).insert(ruleDto);
    serverTester.get(RuleRegistry.class).reindex(ruleDto);

    assertThat(rules.find(RuleQuery.builder().build()).results()).hasSize(1);
  }

  @Test
  public void i18n_message() throws Exception {
    I18n i18n = serverTester.get(I18n.class);
    assertThat(i18n.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");

    GwtI18n gwtI18n= serverTester.get(GwtI18n.class);
    assertThat(gwtI18n.getJsDictionnary(Locale.ENGLISH)).contains("\"design.help\": \"Help\"");
  }
}
