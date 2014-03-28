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

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.check.Cardinality;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class RuleTest {

  @Test
  public void test_getters_and_setters() throws Exception {
    Rule rule = new Rule.Builder()
      .setId(1)
      .setKey("AvoidCycle")
      .setRepositoryKey("squid")
      .setName("Avoid Cycle")
      .setDescription("Avoid cycle between packages")
      .setLanguage("java")
      .setSeverity(Severity.BLOCKER)
      .setStatus("BETA")
      .setCardinality(Cardinality.SINGLE.name())
      .setTemplateId(2)
      .setRuleNote(new RuleNote("Some note", "john", new Date(), new Date()))
      .setAdminTags(newArrayList("AdminTag"))
      .setSystemTags(newArrayList("SysTag"))
      .setParams(newArrayList(new RuleParam("key", "desc", "default", RuleParamType.STRING)))
      .setDebtCharacteristicKey("REUSABILITY")
      .setDebtCharacteristicName("Reusability")
      .setDebtSubCharacteristicKey("MODULARITY")
      .setDebtSubCharacteristicName("Modularity")
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "1h", "15min"))
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date())
      .build();

    assertThat(rule.id()).isEqualTo(1);
    assertThat(rule.ruleKey()).isEqualTo(RuleKey.of("squid", "AvoidCycle"));
    assertThat(rule.name()).isEqualTo("Avoid Cycle");
    assertThat(rule.description()).isEqualTo("Avoid cycle between packages");
    assertThat(rule.language()).isEqualTo("java");
    assertThat(rule.severity()).isEqualTo("BLOCKER");
    assertThat(rule.status()).isEqualTo("BETA");
    assertThat(rule.cardinality()).isEqualTo("SINGLE");
    assertThat(rule.templateId()).isEqualTo(2);
    assertThat(rule.ruleNote()).isNotNull();
    assertThat(rule.adminTags()).hasSize(1);
    assertThat(rule.systemTags()).hasSize(1);
    assertThat(rule.params()).hasSize(1);
    assertThat(rule.debtCharacteristicKey()).isEqualTo("REUSABILITY");
    assertThat(rule.debtCharacteristicName()).isEqualTo("Reusability");
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo("MODULARITY");
    assertThat(rule.debtSubCharacteristicName()).isEqualTo("Modularity");
    assertThat(rule.debtRemediationFunction()).isEqualTo(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET, "1h", "15min"));
    assertThat(rule.createdAt()).isNotNull();
    assertThat(rule.updatedAt()).isNotNull();
  }

  @Test
  public void is_template() throws Exception {
    assertThat(new Rule.Builder()
      .setId(1)
      .setKey("AvoidCycle")
      .setRepositoryKey("squid")
      .setName("Avoid Cycle")
      .setDescription("Avoid cycle between packages")
      .setLanguage("java")
      .setSeverity(Severity.BLOCKER)
      .setStatus("BETA")
      .setCardinality(Cardinality.MULTIPLE.name())
      .setCreatedAt(new Date())
      .build().isTemplate()).isTrue();

    assertThat(new Rule.Builder()
      .setId(1)
      .setKey("AvoidCycle")
      .setRepositoryKey("squid")
      .setName("Avoid Cycle")
      .setDescription("Avoid cycle between packages")
      .setLanguage("java")
      .setSeverity(Severity.BLOCKER)
      .setStatus("BETA")
      .setCardinality(Cardinality.SINGLE.name())
      .setCreatedAt(new Date())
      .build().isTemplate()).isFalse();
  }

  @Test
  public void is_copy_of_template() throws Exception {
    assertThat(new Rule.Builder()
      .setId(1)
      .setKey("AvoidCycle")
      .setRepositoryKey("squid")
      .setName("Avoid Cycle")
      .setDescription("Avoid cycle between packages")
      .setLanguage("java")
      .setSeverity(Severity.BLOCKER)
      .setStatus("BETA")
      .setCardinality(Cardinality.MULTIPLE.name())
      .setTemplateId(null)
      .setCreatedAt(new Date())
      .build().isEditable()).isFalse();

    assertThat(new Rule.Builder()
      .setId(1)
      .setKey("AvoidCycle")
      .setRepositoryKey("squid")
      .setName("Avoid Cycle")
      .setDescription("Avoid cycle between packages")
      .setLanguage("java")
      .setSeverity(Severity.BLOCKER)
      .setStatus("BETA")
      .setCardinality(Cardinality.SINGLE.name())
      .setTemplateId(2)
      .setCreatedAt(new Date())
      .build().isEditable()).isTrue();
  }
}


