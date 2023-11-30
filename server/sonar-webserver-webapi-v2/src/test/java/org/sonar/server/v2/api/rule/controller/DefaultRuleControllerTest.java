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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.server.common.rule.service.RuleService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.rule.converter.RuleRestResponseGenerator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.sonar.server.v2.WebApiEndpoints.RULES_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRuleControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final RuleService ruleService = mock();

  private final MockMvc mockMvc = ControllerTester
    .getMockMvc(new DefaultRuleController(userSession, ruleService, new RuleRestResponseGenerator()));


  @Test
  public void create() throws Exception {
    mockMvc.perform(post(RULES_ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE).content("{}"))
      .andExpectAll(
        status().isOk());
  }
}
