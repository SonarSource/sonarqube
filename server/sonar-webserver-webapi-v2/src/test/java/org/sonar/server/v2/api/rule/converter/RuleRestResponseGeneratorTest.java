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
package org.sonar.server.v2.api.rule.converter;

import java.util.List;
import java.util.Locale;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.common.rule.service.RuleInformation;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.v2.api.rule.resource.Parameter;
import org.sonar.server.v2.api.rule.response.RuleDescriptionSectionRestResponse;
import org.sonar.server.v2.api.rule.response.RuleRestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleRestResponseGeneratorTest {
  @Mock
  private Languages languages;

  @Mock
  private MacroInterpreter macroInterpreter;

  @Mock
  RuleDescriptionFormatter ruleDescriptionFormatter;

  @InjectMocks
  private RuleRestResponseGenerator ruleRestResponseGenerator;

  @Test
  public void toRuleRestResponse_shouldReturnSameFieldForStandardMapping() {
    when(macroInterpreter.interpret(Mockito.anyString())).thenAnswer(invocation -> "interpreted" + invocation.getArgument(0));
    when(ruleDescriptionFormatter.toHtml(any(), any())).thenAnswer(invocation -> "html" + ((RuleDescriptionSectionDto) invocation.getArgument(1)).getContent());

    RuleDto dto = RuleTesting.newRule();
    when(languages.get(dto.getLanguage())).thenReturn(LanguageTesting.newLanguage(dto.getLanguage(), "languageName"));

    RuleRestResponse ruleRestResponse = ruleRestResponseGenerator.toRuleRestResponse(new RuleInformation(dto, List.of()));
    assertThat(ruleRestResponse.id()).isEqualTo(dto.getUuid());
    assertThat(ruleRestResponse.key()).isEqualTo(dto.getKey().toString());
    assertThat(ruleRestResponse.repositoryKey()).isEqualTo(dto.getRepositoryKey());
    assertThat(ruleRestResponse.name()).isEqualTo(dto.getName());
    assertThat(ruleRestResponse.descriptionSections())
      .extracting(RuleDescriptionSectionRestResponse::key, RuleDescriptionSectionRestResponse::content, RuleDescriptionSectionRestResponse::context)
      .containsExactly(dto.getRuleDescriptionSectionDtos().stream().map(s -> tuple(s.getKey(), "interpreted" + "html" + s.getContent(), s.getContext())).toArray(Tuple[]::new));
    assertThat(ruleRestResponse.severity()).isEqualTo(dto.getSeverityString());
    assertThat(ruleRestResponse.type().name()).isEqualTo(RuleType.valueOf(dto.getType()).name());
    assertThat(ruleRestResponse.impacts()).extracting(r -> r.severity().name(), r -> r.softwareQuality().name())
      .containsExactly(dto.getDefaultImpacts().stream().map(e -> tuple(e.getSeverity().name(), e.getSoftwareQuality().name())).toArray(Tuple[]::new));
    assertThat(ruleRestResponse.cleanCodeAttribute().name()).isEqualTo(dto.getCleanCodeAttribute().name());
    assertThat(ruleRestResponse.cleanCodeAttributeCategory().name()).isEqualTo(dto.getCleanCodeAttribute().getAttributeCategory().name());
    assertThat(ruleRestResponse.status().name()).isEqualTo(dto.getStatus().name());
    assertThat(ruleRestResponse.external()).isEqualTo(dto.isExternal());
    assertThat(ruleRestResponse.createdAt()).isEqualTo(DateUtils.formatDateTime(dto.getCreatedAt()));
    assertThat(ruleRestResponse.gapDescription()).isEqualTo(dto.getGapDescription());
    assertThat(ruleRestResponse.markdownNote()).isEqualTo(dto.getNoteData());
    assertThat(ruleRestResponse.educationPrinciples()).containsExactlyElementsOf(dto.getEducationPrinciples());
    assertThat(ruleRestResponse.template()).isEqualTo(dto.isTemplate());
    assertThat(ruleRestResponse.templateId()).isEqualTo(dto.getTemplateUuid());
    assertThat(ruleRestResponse.tags()).containsExactlyElementsOf(dto.getTags());
    assertThat(ruleRestResponse.systemTags()).containsExactlyElementsOf(dto.getSystemTags());
    assertThat(ruleRestResponse.languageKey()).isEqualTo(dto.getLanguage());
    assertThat(ruleRestResponse.languageName()).isEqualTo("languageName");

    DefaultDebtRemediationFunction function = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(dto.getRemediationFunction().toUpperCase(Locale.ENGLISH)),
      dto.getRemediationGapMultiplier(),
      dto.getRemediationBaseEffort());
    assertThat(ruleRestResponse.remediationFunctionBaseEffort()).isEqualTo(function.baseEffort());
    assertThat(ruleRestResponse.remediationFunctionGapMultiplier()).isEqualTo(function.gapMultiplier());
    assertThat(ruleRestResponse.remediationFunctionType()).isEqualTo(dto.getRemediationFunction());
  }

  @Test
  public void toRuleRestResponse_shouldReturnNullFields_whenRuleIsEmpty() {
    RuleDto dto = new RuleDto().setRuleKey("key").setRepositoryKey("repoKey").setStatus(RuleStatus.READY).setType(RuleType.BUG.getDbConstant());
    RuleRestResponse ruleRestResponse = ruleRestResponseGenerator.toRuleRestResponse(new RuleInformation(dto, List.of()));
    assertThat(ruleRestResponse.cleanCodeAttribute()).isNull();
    assertThat(ruleRestResponse.cleanCodeAttributeCategory()).isNull();
    assertThat(ruleRestResponse.htmlNote()).isNull();
  }

  @Test
  public void toRuleRestResponse_shouldReturnParameters_whenParametersAreProvided() {
    RuleDto dto = RuleTesting.newRule();
    RuleParamDto ruleParamDto1 = RuleTesting.newRuleParam(dto);
    RuleParamDto ruleParamDto2 = RuleTesting.newRuleParam(dto);
    RuleRestResponse ruleRestResponse = ruleRestResponseGenerator.toRuleRestResponse(new RuleInformation(dto, List.of(ruleParamDto1, ruleParamDto2)));

    assertThat(ruleRestResponse.parameters()).extracting(Parameter::key, Parameter::htmlDescription, Parameter::defaultValue)
      .containsExactlyInAnyOrder(tuple(ruleParamDto1.getName(), ruleParamDto1.getDescription(), ruleParamDto1.getDefaultValue()),
        tuple(ruleParamDto2.getName(), ruleParamDto2.getDescription(), ruleParamDto2.getDefaultValue()));
  }

  @Test
  public void toRuleRestResponse_shouldReturnAdhocInformation_whenRuleIsAdhoc() {
    when(macroInterpreter.interpret(Mockito.anyString())).thenAnswer(invocation -> "interpreted" + invocation.getArgument(0));
    RuleDto dto = RuleTesting.newRule();
    dto.setIsAdHoc(true);
    dto.setAdHocName("adhocName");
    dto.setAdHocDescription("adhocDescription");
    dto.setAdHocSeverity(Severity.INFO);
    dto.setAdHocType(RuleType.BUG.getDbConstant());

    RuleRestResponse ruleRestResponse = ruleRestResponseGenerator.toRuleRestResponse(new RuleInformation(dto, List.of()));
    assertThat(ruleRestResponse.name()).isEqualTo(dto.getAdHocName());
    assertThat(ruleRestResponse.descriptionSections())
      .extracting(RuleDescriptionSectionRestResponse::key, RuleDescriptionSectionRestResponse::content, RuleDescriptionSectionRestResponse::context)
      .containsExactly(tuple("default", "interpreted" + dto.getAdHocDescription(), null));
    assertThat(ruleRestResponse.severity()).isEqualTo(dto.getAdHocSeverity());
    assertThat(ruleRestResponse.type().name()).isEqualTo(RuleType.valueOf(dto.getAdHocType()).name());
  }

  @Test
  public void toRuleRestResponse_shouldReturnRemediationFunctions() {
    when(macroInterpreter.interpret(Mockito.anyString())).thenAnswer(invocation -> "interpreted" + invocation.getArgument(0));
    RuleDto dto = RuleTesting.newRule();
    dto.setIsAdHoc(true);
    dto.setAdHocName("adhocName");
    dto.setAdHocDescription("adhocDescription");
    dto.setAdHocSeverity(Severity.INFO);
    dto.setAdHocType(RuleType.BUG.getDbConstant());

    RuleRestResponse ruleRestResponse = ruleRestResponseGenerator.toRuleRestResponse(new RuleInformation(dto, List.of()));
    assertThat(ruleRestResponse.name()).isEqualTo(dto.getAdHocName());
    assertThat(ruleRestResponse.descriptionSections())
      .extracting(RuleDescriptionSectionRestResponse::key, RuleDescriptionSectionRestResponse::content, RuleDescriptionSectionRestResponse::context)
      .containsExactly(tuple("default", "interpreted" + dto.getAdHocDescription(), null));
    assertThat(ruleRestResponse.severity()).isEqualTo(dto.getAdHocSeverity());
    assertThat(ruleRestResponse.type().name()).isEqualTo(RuleType.valueOf(dto.getAdHocType()).name());
  }

}
