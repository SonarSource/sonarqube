/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.xoo.rule;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.impl.server.RulesDefinitionContext;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.xoo.rule.hotspot.HotspotWithContextsSensor;
import org.sonar.xoo.rule.hotspot.HotspotWithoutContextSensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;

public class XooRulesDefinitionTest {

  private XooRulesDefinition def = new XooRulesDefinition(SonarRuntimeImpl.forSonarQube(Version.create(9, 3), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY));

  private RulesDefinition.Context context = new RulesDefinitionContext();

  @Before
  public void setUp() {
    def.define(context);
  }

  @Test
  public void define_xoo_rules() {
    RulesDefinition.Repository repo = getRepository();

    RulesDefinition.Rule rule = repo.rule(OneIssuePerLineSensor.RULE_KEY);
    assertThat(rule.name()).isNotEmpty();
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(rule.debtRemediationFunction().gapMultiplier()).isEqualTo("1min");
    assertThat(rule.debtRemediationFunction().baseEffort()).isNull();
    assertThat(rule.gapDescription()).isNotEmpty();
    assertThat(rule.ruleDescriptionSections()).isNotEmpty();
    assertThat(rule.ruleDescriptionSections().stream().anyMatch(rds -> rds.getContext().isPresent())).isTrue();
  }

  @Test
  public void define_xoo_hotspot_rule() {
    RulesDefinition.Repository repo = getRepository();

    RulesDefinition.Rule rule = repo.rule(HotspotWithoutContextSensor.RULE_KEY);
    assertThat(rule.name()).isNotEmpty();
    assertThat(rule.securityStandards())
      .isNotEmpty()
      .containsExactlyInAnyOrder("cwe:1", "cwe:89", "cwe:123", "cwe:863", "owaspTop10:a1", "owaspTop10:a3",
        "owaspTop10-2021:a3", "owaspTop10-2021:a2");
  }

  @Test
  public void define_xoo_hotspot_rule_with_contexts() {
    RulesDefinition.Repository repo = getRepository();

    RulesDefinition.Rule rule = repo.rule(HotspotWithContextsSensor.RULE_KEY);
    assertThat(rule.name()).isNotEmpty();
    assertThat(rule.securityStandards()).isEmpty();
    assertThat(rule.ruleDescriptionSections()).isNotEmpty();
    assertThat(rule.ruleDescriptionSections().stream()
      .filter(rds -> rds.getKey().equals(HOW_TO_FIX_SECTION_KEY)))
      .allMatch(rds -> rds.getContext().isPresent());
  }

  @Test
  public void define_xoo_vulnerability_rule() {
    RulesDefinition.Repository repo = getRepository();

    RulesDefinition.Rule rule = repo.rule(OneVulnerabilityIssuePerProjectSensor.RULE_KEY);
    assertThat(rule.name()).isNotEmpty();
    assertThat(rule.securityStandards())
      .isNotEmpty()
      .containsExactlyInAnyOrder("cwe:250", "cwe:546", "cwe:564", "cwe:943", "owaspTop10-2021:a6", "owaspTop10-2021:a9",
        "owaspTop10:a10", "owaspTop10:a9");
  }

  @Test
  public void define_xooExternal_rules() {
    RulesDefinition.Repository repo = context.repository("external_XooEngine");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("XooEngine");
    assertThat(repo.language()).isEqualTo("xoo");
    assertThat(repo.rules()).hasSize(2);
  }

  @Test
  public void define_xoo2_rules() {
    RulesDefinition.Repository repo = context.repository("xoo2");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("Xoo2");
    assertThat(repo.language()).isEqualTo("xoo2");
    assertThat(repo.rules()).hasSize(2);
  }

  private RulesDefinition.Repository getRepository() {
    RulesDefinition.Repository repo = context.repository("xoo");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("Xoo");
    assertThat(repo.language()).isEqualTo("xoo");
    assertThat(repo.rules()).hasSize(28);
    return repo;
  }
}
