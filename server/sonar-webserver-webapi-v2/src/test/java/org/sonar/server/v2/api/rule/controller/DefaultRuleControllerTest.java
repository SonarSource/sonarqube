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
package org.sonar.server.v2.api.rule.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.common.rule.ReactivationException;
import org.sonar.server.common.rule.service.RuleInformation;
import org.sonar.server.common.rule.service.RuleService;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.rule.converter.RuleRestResponseGenerator;
import org.sonar.server.v2.api.rule.enums.CleanCodeAttributeRestEnum;
import org.sonar.server.v2.api.rule.enums.ImpactSeverityRestEnum;
import org.sonar.server.v2.api.rule.enums.RuleStatusRestEnum;
import org.sonar.server.v2.api.rule.enums.SoftwareQualityRestEnum;
import org.sonar.server.v2.api.rule.request.RuleCreateRestRequest;
import org.sonar.server.v2.api.rule.resource.Impact;
import org.sonar.server.v2.api.rule.resource.Parameter;
import org.sonar.server.v2.api.rule.response.RuleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.v2.WebApiEndpoints.RULES_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRuleControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final RuleService ruleService = mock(RuleService.class);
  private final Languages languages = mock(Languages.class);
  private final MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private final RuleDescriptionFormatter ruleDescriptionFormatter = mock(RuleDescriptionFormatter.class);
  private final RuleRestResponseGenerator ruleRestResponseGenerator = new RuleRestResponseGenerator(languages, macroInterpreter,
    ruleDescriptionFormatter);

  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultRuleController(userSession, ruleService,
    ruleRestResponseGenerator));
  private static final Gson gson = new GsonBuilder().create();

  @Before
  public void setUp() {
    when(macroInterpreter.interpret(anyString())).thenAnswer(invocation -> "interpreted" + invocation.getArgument(0));
    when(ruleDescriptionFormatter.toHtml(any(), any())).thenAnswer(invocation -> "html" + ((RuleDescriptionSectionDto) invocation.getArgument(1)).getContent());
    when(languages.get(anyString())).thenAnswer(invocation -> LanguageTesting.newLanguage(invocation.getArgument(0, String.class),
      "languageName"));
  }

  @Test
  public void create_shouldReturnRule() throws Exception {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    RuleInformation ruleInformation = generateRuleInformation();
    when(ruleService.createCustomRule(any())).thenReturn(ruleInformation);

    MvcResult mvcResult = mockMvc.perform(
        post(RULES_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(newRequest())))
      .andExpect(status().isOk())
      .andReturn();

    RuleRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), RuleRestResponse.class);
    assertThat(response).isEqualTo(ruleRestResponseGenerator.toRuleRestResponse(ruleInformation));
  }

  @Test
  public void create_whenNotLoggedIn_shouldFailWithUnauthorized() throws Exception {
    mockMvc.perform(
        post(RULES_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(newRequest())))
      .andExpectAll(status().isUnauthorized(), content().json("{message:'Authentication is required'}"));
  }

  @Test
  public void create_whenNoPermission_shouldFailWithForbidden() throws Exception {
    userSession.logIn();

    mockMvc.perform(
        post(RULES_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(newRequest())))
      .andExpectAll(status().isForbidden(), content().json("{message:'Insufficient privileges'}"));
  }

  @Test
  public void create_whenReactivationExceptionThrown_shouldFailWithConflict() throws Exception {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    String errorMessage = "reactivation_exception";
    when(ruleService.createCustomRule(any())).thenThrow(new ReactivationException(errorMessage, null));

    mockMvc.perform(
        post(RULES_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(newRequest())))
      .andExpectAll(status().isConflict(), content().json(String.format("{message:'%s'}", errorMessage)));
  }

  @Test
  public void create_whenMissingBodyField_shouldFailWithBadRequest() throws Exception {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    RuleCreateRestRequest request = newRequest(null);

    mockMvc.perform(
        post(RULES_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(request)))
      .andExpectAll(status().isBadRequest(), content().json("{message:'Value {} for field name was rejected. Error: must not be null.'}"));
  }

  @Test
  public void create_whenInvalidParam_shouldFailWithBadRequest() throws Exception {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    RuleCreateRestRequest request = newRequest("a".repeat(201));

    mockMvc.perform(
        post(RULES_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(request)))
      .andExpectAll(status().isBadRequest(),
        content().json(String.format("{message:'Value %s for field name was rejected. Error: size must be between 0 and 200.'}",
          request.name())));
  }

  private static RuleCreateRestRequest newRequest() {
    return newRequest("custom rule name");
  }

  private static RuleCreateRestRequest newRequest(@Nullable String name) {
    return new RuleCreateRestRequest(
      "java:custom_rule",
      "java:template_rule",
      name,
      "some desc",
      RuleStatusRestEnum.BETA,
      List.of(new Parameter("key1", "desc", "value1", "text")),
      CleanCodeAttributeRestEnum.MODULAR,
      List.of(new Impact(SoftwareQualityRestEnum.MAINTAINABILITY, ImpactSeverityRestEnum.LOW)));
  }

  private RuleInformation generateRuleInformation() {
    RuleDto ruleDto = RuleTesting.newCustomRule(RuleTesting.newTemplateRule(RuleKey.parse("java:template_rule")));
    RuleTesting.newRuleParam(ruleDto);
    return new RuleInformation(ruleDto, List.of(RuleTesting.newRuleParam(ruleDto), RuleTesting.newRuleParam(ruleDto)));
  }
}
