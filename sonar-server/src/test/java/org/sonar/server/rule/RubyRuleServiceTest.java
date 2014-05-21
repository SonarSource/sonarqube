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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.rule2.RuleService;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RubyRuleServiceTest {

  @Mock
  RuleService service;

  RubyRuleService rubyService;

  @Before
  public void setUp() throws Exception {
    rubyService = new RubyRuleService(service);
  }

  @Test
  public void update_rule() {
    Map<String, Object> params = newHashMap();
    params.put("ruleKey", "squid:UselessImportCheck");
    params.put("debtCharacteristicKey", "MODULARITY");
    params.put("debtRemediationFunction", "LINEAR_OFFSET");
    params.put("debtRemediationCoefficient", "1h");
    params.put("debtRemediationOffset", "10min");

//    rubyService.updateRule(params);
//    ArgumentCaptor<RuleOperations.RuleChange> ruleChangeCaptor = ArgumentCaptor.forClass(RuleOperations.RuleChange.class);
//    verify(rules).updateRule(ruleChangeCaptor.capture());
//
//    RuleOperations.RuleChange ruleChange = ruleChangeCaptor.getValue();
//    assertThat(ruleChange.ruleKey()).isEqualTo(RuleKey.of("squid", "UselessImportCheck"));
//    assertThat(ruleChange.debtCharacteristicKey()).isEqualTo("MODULARITY");
//    assertThat(ruleChange.debtRemediationFunction()).isEqualTo("LINEAR_OFFSET");
//    assertThat(ruleChange.debtRemediationCoefficient()).isEqualTo("1h");
//    assertThat(ruleChange.debtRemediationOffset()).isEqualTo("10min");
  }

  @Test
  public void find_by_key() {
    rubyService.findByKey("repo:key");
    verify(service).getByKey(RuleKey.of("repo", "key"));
  }

  @Test
  public void just_for_fun_and_coverage() throws Exception {
    rubyService.start();
    rubyService.stop();
    // do not fail
  }
}
