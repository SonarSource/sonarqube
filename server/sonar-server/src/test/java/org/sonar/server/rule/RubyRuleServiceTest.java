/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ThreadLocalUserSession;

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
  DbClient dbClient;

  @Mock
  DbSession dbSession;

  @Mock
  RuleDao ruleDao;

  @Mock
  RuleUpdater updater;

  @Captor
  ArgumentCaptor<SearchOptions> optionsCaptor;

  @Captor
  ArgumentCaptor<RuleQuery> ruleQueryCaptor;

  RubyRuleService service;

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.ruleDao()).thenReturn(ruleDao);
    service = new RubyRuleService(dbClient, ruleService, updater, userSessionRule);
  }

  @Test
  public void find_by_key() {
    RuleKey ruleKey = RuleKey.of("squid", "S001");
    RuleDto ruleDto = RuleTesting.newXooX1();

    when(ruleDao.selectByKey(dbSession, ruleKey)).thenReturn(Optional.of(ruleDto));

    assertThat(service.findByKey("squid:S001")).isEqualTo(ruleDto);
  }

  @Test
  public void search_rules() {
    when(ruleService.search(any(RuleQuery.class), any(SearchOptions.class))).thenReturn(mock(SearchIdResult.class));

    HashMap<String, Object> params = newHashMap();
    params.put("searchQuery", "Exception");
    params.put("key", "S001");
    params.put("languages", "xoo,js");
    params.put("repositories", "checkstyle,pmd");
    params.put("severities", "MAJOR,MINOR");
    params.put("statuses", "BETA,READY");
    params.put("tags", "tag1,tag2");
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
    assertThat(ruleQueryCaptor.getValue().getQProfileKey()).isNull();
    assertThat(ruleQueryCaptor.getValue().getActivation()).isNull();

    assertThat(optionsCaptor.getValue().getLimit()).isEqualTo(40);
    assertThat(optionsCaptor.getValue().getOffset()).isEqualTo(0);
  }

  @Test
  public void search_rules_activated_on_a_profile() {
    when(ruleService.search(any(RuleQuery.class), any(SearchOptions.class))).thenReturn(mock(SearchIdResult.class));

    HashMap<String, Object> params = newHashMap();
    params.put("profile", "xoo-profile");
    service.find(params);

    verify(ruleService).search(ruleQueryCaptor.capture(), optionsCaptor.capture());

    assertThat(ruleQueryCaptor.getValue().getQProfileKey()).isEqualTo("xoo-profile");
    assertThat(ruleQueryCaptor.getValue().getActivation()).isTrue();
  }

  @Test
  public void search_rules_without_page_size_param() {
    when(ruleService.search(any(RuleQuery.class), any(SearchOptions.class))).thenReturn(mock(SearchIdResult.class));

    HashMap<String, Object> params = newHashMap();
    params.put(Param.PAGE, "1");
    service.find(params);

    verify(ruleService).search(ruleQueryCaptor.capture(), optionsCaptor.capture());

    assertThat(optionsCaptor.getValue().getLimit()).isEqualTo(50);
    assertThat(optionsCaptor.getValue().getOffset()).isEqualTo(0);
  }

  @Test
  public void update_rule() {
    when(ruleService.search(any(RuleQuery.class), any(SearchOptions.class))).thenReturn(mock(SearchIdResult.class));

    service.updateRule(ImmutableMap.<String, Object>of("ruleKey", "squid:S001"));

    verify(updater).update(any(RuleUpdate.class), any(ThreadLocalUserSession.class));
  }
}
