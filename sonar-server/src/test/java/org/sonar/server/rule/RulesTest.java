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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RulesTest {

  @Mock
  RuleDao ruleDao;

  @Mock
  RuleOperations ruleOperations;

  @Mock
  RuleRegistry ruleRegistry;

  Rules rules;

  @Before
  public void setUp() {
    rules = new Rules(ruleDao, ruleOperations, ruleRegistry);
  }

  @Test
  public void update_rule() throws Exception {
    RuleOperations.RuleChange ruleChange = new RuleOperations.RuleChange();
    rules.updateRule(ruleChange);
    verify(ruleOperations).updateRule(eq(ruleChange), any(UserSession.class));
  }

  @Test
  public void create_rule_note() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    rules.updateRuleNote(10, "My note");

    verify(ruleOperations).updateRuleNote(eq(rule), eq("My note"), any(UserSession.class));
  }

  @Test
  public void delete_rule_note() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    rules.updateRuleNote(10, "");

    verify(ruleOperations).deleteRuleNote(eq(rule), any(UserSession.class));
  }

  @Test
  public void create_new_custom_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    RuleDto newRule = new RuleDto().setId(11);
    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    when(ruleOperations.createCustomRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My note"), eq(paramsByKey), any(UserSession.class))).thenReturn(newRule);

    assertThat(rules.createCustomRule(10, "Rule name", Severity.MAJOR, "My note", paramsByKey)).isEqualTo(11);

    verify(ruleOperations).createCustomRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My note"), eq(paramsByKey), any(UserSession.class));
  }

  @Test
  public void fail_to_create_new_custom_rule_on_empty_parameters() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    RuleDto newRule = new RuleDto().setId(11);
    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    when(ruleOperations.createCustomRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My note"), eq(paramsByKey), any(UserSession.class))).thenReturn(newRule);

    try {
      rules.createCustomRule(10, "", "", "", paramsByKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      assertThat(((BadRequestException) e).errors()).hasSize(3);
    }
    verifyZeroInteractions(ruleOperations);
  }

  @Test
  public void fail_to_create_new_custom_rule_when_rule_name_already_exists() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    when(ruleDao.selectByName("Rule name")).thenReturn(new RuleDto());

    RuleDto newRule = new RuleDto().setId(11);
    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    when(ruleOperations.createCustomRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My description"), eq(paramsByKey), any(UserSession.class))).thenReturn(newRule);

    try {
      rules.createCustomRule(10, "Rule name", Severity.MAJOR, "My description", paramsByKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      assertThat(((BadRequestException) e).errors()).hasSize(1);
    }
    verifyZeroInteractions(ruleOperations);
  }

  @Test
  public void update_custom_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(11)).thenReturn(rule);
    when(ruleDao.selectByName("Updated name")).thenReturn(null);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");

    rules.updateCustomRule(11, "Updated name", Severity.MAJOR, "Updated description", paramsByKey);

    verify(ruleOperations).updateCustomRule(eq(rule), eq("Updated name"), eq(Severity.MAJOR), eq("Updated description"), eq(paramsByKey), any(UserSession.class));
  }

  @Test
  public void update_custom_rule_with_same_name() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(11)).thenReturn(rule);
    when(ruleDao.selectByName("Rule name")).thenReturn(rule);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");

    rules.updateCustomRule(11, "Rule name", Severity.MAJOR, "Updated description", paramsByKey);

    verify(ruleOperations).updateCustomRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("Updated description"), eq(paramsByKey), any(UserSession.class));
  }

  @Test
  public void fail_to_update_custom_rule_when_no_parent() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254");
    when(ruleDao.selectById(11)).thenReturn(rule);
    when(ruleDao.selectByName("Rule name")).thenReturn(rule);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");

    try {
      rules.updateCustomRule(11, "Rule name", Severity.MAJOR, "Updated description", paramsByKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(ruleOperations);
  }

  @Test
  public void delete_custom_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(11)).thenReturn(rule);

    rules.deleteCustomRule(11);

    verify(ruleOperations).deleteCustomRule(eq(rule), any(UserSession.class));
  }

  @Test
  public void pass_tags_to_update() {
    final int ruleId = 11;
    RuleDto rule = new RuleDto().setId(ruleId).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(ruleId)).thenReturn(rule);

    rules.updateRuleTags(ruleId, null);
    verify(ruleOperations).updateRuleTags(eq(rule), isA(List.class), any(UserSession.class));
  }

  @Test
  public void prepare_empty_tag_list() {
    final int ruleId = 11;
    RuleDto rule = new RuleDto().setId(ruleId).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(ruleId)).thenReturn(rule);

    List<String> tags = ImmutableList.of("tag1", "tag2");
    rules.updateRuleTags(ruleId, tags);
    verify(ruleOperations).updateRuleTags(eq(rule), eq(tags), any(UserSession.class));
  }

  @Test
  public void find_by_key() {
    RuleKey key = RuleKey.of("polop", "palap");
    Rule rule = mock(Rule.class);
    when(ruleRegistry.findByKey(key)).thenReturn(rule );
    assertThat(rules.findByKey(key)).isEqualTo(rule);
    verify(ruleRegistry).findByKey(key);
  }
}
