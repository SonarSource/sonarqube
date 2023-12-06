/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.rule.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.common.rule.service.RuleInformation;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.v2.api.rule.enums.CleanCodeAttributeCategoryRestEnum;
import org.sonar.server.v2.api.rule.enums.CleanCodeAttributeRestEnum;
import org.sonar.server.v2.api.rule.enums.ImpactSeverityRestEnum;
import org.sonar.server.v2.api.rule.enums.RuleStatusRestEnum;
import org.sonar.server.v2.api.rule.enums.RuleTypeRestEnum;
import org.sonar.server.v2.api.rule.enums.SoftwareQualityRestEnum;
import org.sonar.server.v2.api.rule.resource.Impact;
import org.sonar.server.v2.api.rule.resource.Parameter;
import org.sonar.server.v2.api.rule.response.RuleDescriptionSectionContextRestResponse;
import org.sonar.server.v2.api.rule.response.RuleDescriptionSectionRestResponse;
import org.sonar.server.v2.api.rule.response.RuleRestResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;

public class RuleRestResponseGenerator {

  private final Languages languages;
  private final MacroInterpreter macroInterpreter;
  private final RuleDescriptionFormatter ruleDescriptionFormatter;

  public RuleRestResponseGenerator(Languages languages, MacroInterpreter macroInterpreter, RuleDescriptionFormatter ruleDescriptionFormatter) {

    this.languages = languages;
    this.macroInterpreter = macroInterpreter;
    this.ruleDescriptionFormatter = ruleDescriptionFormatter;
  }

  public RuleRestResponse toRuleRestResponse(RuleInformation ruleInformation) {

    RuleRestResponse.Builder builder = RuleRestResponse.Builder.builder();
    RuleDto ruleDto = ruleInformation.ruleDto();
    builder
      .setId(ruleDto.getUuid())
      .setKey(ruleDto.getKey().toString())
      .setRepositoryKey(ruleDto.getRepositoryKey())
      .setName(ruleDto.getName())
      .setSeverity(ruleDto.getSeverityString())
      .setType(RuleTypeRestEnum.from(RuleType.valueOf(ruleDto.getType())))
      .setImpacts(toImpactRestResponse(ruleDto.getDefaultImpacts()))
      .setCleanCodeAttribute(CleanCodeAttributeRestEnum.from(ruleDto.getCleanCodeAttribute()))
      .setCleanCodeAttributeCategory(ofNullable(ruleDto.getCleanCodeAttribute())
        .map(CleanCodeAttribute::getAttributeCategory)
        .map(CleanCodeAttributeCategoryRestEnum::from)
        .orElse(null))
      .setStatus(RuleStatusRestEnum.from(ruleDto.getStatus()))
      .setExternal(ruleDto.isExternal())
      .setCreatedAt(toDateTime(ruleDto.getCreatedAt()))
      .setGapDescription(ruleDto.getGapDescription())
      .setHtmlNote(ofNullable(ruleDto.getNoteData()).map(n -> macroInterpreter.interpret(Markdown.convertToHtml(n))).orElse(null))
      .setMarkdownNote(ruleDto.getNoteData())
      .setEducationPrinciples(new ArrayList<>(ruleDto.getEducationPrinciples()))
      .setTemplate(ruleDto.isTemplate())
      .setTemplateId(ruleDto.getTemplateUuid())
      .setTags(new ArrayList<>(ruleDto.getTags()))
      .setSystemTags(new ArrayList<>(ruleDto.getSystemTags()))
      .setLanguageKey(ruleDto.getLanguage())
      .setLanguageName(getLanguageName(ruleDto.getLanguage()))
      .setParameters(toRuleParameterResponse(ruleInformation.params()));

    setDescriptionFields(builder, ruleDto);
    setRemediationFunctionFields(builder, ruleDto);

    if (ruleDto.isAdHoc()) {
      ofNullable(ruleDto.getAdHocName()).ifPresent(builder::setName);
      ofNullable(ruleDto.getAdHocDescription())
        .map(this::toDescriptionSectionResponse)
        .ifPresent(section -> builder.setDescriptionSections(List.of(section)));
      ofNullable(ruleDto.getAdHocSeverity()).ifPresent(builder::setSeverity);
      ofNullable(ruleDto.getAdHocType()).ifPresent(type -> builder.setType(RuleTypeRestEnum.from(RuleType.valueOf(type))));
    }
    return builder.build();
  }

