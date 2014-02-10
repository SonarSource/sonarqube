/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.rule;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.QualityProfileDao;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActiveRulesProviderTest extends AbstractDaoTestCase {

  ModuleQProfiles qProfiles = mock(ModuleQProfiles.class);
  RuleFinder ruleFinder = mock(RuleFinder.class);

  @Before
  public void init_rules() {
    Rule squidRule = new Rule().setRepositoryKey("squid").setKey("S0001");
    squidRule.createParameter("min").setDefaultValue("12");
    when(ruleFinder.findById(10)).thenReturn(squidRule);
    when(ruleFinder.findById(100)).thenReturn(new Rule().setRepositoryKey("phpunit").setKey("P1"));
  }

  @Test
  public void build_active_rules() throws Exception {
    setupData("shared");
    QualityProfileDao profileDao = new QualityProfileDao(getMyBatis());
    when(qProfiles.findAll()).thenReturn(Arrays.asList(
      // 1 rule is enabled on java with severity INFO
      new ModuleQProfiles.QProfile(profileDao.selectById(2)),
      // 1 rule is enabled on php with severity BLOCKER
      new ModuleQProfiles.QProfile(profileDao.selectById(3))
    ));

    ActiveRulesProvider provider = new ActiveRulesProvider();
    ActiveRuleDao activeRuleDao = new ActiveRuleDao(getMyBatis());
    ActiveRules activeRules = provider.provide(qProfiles, activeRuleDao, ruleFinder);

    assertThat(activeRules.findAll()).hasSize(2);
    assertThat(activeRules.findByRepository("squid")).hasSize(1);
    assertThat(activeRules.findByRepository("phpunit")).hasSize(1);
    assertThat(activeRules.findByRepository("unknown")).isEmpty();
    ActiveRule squidRule = activeRules.find(RuleKey.of("squid", "S0001"));
    assertThat(squidRule.severity()).isEqualTo(Severity.INFO);
    assertThat(squidRule.internalKey()).isNull();
    // "max" and "format" parameters are set in db, "min" is not set but has a default value
    assertThat(squidRule.params()).hasSize(3);
    assertThat(squidRule.param("min")).isEqualTo("12");
    assertThat(squidRule.param("max")).isEqualTo("20");
    assertThat(squidRule.param("format")).isEqualTo("html");

    ActiveRule phpRule = activeRules.find(RuleKey.of("phpunit", "P1"));
    assertThat(phpRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(phpRule.params()).isEmpty();
  }
}
