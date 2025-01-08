/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.rule.registration;

import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.rule.RuleDescriptionSectionsGeneratorResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartupRuleUpdaterTest {

  private final DbClient dbClient = mock();
  private final System2 system2 = mock();
  private final UuidFactory uuidFactory = mock();
  private final RuleDescriptionSectionsGeneratorResolver sectionsGeneratorResolver = mock();

  private final StartupRuleUpdater underTest = new StartupRuleUpdater(dbClient, system2, uuidFactory, sectionsGeneratorResolver);

  @Test
  public void findChangesAndUpdateRule_whenCleanCodeTaxonomyChanged_shouldSetAnythingChangedToTrue() {
    RulesDefinition.Rule ruleDef = getDefaultRuleDef();
    when(ruleDef.cleanCodeAttribute()).thenReturn(CleanCodeAttribute.CLEAR);
    Map<SoftwareQuality, Severity> newImpacts = Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW);
    when(ruleDef.defaultImpacts()).thenReturn(newImpacts);

    RuleDto rule = getDefaultRuleDto();
    when(rule.getCleanCodeAttribute()).thenReturn(CleanCodeAttribute.COMPLETE);
    Set<ImpactDto> oldImpacts = Set.of(new ImpactDto(SoftwareQuality.RELIABILITY, Severity.LOW));
    when(rule.getDefaultImpacts()).thenReturn(oldImpacts);

    StartupRuleUpdater.RuleChange changesAndUpdateRule = underTest.findChangesAndUpdateRule(ruleDef, rule);

    assertTrue(changesAndUpdateRule.hasRuleDefinitionChanged());
    assertThat(changesAndUpdateRule.getPluginRuleUpdate().getOldCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.COMPLETE);
    assertThat(changesAndUpdateRule.getPluginRuleUpdate().getNewCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CLEAR);
    assertThat(changesAndUpdateRule.getPluginRuleUpdate().getNewImpacts()).isEqualTo(newImpacts);
    assertThat(changesAndUpdateRule.getPluginRuleUpdate().getOldImpacts()).containsEntry(SoftwareQuality.RELIABILITY, Severity.LOW);
  }

  @Test
  public void findChangesAndUpdateRule_whenImpactsChanged_thenDontIncludeUnchangedImpacts() {
    RulesDefinition.Rule ruleDef = getDefaultRuleDef();
    when(ruleDef.cleanCodeAttribute()).thenReturn(CleanCodeAttribute.CLEAR);
    Map<SoftwareQuality, Severity> newImpacts = Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW, SoftwareQuality.SECURITY, Severity.HIGH);
    when(ruleDef.defaultImpacts()).thenReturn(newImpacts);

    RuleDto rule = getDefaultRuleDto();
    when(rule.getCleanCodeAttribute()).thenReturn(CleanCodeAttribute.COMPLETE);
    Set<ImpactDto> oldImpacts = Set.of(new ImpactDto(SoftwareQuality.RELIABILITY, Severity.LOW),
      new ImpactDto(SoftwareQuality.SECURITY, Severity.HIGH));
    when(rule.getDefaultImpacts()).thenReturn(oldImpacts);

    StartupRuleUpdater.RuleChange changesAndUpdateRule = underTest.findChangesAndUpdateRule(ruleDef, rule);

    assertTrue(changesAndUpdateRule.hasRuleDefinitionChanged());
    assertThat(changesAndUpdateRule.getPluginRuleUpdate().getNewImpacts()).containsOnly(Map.entry(SoftwareQuality.MAINTAINABILITY, Severity.LOW));
    assertThat(changesAndUpdateRule.getPluginRuleUpdate().getOldImpacts()).containsOnly(Map.entry(SoftwareQuality.RELIABILITY, Severity.LOW));
  }

  @Test
  public void findChangesAndUpdateRule_whenNoCleanCodeTaxonomyChanged_thenPluginRuleChangeShouldBeNull() {
    RulesDefinition.Rule ruleDef = getDefaultRuleDef();
    when(ruleDef.cleanCodeAttribute()).thenReturn(CleanCodeAttribute.COMPLETE);
    Map<SoftwareQuality, Severity> newImpacts = Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW);
    when(ruleDef.defaultImpacts()).thenReturn(newImpacts);

    RuleDto rule = getDefaultRuleDto();
    when(rule.getCleanCodeAttribute()).thenReturn(CleanCodeAttribute.COMPLETE);
    Set<ImpactDto> oldImpacts = Set.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.LOW));
    when(rule.getDefaultImpacts()).thenReturn(oldImpacts);

    StartupRuleUpdater.RuleChange changesAndUpdateRule = underTest.findChangesAndUpdateRule(ruleDef, rule);

    assertTrue(changesAndUpdateRule.hasRuleDefinitionChanged());
    assertThat(changesAndUpdateRule.getPluginRuleUpdate()).isNull();
  }

  private RulesDefinition.Rule getDefaultRuleDef() {
    RulesDefinition.Rule ruleDef = mock();
    when(ruleDef.scope()).thenReturn(RuleScope.TEST);
    when(ruleDef.repository()).thenReturn(mock());
    when(ruleDef.type()).thenReturn(RuleType.BUG);
    return ruleDef;
  }

  private RuleDto getDefaultRuleDto() {
    RuleDto ruleDto = mock();
    when(ruleDto.getScope()).thenReturn(RuleDto.Scope.TEST);
    return ruleDto;
  }
}
