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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ThreadLocalUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RubyRuleServiceTest {

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  RuleService ruleService;

  @Mock
  RuleUpdater updater;

  @Captor
  ArgumentCaptor<QueryContext> optionsCaptor;

  @Captor
  ArgumentCaptor<RuleQuery> ruleQueryCaptor;

  RubyRuleService service;

  @Before
  public void setUp() {
    service = new RubyRuleService(ruleService, updater, userSessionRule);
  }

  @Test
  public void find_by_key() {
    service.findByKey("squid:S001");
    verify(ruleService).getByKey(RuleKey.of("squid", "S001"));
  }

  @Test
  public void search_rules() {
    when(ruleService.search(any(RuleQuery.class), any(QueryContext.class))).thenReturn(mock(Result.class));

    HashMap<String, Object> params = newHashMap();
    params.put("searchQuery", "Exception");
    params.put("key", "S001");
    params.put("languages", "xoo,js");
    params.put("repositories", "checkstyle,pmd");
    params.put("severities", "MAJOR,MINOR");
    params.put("statuses", "BETA,READY");
    params.put("tags", "tag1,tag2");
    params.put("debtCharacteristics", "char1,char2");
    params.put("hasDebtCharacteristic", "true");
    params.put(Param.PAGE, "1");
    params.put("pageSize", "40");
    service.find(params);

    verify(ruleService).search(ruleQueryCaptor.capture(), optionsCaptor.capture());

    assertThat(ruleQueryCaptor.getValue().getQueryText()).isEqualTo("Exception");
    assertThat(ruleQueryCaptor.getValue().getKey()).isEqualTo("S001");
    assertThat(ruleQueryCaptor.getValue().getLanguages()).containsOnly("xoo", "js");
    assertThat(ruleQueryCaptor.getValue().getRepositories()).containsOnly("checkstyle", "pmd");
    assertThat(ruleQueryCaptor.getValue().getRepositories()).containsOnly("checkstyle", "pmd");
    assertThat(ruleQueryCaptor.getValue().getSeverities()).containsOnly("MAJOR", "MINOR");
    assertThat(ruleQueryCaptor.getValue().getStatuses()).containsOnly(RuleStatus.BETA, RuleStatus.READY);
    assertThat(ruleQueryCaptor.getValue().getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleQueryCaptor.getValue().getDebtCharacteristics()).containsOnly("char1", "char2");
    assertThat(ruleQueryCaptor.getValue().getHasDebtCharacteristic()).isTrue();
    assertThat(ruleQueryCaptor.getValue().getQProfileKey()).isNull();
    assertThat(ruleQueryCaptor.getValue().getActivation()).isNull();

    assertThat(optionsCaptor.getValue().getLimit()).isEqualTo(40);
    assertThat(optionsCaptor.getValue().getOffset()).isEqualTo(0);
  }

  @Test
  public void search_rules_activated_on_a_profile() {
    when(ruleService.search(any(RuleQuery.class), any(QueryContext.class))).thenReturn(mock(Result.class));

    HashMap<String, Object> params = newHashMap();
    params.put("profile", "xoo-profile");
    service.find(params);

    verify(ruleService).search(ruleQueryCaptor.capture(), optionsCaptor.capture());

    assertThat(ruleQueryCaptor.getValue().getQProfileKey()).isEqualTo("xoo-profile");
    assertThat(ruleQueryCaptor.getValue().getActivation()).isTrue();
  }

  @Test
  public void search_rules_without_page_size_param() {
    when(ruleService.search(any(RuleQuery.class), any(QueryContext.class))).thenReturn(mock(Result.class));

    HashMap<String, Object> params = newHashMap();
    params.put(Param.PAGE, "1");
    service.find(params);

    verify(ruleService).search(ruleQueryCaptor.capture(), optionsCaptor.capture());

    assertThat(optionsCaptor.getValue().getLimit()).isEqualTo(50);
    assertThat(optionsCaptor.getValue().getOffset()).isEqualTo(0);
  }

  @Test
  public void search_all_rules() {
    List<Rule> rules = newArrayList(mock(Rule.class));
    Result serviceResult = mock(Result.class);
    when(serviceResult.scroll()).thenReturn(rules.iterator());

    when(ruleService.search(any(RuleQuery.class), any(QueryContext.class))).thenReturn(serviceResult);

    HashMap<String, Object> params = newHashMap();
    params.put("pageSize", "-1");
    PagedResult<Rule> result = service.find(params);

    verify(serviceResult).scroll();

    verify(ruleService).search(ruleQueryCaptor.capture(), optionsCaptor.capture());
    assertThat(result.paging().pageSize()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void update_rule() {
    when(ruleService.search(any(RuleQuery.class), any(QueryContext.class))).thenReturn(mock(Result.class));

    service.updateRule(ImmutableMap.<String, Object>of("ruleKey", "squid:S001"));

    verify(updater).update(any(RuleUpdate.class), any(ThreadLocalUserSession.class));
  }

  @Test
  public void search_manual_rules() {
    when(ruleService.search(any(RuleQuery.class), any(QueryContext.class))).thenReturn(mock(Result.class));

    service.searchManualRules();

    verify(ruleService).search(any(RuleQuery.class), any(QueryContext.class));
  }

  @Test
  public void create_manual_rules() {
    service.createManualRule(ImmutableMap.<String, Object>of("manualKey", "MY_MANUAL"));

    verify(ruleService).create(any(NewRule.class));
  }

  @Test
  public void update_manual_rules() {
    service.updateManualRule(ImmutableMap.<String, Object>of("ruleKey", "manual:MY_MANUAL"));

    verify(ruleService).update(any(RuleUpdate.class));
  }

  @Test
  public void delete_manual_rules() {
    service.deleteManualRule("manual:MY_MANUAL");

    verify(ruleService).delete(RuleKey.of("manual", "MY_MANUAL"));
  }
}
