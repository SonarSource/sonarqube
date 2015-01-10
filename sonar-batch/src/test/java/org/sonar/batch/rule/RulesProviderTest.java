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

package org.sonar.batch.rule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.debt.DebtRemediationFunction;
import org.sonar.api.batch.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.RuleParam;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.rule.RuleDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class RulesProviderTest extends AbstractDaoTestCase {

  @Mock
  Durations durations;

  RuleDao ruleDao;

  DefaultDebtModel debtModel;

  RulesProvider provider;

  @Before
  public void setUp() throws Exception {
    debtModel = new DefaultDebtModel()
      .addCharacteristic(new DefaultDebtCharacteristic()
        .setId(100)
        .setKey("MEMORY_EFFICIENCY")
        .setName("Memory use")
        .setOrder(1))
      .addCharacteristic(new DefaultDebtCharacteristic()
        .setId(101)
        .setKey("EFFICIENCY")
        .setName("Efficiency")
        .setParentId(100));
    debtModel
      .addCharacteristic(new DefaultDebtCharacteristic()
        .setId(102)
        .setKey("COMPILER_RELATED_PORTABILITY")
        .setName("Compiler")
        .setOrder(1))
      .addCharacteristic(new DefaultDebtCharacteristic()
        .setId(103)
        .setKey("PORTABILITY")
        .setName("Portability")
        .setParentId(102));

    durations = new Durations(new Settings().setProperty("sonar.technicalDebt.hoursInDay", 8), null);
    ruleDao = new RuleDao(getMyBatis());

    provider = new RulesProvider();
  }

  @Test
  public void build_rules() throws Exception {
    setupData("shared");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    assertThat(rules.findAll()).hasSize(1);
    assertThat(rules.findByRepository("checkstyle")).hasSize(1);
    assertThat(rules.findByRepository("unknown")).isEmpty();

    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.name()).isEqualTo("Avoid Null");
    assertThat(rule.description()).isEqualTo("Should avoid NULL");
    assertThat(rule.severity()).isEqualTo(Severity.MINOR);
    assertThat(rule.internalKey()).isNull();
    assertThat(rule.params()).hasSize(1);

    RuleParam param = rule.param("myParameter");
    assertThat(param).isNotNull();
    assertThat(param.description()).isEqualTo("My Parameter");
  }

  @Test
  public void build_rules_with_default_debt_definitions() throws Exception {
    setupData("build_rules_with_default_debt_definitions");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isEqualTo("EFFICIENCY");
    assertThat(rule.debtRemediationFunction()).isEqualTo(DebtRemediationFunction.createLinearWithOffset(Duration.decode("5d", 8), Duration.decode("10h", 8)));
  }

  @Test
  public void build_rules_with_overridden_debt_definitions() throws Exception {
    setupData("build_rules_with_overridden_debt_definitions");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isEqualTo("PORTABILITY");
    assertThat(rule.debtRemediationFunction()).isEqualTo(DebtRemediationFunction.createLinear(Duration.decode("2h", 8)));
  }

  @Test
  public void build_rules_with_default_and_overridden_debt_definitions() throws Exception {
    setupData("build_rules_with_default_and_overridden_debt_definitions");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    // As both default columns and user columns on debt are set, user debt columns should be used
    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isEqualTo("PORTABILITY");
    assertThat(rule.debtRemediationFunction()).isEqualTo(DebtRemediationFunction.createLinear(Duration.decode("2h", 8)));
  }

  @Test
  public void build_rules_with_default_characteristic_and_overridden_function() throws Exception {
    setupData("build_rules_with_default_characteristic_and_overridden_function");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    // As both default columns and user columns on debt are set, user debt columns should be used
    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isEqualTo("PORTABILITY");
    assertThat(rule.debtRemediationFunction()).isEqualTo(DebtRemediationFunction.createLinear(Duration.decode("2h", 8)));
  }

  @Test
  public void build_rules_with_overridden_characteristic_and_default_function() throws Exception {
    setupData("build_rules_with_overridden_characteristic_and_default_function");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    // As both default columns and user columns on debt are set, user debt columns should be used
    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isEqualTo("PORTABILITY");
    assertThat(rule.debtRemediationFunction()).isEqualTo(DebtRemediationFunction.createLinear(Duration.decode("2h", 8)));
  }

  @Test
  public void build_rules_with_disable_characteristic() throws Exception {
    setupData("build_rules_with_disable_characteristic");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isNull();
    assertThat(rule.debtRemediationFunction()).isNull();
  }

  @Test
  public void build_rules_with_default_characteristic_and_disable_characteristic() throws Exception {
    setupData("build_rules_with_default_characteristic_and_disable_characteristic");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.debtSubCharacteristic()).isNull();
    assertThat(rule.debtRemediationFunction()).isNull();
  }

  @Test
  public void fail_if_characteristic_not_found() throws Exception {
    setupData("fail_if_characteristic_not_found");

    try {
      provider.provide(ruleDao, debtModel, durations);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Sub characteristic id '999' on rule 'checkstyle:AvoidNull' has not been found");
    }
  }

  @Test
  public void fail_if_no_function() throws Exception {
    setupData("fail_if_no_function");

    try {
      provider.provide(ruleDao, debtModel, durations);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Remediation function should not be null on rule 'checkstyle:AvoidNull'");
    }
  }
}
