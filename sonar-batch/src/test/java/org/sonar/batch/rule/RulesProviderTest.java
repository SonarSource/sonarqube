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
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.RuleParam;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RemediationFunction;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtModel;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

@RunWith(MockitoJUnitRunner.class)
public class RulesProviderTest extends AbstractDaoTestCase {

  @Mock
  Durations durations;

  RuleDao ruleDao;

  DefaultTechnicalDebtModel debtModel;

  RulesProvider provider;

  @Before
  public void setUp() throws Exception {
    debtModel = new DefaultTechnicalDebtModel();
    debtModel.addRootCharacteristic(new DefaultCharacteristic()
      .setId(101)
      .setKey("EFFICIENCY")
      .setName("Efficiency")
      .setParent(new DefaultCharacteristic()
        .setId(100)
        .setKey("MEMORY_EFFICIENCY")
        .setName("Memory use")));
    debtModel.addRootCharacteristic(new DefaultCharacteristic()
      .setId(103)
      .setKey("PORTABILITY")
      .setName("Portability")
      .setParent(new DefaultCharacteristic()
        .setId(102)
        .setKey("COMPILER_RELATED_PORTABILITY")
        .setName("Compiler")));

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
    assertThat(rule.metadata()).isNull();
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
    assertThat(rule.characteristic()).isEqualTo("EFFICIENCY");
    assertThat(rule.function()).isEqualTo(RemediationFunction.LINEAR_OFFSET);
    assertThat(rule.factor()).isEqualTo(Duration.decode("5d", 8));
    assertThat(rule.offset()).isEqualTo(Duration.decode("10h", 8));
  }

  @Test
  public void build_rules_with_user_debt_definitions() throws Exception {
    setupData("build_rules_with_user_debt_definitions");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.characteristic()).isEqualTo("PORTABILITY");
    assertThat(rule.function()).isEqualTo(RemediationFunction.LINEAR);
    assertThat(rule.factor()).isEqualTo(Duration.decode("2h", 8));
    assertThat(rule.offset()).isNull();
  }

  @Test
  public void build_rules_with_default_and_user_debt_definitions() throws Exception {
    setupData("build_rules_with_default_and_user_debt_definitions");

    Rules rules = provider.provide(ruleDao, debtModel, durations);

    // As both default columns and user columns on debt are set, user debt columns should be used
    Rule rule = rules.find(RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(rule.characteristic()).isEqualTo("PORTABILITY");
    assertThat(rule.function()).isEqualTo(RemediationFunction.LINEAR);
    assertThat(rule.factor()).isEqualTo(Duration.decode("2h", 8));
    assertThat(rule.offset()).isNull();
  }

  @Test
  public void fail_if_characteristic_not_found() throws Exception {
    setupData("fail_if_characteristic_not_found");

    try {
      provider.provide(ruleDao, debtModel, durations);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Characteristic id '999' on rule 'checkstyle:AvoidNull' has not been found");
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
