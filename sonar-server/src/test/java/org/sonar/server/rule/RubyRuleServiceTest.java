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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.Severity;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RubyRuleServiceTest {

  @Mock
  Rules rules;

  @Mock
  RuleRegistry ruleRegistry;

  RubyRuleService facade;

  @Before
  public void setUp() throws Exception {
    facade = new RubyRuleService(rules, ruleRegistry);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void should_translate_arguments_ind_find_ids() {
    Map<String, String> options = Maps.newHashMap();
    String status = " ";
    String repositories = "repo1|repo2";
    String searchText = "search text";

    options.put("status", status);
    options.put("repositories", repositories);
    // language not specified to cover blank option case
    options.put("searchtext", searchText);

    facade.findIds(options);
    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    verify(ruleRegistry).findIds(captor.capture());
    Map<String, String> params = (Map<String, String>) captor.getValue();
    assertThat(params.get("status")).isNull();
    assertThat(params.get("repositoryKey")).isEqualTo(repositories);
    assertThat(params.get("language")).isNull();
    assertThat(params.get("nameOrKey")).isEqualTo(searchText);
  }

  @Test
  public void should_update_index_when_rule_saved() {
    // this is not a magic number
    int ruleId = 42;
    facade.saveOrUpdate(ruleId);
    verify(ruleRegistry).saveOrUpdate(ruleId);
  }

  @Test
  public void update_rule_note(){
    facade.updateRuleNote(10, "My note");
    verify(rules).updateRuleNote(10, "My note");
  }

  @Test
  public void create_rule(){
    facade.createRule(10, "Rule name", Severity.MAJOR, "My note", ImmutableMap.of("max", "20"));
    verify(rules).createRule(10, "Rule name", Severity.MAJOR, "My note", ImmutableMap.of("max", "20"));
  }

  @Test
  public void update_rule(){
    facade.updateRule(10, "Rule name", Severity.MAJOR, "My note", ImmutableMap.of("max", "20"));
    verify(rules).updateRule(10, "Rule name", Severity.MAJOR, "My note", ImmutableMap.of("max", "20"));
  }

  @Test
  public void delete_note(){
    facade.deleteRule(10);
    verify(rules).deleteRule(10);
  }

  @Test
  public void update_rule_tags(){
    facade.updateRuleTags(10, ImmutableList.of("tag1", "tag2"));
    verify(rules).updateRuleTags(10,  ImmutableList.of("tag1", "tag2"));
  }

  @Test
  public void find_by_params(){
    facade.find(ImmutableMap.<String, Object>of("languages", Lists.newArrayList("java")));
    verify(rules).find(argThat(new ArgumentMatcher<RuleQuery>() {
      @Override
      public boolean matches(Object o) {
        RuleQuery query = (RuleQuery) o;
        return query.languages().contains("java");
      }
    }));
  }

  @Test
  public void create_rule_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("searchQuery", "NPE");
    map.put("key", "rule");
    map.put("languages", newArrayList("java", "xoo"));
    map.put("repositories", newArrayList("pmd", "checkstyle"));
    map.put("severities", newArrayList("MINOR", "MAJOR"));
    map.put("statuses", newArrayList("READY", "BETA"));
    map.put("tags", newArrayList("has-params", "keep-enabled"));
    map.put("debtCharacteristics", newArrayList("MODULARITY", "REUSABILITY"));
    map.put("hasDebtCharacteristic", "true");

    map.put("pageSize", 10l);
    map.put("pageIndex", 50);

    RuleQuery query = RubyRuleService.toRuleQuery(map);
    assertThat(query.searchQuery()).isEqualTo("NPE");
    assertThat(query.key()).isEqualTo("rule");
    assertThat(query.languages()).containsOnly("java", "xoo");
    assertThat(query.repositories()).containsOnly("pmd", "checkstyle");
    assertThat(query.severities()).containsOnly("MINOR", "MAJOR");
    assertThat(query.statuses()).containsOnly("READY", "BETA");
    assertThat(query.tags()).containsOnly("has-params", "keep-enabled");
    assertThat(query.debtCharacteristics()).containsOnly("MODULARITY", "REUSABILITY");
    assertThat(query.hasDebtCharacteristic()).isTrue();
    assertThat(query.pageSize()).isEqualTo(10);
    assertThat(query.pageIndex()).isEqualTo(50);
  }

  @Test
  public void just_for_fun_and_coverage() throws Exception {
    facade.start();
    facade.stop();
    // do not fail
  }
}
