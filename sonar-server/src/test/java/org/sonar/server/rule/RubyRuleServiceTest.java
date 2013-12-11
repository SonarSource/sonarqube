/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RubyRuleServiceTest {

  RuleI18nManager i18n = mock(RuleI18nManager.class);
  RuleRegistry ruleRegistry = mock(RuleRegistry.class);
  RubyRuleService facade = new RubyRuleService(i18n, ruleRegistry);

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
  public void just_for_fun_and_coverage() throws Exception {
    facade.start();
    facade.stop();
    // do not fail
  }
}
