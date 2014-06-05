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

package org.sonar.server.rule.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.server.rule.NewRule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CreateActionTest {

  @Mock
  RuleService ruleService;

  @Captor
  ArgumentCaptor<NewRule> newRuleCaptor;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new RulesWebService(mock(SearchAction.class), mock(ShowAction.class), mock(TagsAction.class), new CreateAction(ruleService),
      mock(AppAction.class), mock(UpdateAction.class)));
  }

  @Test
  public void create_custom_rule() throws Exception {
    WsTester.TestRequest request = tester.newGetRequest("api/rules", "create")
      .setParam("template_key", "java:S001")
      .setParam("name", "My custom rule")
      .setParam("html_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "key=value");
    request.execute();

    verify(ruleService).create(newRuleCaptor.capture());

    NewRule newRule = newRuleCaptor.getValue();
    assertThat(newRule.templateKey()).isEqualTo(RuleKey.of("java", "S001"));
    assertThat(newRule.name()).isEqualTo("My custom rule");
    assertThat(newRule.htmlDescription()).isEqualTo("Description");
    assertThat(newRule.severity()).isEqualTo("MAJOR");
    assertThat(newRule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(newRule.params()).hasSize(1);
    assertThat(newRule.params().get("key")).isEqualTo("value");
  }
}
