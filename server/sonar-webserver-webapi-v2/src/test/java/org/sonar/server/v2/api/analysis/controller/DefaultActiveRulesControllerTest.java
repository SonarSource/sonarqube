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
package org.sonar.server.v2.api.analysis.controller;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.server.rule.ActiveRuleRestReponse;
import org.sonar.server.v2.api.analysis.service.ActiveRulesHandler;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.ACTIVE_RULES_ENDPOINT;
import static org.sonar.server.v2.api.ControllerTester.getMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultActiveRulesControllerTest {

  private final ActiveRulesHandler handler = mock(ActiveRulesHandler.class);

  private final MockMvc mockMvc = getMockMvc(new DefaultActiveRulesController(handler));

  @Test
  void getActiveRules_shouldReturnActiveRulesAsJson() throws Exception {
    var minimalAr = new ActiveRuleRestReponse.ActiveRule(
      new ActiveRuleRestReponse.RuleKey("xoo", "rule1"),
      "Rule 1",
      "MAJOR",
      "2024-12-09",
      "2024-12-09",
      null,
      "java",
      null,
      "qProfileKey",
      List.of(),
      List.of(),
      Map.of());
    var maximalAr = new ActiveRuleRestReponse.ActiveRule(
      new ActiveRuleRestReponse.RuleKey("xoo", "rule1"),
      "Rule 1",
      "MAJOR",
      "2024-12-09",
      "2024-12-09",
      "someInternalKey",
      "java",
      "templateKey",
      "qProfileKey",
      List.of(new ActiveRuleRestReponse.RuleKey("old", "rule")),
      List.of(new ActiveRuleRestReponse.Param("key", "value")),
      Map.of(SoftwareQuality.MAINTAINABILITY, Severity.HIGH));
    when(handler.getActiveRules("someKey")).thenReturn(List.of(minimalAr, maximalAr));

    String expectedJson = """
      [
        {
          "ruleKey":{"repository":"xoo","rule":"rule1"},
          "name":"Rule 1",
          "severity":"MAJOR",
          "createdAt":"2024-12-09",
          "updatedAt":"2024-12-09",
          "language":"java",
          "qProfileKey":"qProfileKey"
        },
        {
          "ruleKey":{"repository":"xoo","rule":"rule1"},
          "name":"Rule 1","severity":"MAJOR",
          "createdAt":"2024-12-09",
          "updatedAt":"2024-12-09",
          "internalKey":"someInternalKey",
          "language":"java",
          "templateRuleKey":"templateKey",
          "qProfileKey":"qProfileKey",
          "deprecatedKeys":[{"repository":"old","rule":"rule"}],
          "params":[{"key":"key","value":"value"}],
          "impacts":{"MAINTAINABILITY":"HIGH"}}
      ]
      """;

    mockMvc.perform(get(ACTIVE_RULES_ENDPOINT + "?projectKey=someKey"))
      .andExpectAll(
        status().isOk(),
        content().json(expectedJson, JsonCompareMode.STRICT));
  }

}