  private static void setRemediationFunctionFields(RuleRestResponse.Builder builder, RuleDto ruleDto) {
    ofNullable(debtRemediationFunction(ruleDto))
      .ifPresent(function -> {
        builder.setRemediationFunctionBaseEffort(function.baseEffort());
        builder.setRemediationFunctionGapMultiplier(function.gapMultiplier());
        ofNullable(function.type()).map(Enum::name).ifPresent(builder::setRemediationFunctionType);
      });
  }

  private static List<Parameter> toRuleParameterResponse(List<RuleParamDto> ruleParamDtos) {
    return ruleParamDtos.stream()
      .map(p -> new Parameter(p.getName(), Markdown.convertToHtml(p.getDescription()), p.getDefaultValue(), p.getType()))
      .toList();
  }

  @CheckForNull
  private String getLanguageName(@Nullable String languageKey) {
    if (languageKey == null) {
      return null;
    }
    Language language = languages.get(languageKey);
    return language == null ? languageKey : language.getName();
  }

  private void setDescriptionFields(RuleRestResponse.Builder builder, RuleDto ruleDto) {
    builder.setDescriptionSections(ruleDto.getRuleDescriptionSectionDtos().stream()
      .map(sectionDto -> toDescriptionSectionResponse(ruleDto, sectionDto))
      .toList());

    String htmlDescription = ruleDescriptionFormatter.getDescriptionAsHtml(ruleDto);
    if (MARKDOWN.equals(ruleDto.getDescriptionFormat())) {
      Optional.ofNullable(ruleDto.getDefaultRuleDescriptionSection())
        .map(RuleDescriptionSectionDto::getContent)
        .ifPresent(builder::setMarkdownDescription);
    } else if (htmlDescription != null) {
      builder.setMarkdownDescription(macroInterpreter.interpret(htmlDescription));
    }
  }

  private RuleDescriptionSectionRestResponse toDescriptionSectionResponse(RuleDto ruleDto, RuleDescriptionSectionDto section) {
    String htmlContent = ruleDescriptionFormatter.toHtml(ruleDto.getDescriptionFormat(), section);
    String interpretedHtmlContent = macroInterpreter.interpret(htmlContent);
    return new RuleDescriptionSectionRestResponse(section.getKey(), interpretedHtmlContent,
      ofNullable(section.getContext())
        .map(c -> new RuleDescriptionSectionContextRestResponse(c.getKey(), c.getDisplayName()))
        .orElse(null));
  }

  private RuleDescriptionSectionRestResponse toDescriptionSectionResponse(String description) {
    return new RuleDescriptionSectionRestResponse(RuleDescriptionSectionDto.DEFAULT_KEY, macroInterpreter.interpret(description), null);
  }

  private static List<Impact> toImpactRestResponse(Set<ImpactDto> defaultImpacts) {
    return defaultImpacts.stream()
      .map(i -> new Impact(SoftwareQualityRestEnum.from(i.getSoftwareQuality()), ImpactSeverityRestEnum.from(i.getSeverity())))
      .toList();
  }

  @CheckForNull
  private static DebtRemediationFunction defaultDebtRemediationFunction(final RuleDto ruleDto) {
    final String function = ruleDto.getDefRemediationFunction();
    if (function == null || function.isEmpty()) {
      return null;
    } else {
      return new DefaultDebtRemediationFunction(
        DebtRemediationFunction.Type.valueOf(function.toUpperCase(Locale.ENGLISH)),
        ruleDto.getDefRemediationGapMultiplier(),
        ruleDto.getDefRemediationBaseEffort());
    }
  }

  @CheckForNull
  private static DebtRemediationFunction debtRemediationFunction(RuleDto ruleDto) {
    final String function = ruleDto.getRemediationFunction();
    if (function == null || function.isEmpty()) {
      return defaultDebtRemediationFunction(ruleDto);
    } else {
      return new DefaultDebtRemediationFunction(
        DebtRemediationFunction.Type.valueOf(function.toUpperCase(Locale.ENGLISH)),
        ruleDto.getRemediationGapMultiplier(),
        ruleDto.getRemediationBaseEffort());
    }
  }

  private static String toDateTime(@Nullable Long dateTimeMs) {
    return Optional.ofNullable(dateTimeMs).map(DateUtils::formatDateTime).orElse(null);
  }

}
