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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WsTester;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.paging.PagingResult;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleQuery;
import org.sonar.server.rule.Rules;
import org.sonar.server.user.MockUserSession;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleSearchWsHandlerTest {

  @Mock
  Rules rules;

  @Mock
  Languages languages;

  Rule.Builder ruleBuilder = new Rule.Builder()
    .setKey("AvoidCycle")
    .setRepositoryKey("squid")
    .setName("Avoid cycle")
    .setDescription("Avoid cycle between packages")
    .setLanguage("java");

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new RulesWs(new RuleSearchWsHandler(rules, languages), mock(RuleShowWsHandler.class), mock(AddTagsWsHandler.class), mock(RemoveTagsWsHandler.class)));
  }

  @Test
  public void search_rules() throws Exception {
    final int pageSize = 10;
    final int pageIndex = 2;
    Rule rule = ruleBuilder.build();

    when(rules.find(any(RuleQuery.class))).thenReturn(
      new PagedResult<Rule>(ImmutableList.of(rule), PagingResult.create(pageSize, pageIndex, 1)));
    Language lang = mock(Language.class);
    when(lang.getName()).thenReturn("Java");
    when(languages.get("java")).thenReturn(lang);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("list").setParam("ps", "10").setParam("p", "2");
    request.execute().assertJson("{'more':false,'total':1,'results':["
      + "{'key':'squid:AvoidCycle','name':'Avoid cycle','language':'Java'}"
      + "]}");
  }

  @Test
  public void search_rule_by_key() throws Exception {
    String ruleKey = "squid:AvoidCycle";
    Rule rule = ruleBuilder.build();

    when(rules.findByKey(RuleKey.parse(ruleKey))).thenReturn(rule);
    Language lang = mock(Language.class);
    when(lang.getName()).thenReturn("Java");
    when(languages.get("java")).thenReturn(lang);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("list").setParam("k", ruleKey);
    request.execute().assertJson("{'more':false,'total':1,'results':["
      + "{'key':'squid:AvoidCycle','name':'Avoid cycle','language':'Java'}"
      + "]}");
  }
}
