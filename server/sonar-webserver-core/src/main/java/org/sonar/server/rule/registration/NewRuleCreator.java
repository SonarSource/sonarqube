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
package org.sonar.server.rule.registration;

import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.rule.RuleDescriptionSectionsGeneratorResolver;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class NewRuleCreator {

  private final RuleDescriptionSectionsGeneratorResolver ruleDescriptionSectionsGeneratorResolver;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public NewRuleCreator(RuleDescriptionSectionsGeneratorResolver ruleDescriptionSectionsGeneratorResolver, UuidFactory uuidFactory, System2 system2) {
    this.ruleDescriptionSectionsGeneratorResolver = ruleDescriptionSectionsGeneratorResolver;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  RuleDto createNewRule(RulesRegistrationContext context, RulesDefinition.Rule ruleDef) {
    RuleDto newRule = createRuleWithSimpleFields(ruleDef, uuidFactory.create(), system2.now());
    ruleDescriptionSectionsGeneratorResolver.generateFor(ruleDef).forEach(newRule::addRuleDescriptionSectionDto);
    context.created(newRule);
    return newRule;
  }

  RuleDto createRuleWithSimpleFields(RulesDefinition.Rule ruleDef, String uuid, long now) {
    RuleDto ruleDto = new RuleDto()
      .setUuid(uuid)
      .setRuleKey(RuleKey.of(ruleDef.repository().key(), ruleDef.key()))
      .setPluginKey(ruleDef.pluginKey())
      .setIsTemplate(ruleDef.template())
      .setConfigKey(ruleDef.internalKey())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setSeverity(ruleDef.severity())
      .setStatus(ruleDef.status())
      .setGapDescription(ruleDef.gapDescription())
      .setSystemTags(ruleDef.tags())
      .setSecurityStandards(ruleDef.securityStandards())
      .setType(RuleType.valueOf(ruleDef.type().name()))
      .setScope(RuleDto.Scope.valueOf(ruleDef.scope().name()))
      .setIsExternal(ruleDef.repository().isExternal())
      .setIsAdHoc(false)
      .setCreatedAt(now)
      .setUpdatedAt(now)
      .setEducationPrinciples(ruleDef.educationPrincipleKeys());

    if (!RuleType.SECURITY_HOTSPOT.equals(ruleDef.type())) {
      CleanCodeAttribute cleanCodeAttribute = ruleDef.cleanCodeAttribute();
      ruleDto.setCleanCodeAttribute(cleanCodeAttribute != null ? cleanCodeAttribute : CleanCodeAttribute.defaultCleanCodeAttribute());
      ruleDto.replaceAllDefaultImpacts(ruleDef.defaultImpacts().entrySet()
        .stream()
        .map(e -> new ImpactDto().setSoftwareQuality(e.getKey()).setSeverity(e.getValue()))
        .collect(Collectors.toSet()));
    }

    if (isNotEmpty(ruleDef.htmlDescription())) {
      ruleDto.setDescriptionFormat(RuleDto.Format.HTML);
    } else if (isNotEmpty(ruleDef.markdownDescription())) {
      ruleDto.setDescriptionFormat(RuleDto.Format.MARKDOWN);
    }

    DebtRemediationFunction debtRemediationFunction = ruleDef.debtRemediationFunction();
    if (debtRemediationFunction != null) {
      ruleDto.setDefRemediationFunction(debtRemediationFunction.type().name());
      ruleDto.setDefRemediationGapMultiplier(debtRemediationFunction.gapMultiplier());
      ruleDto.setDefRemediationBaseEffort(debtRemediationFunction.baseEffort());
      ruleDto.setGapDescription(ruleDef.gapDescription());
    }

    return ruleDto;
  }
}
