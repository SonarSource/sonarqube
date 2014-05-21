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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
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
  public void translate_arguments_ind_find_ids() {
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
  public void update_rule() {
    Map<String, Object> params = newHashMap();
    params.put("ruleKey", "squid:UselessImportCheck");
    params.put("debtCharacteristicKey", "MODULARITY");
    params.put("debtRemediationFunction", "LINEAR_OFFSET");
    params.put("debtRemediationCoefficient", "1h");
    params.put("debtRemediationOffset", "10min");

    facade.updateRule(params);
    ArgumentCaptor<RuleOperations.RuleChange> ruleChangeCaptor = ArgumentCaptor.forClass(RuleOperations.RuleChange.class);
    verify(rules).updateRule(ruleChangeCaptor.capture());

    RuleOperations.RuleChange ruleChange = ruleChangeCaptor.getValue();
    assertThat(ruleChange.ruleKey()).isEqualTo(RuleKey.of("squid", "UselessImportCheck"));
    assertThat(ruleChange.debtCharacteristicKey()).isEqualTo("MODULARITY");
    assertThat(ruleChange.debtRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleChange.debtRemediationCoefficient()).isEqualTo("1h");
    assertThat(ruleChange.debtRemediationOffset()).isEqualTo("10min");
  }

  @Test
  public void find_by_key() {
    facade.findByKey("repo:key");
    verify(rules).findByKey(RuleKey.of("repo", "key"));
  }

  @Test
  public void just_for_fun_and_coverage() throws Exception {
    facade.start();
    facade.stop();
    // do not fail
  }
}
