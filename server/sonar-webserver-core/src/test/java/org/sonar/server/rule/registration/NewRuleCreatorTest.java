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

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.core.rule.RuleTypeMapper;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.rule.RuleDescriptionSectionsGeneratorResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.Severity.MAJOR;

public class NewRuleCreatorTest {

  private final RuleDescriptionSectionsGeneratorResolver ruleDescriptionSectionsGeneratorResolver = mock();
  private final UuidFactory uuidFactory = mock();
  private final System2 system2 = mock();
  private final RulesRegistrationContext context = mock();

  private final NewRuleCreator underTest = new NewRuleCreator(ruleDescriptionSectionsGeneratorResolver, uuidFactory, system2);


  @Test
  public void from_whenRuleDefinitionDoesntHaveCleanCodeAttribute_shouldAlwaysSetCleanCodeAttribute() {
    RulesDefinition.Rule ruleDef = getDefaultRule();

    RuleDto newRuleDto = underTest.createNewRule(context, ruleDef);

    assertThat(newRuleDto.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL);
  }

  @Test
  public void from_whenRuleDefinitionDoesHaveCleanCodeAttribute_shouldReturnThisAttribute() {
    RulesDefinition.Rule ruleDef = getDefaultRule(CleanCodeAttribute.TESTED, RuleType.CODE_SMELL);

    RuleDto newRuleDto = underTest.createNewRule(context, ruleDef);

    assertThat(newRuleDto.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.TESTED);
  }

  @Test
  public void createNewRule_whenRuleDefinitionDoesHaveCleanCodeAttributeAndIsSecurityHotspot_shouldReturnNull() {
    RulesDefinition.Rule ruleDef = getDefaultRule(CleanCodeAttribute.TESTED, RuleType.SECURITY_HOTSPOT);

    RuleDto newRuleDto = underTest.createNewRule(context, ruleDef);
    assertThat(newRuleDto.getCleanCodeAttribute()).isNull();
    assertThat(newRuleDto.getDefaultImpacts()).isEmpty();
  }

  @Test
  public void createNewRule_whenImpactDefined_shouldReturnThisImpact() {
    RulesDefinition.Rule ruleDef = getDefaultRule();
    Map<SoftwareQuality, Severity> singleImpact = new HashMap<>();
    singleImpact.put(SoftwareQuality.RELIABILITY, Severity.LOW);
    when(ruleDef.defaultImpacts()).thenReturn(singleImpact);

    RuleDto newRuleDto = underTest.createNewRule(context, ruleDef);

    assertThat(newRuleDto.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsOnly(tuple(SoftwareQuality.RELIABILITY, Severity.LOW));
  }

  private static RulesDefinition.Rule getDefaultRule(@Nullable CleanCodeAttribute attribute, RuleType ruleType) {
    RulesDefinition.Rule ruleDef = mock(RulesDefinition.Rule.class);
    RulesDefinition.Repository repository = mock(RulesDefinition.Repository.class);
    when(ruleDef.repository()).thenReturn(repository);

    when(ruleDef.key()).thenReturn("key");
    when(repository.key()).thenReturn("repoKey");
    when(ruleDef.type()).thenReturn(RuleTypeMapper.toApiRuleType(ruleType));
    when(ruleDef.scope()).thenReturn(RuleScope.TEST);
    when(ruleDef.cleanCodeAttribute()).thenReturn(attribute);
    when(ruleDef.severity()).thenReturn(MAJOR);
    when(ruleDef.defaultImpacts()).thenReturn(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW));
    return ruleDef;
  }

  private static RulesDefinition.Rule getDefaultRule() {
    return getDefaultRule(null, RuleType.CODE_SMELL);
  }
}
