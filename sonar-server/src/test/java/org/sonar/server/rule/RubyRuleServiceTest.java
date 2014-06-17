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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RubyRuleServiceTest {

  @Mock
  RuleService ruleService;

  @Mock
  RuleUpdater updater;

  RubyRuleService service;

  @Before
  public void setUp() throws Exception {
    service = new RubyRuleService(ruleService, updater);
  }

  @Test
  public void find_by_key() throws Exception {
    service.findByKey("squid:S001");
    verify(ruleService).getByKey(RuleKey.of("squid", "S001"));
  }

  @Test
  public void update_rule() throws Exception {
    when(ruleService.search(any(RuleQuery.class), any(QueryOptions.class))).thenReturn(mock(Result.class));

    service.updateRule(ImmutableMap.<String, Object>of("ruleKey", "squid:S001"));

    verify(updater).update(any(RuleUpdate.class), any(UserSession.class));
  }

  @Test
  public void search_manual_rules() throws Exception {
    when(ruleService.search(any(RuleQuery.class), any(QueryOptions.class))).thenReturn(mock(Result.class));

    service.searchManualRules();

    verify(ruleService).search(any(RuleQuery.class), any(QueryOptions.class));
  }

  @Test
  public void create_manual_rules() throws Exception {
    service.createManualRule(ImmutableMap.<String, Object>of("manualKey", "MY_MANUAL"));

    verify(ruleService).create(any(NewRule.class));
  }

  @Test
  public void update_manual_rules() throws Exception {
    service.updateManualRule(ImmutableMap.<String, Object>of("ruleKey", "manual:MY_MANUAL"));

    verify(ruleService).update(any(RuleUpdate.class));
  }

  @Test
  public void delete_manual_rules() throws Exception {
    service.deleteManualRule("manual:MY_MANUAL");

    verify(ruleService).delete(RuleKey.of("manual", "MY_MANUAL"));
  }
}
