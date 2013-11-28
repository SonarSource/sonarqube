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

import com.github.tlrx.elasticsearch.test.EsSetup;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.search.SearchIndex;
import org.sonar.server.search.SearchNode;
import org.sonar.test.TestUtils;

import java.util.HashMap;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleRegistryTest {

  private EsSetup esSetup;
  private SearchIndex searchIndex;
  private RuleDao ruleDao;
  private RuleI18nManager ruleI18nManager;
  RuleRegistry registry;

  @Before
  public void setUp() throws Exception {
    ruleDao = mock(RuleDao.class);
    ruleI18nManager = mock(RuleI18nManager.class);

    esSetup = new EsSetup();
    esSetup.execute(EsSetup.deleteAll());

    SearchNode node = mock(SearchNode.class);
    when(node.client()).thenReturn(esSetup.client());
    searchIndex = new SearchIndex(node);
    searchIndex.start();

    registry = new RuleRegistry(searchIndex, ruleDao, ruleI18nManager);
    registry.start();

    String source1 = IOUtils.toString(TestUtils.getResource(getClass(), "rules/rule1.json").toURI());
    String source2 = IOUtils.toString(TestUtils.getResource(getClass(), "rules/rule2.json").toURI());
    String source3 = IOUtils.toString(TestUtils.getResource(getClass(), "rules/rule3.json").toURI());

    esSetup.execute(
      EsSetup.index("rules", "rule", "1").withSource(source1),
      EsSetup.index("rules", "rule", "2").withSource(source2),
      EsSetup.index("rules", "rule", "3").withSource(source3)
    );

  }

  @After
  public void tearDown() {
    searchIndex.stop();
    esSetup.terminate();
  }

  @Test
  public void should_register_mapping_at_startup() {
    assertThat(esSetup.exists("rules")).isTrue();
    assertThat(esSetup.client().admin().indices().prepareTypesExists("rules").setTypes("rule").execute().actionGet().isExists()).isTrue();
  }

  @Test
  public void should_filter_removed_rules() {
    assertThat(registry.findIds(new HashMap<String, String>())).containsOnly(1, 2);
  }

  @Test
  public void should_display_disabled_rule() {
    assertThat(registry.findIds(ImmutableMap.of("status", "BETA|REMOVED"))).containsOnly(2, 3);
  }

  @Test
  public void should_filter_on_name_or_key() throws Exception {
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters"))).containsOnly(1);
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "issue"))).containsOnly(1, 2);
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "issue line"))).containsOnly(2);
  }

  @Test
  public void should_filter_on_key() throws Exception {
    assertThat(registry.findIds(ImmutableMap.of("key", "OneIssuePerLine"))).containsOnly(2);
  }

  @Test
  public void should_filter_on_multiple_criteria() {
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters", "key", "OneIssuePerLine"))).isEmpty();
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "polop"))).isEmpty();

    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters", "repositoryKey", "xoo"))).containsOnly(1);
  }

  @Test
  public void should_filter_on_multiple_values() {
    assertThat(registry.findIds(ImmutableMap.of("key", "RuleWithParameters|OneIssuePerLine"))).hasSize(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_reject_leading_wildcard() {
    registry.findIds(ImmutableMap.of("nameOrKey", "*ssue"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_wrap_parse_exceptions() {
    registry.findIds(ImmutableMap.of("nameOrKey", "\"'"));
  }

  @Test
  public void should_index_all_rules() {
    long ruleId1 = 3L;
    RuleDto rule1 = new RuleDto();
    rule1.setRepositoryKey("repo");
    rule1.setRuleKey("key1");
    rule1.setId(ruleId1);
    long ruleId2 = 4L;
    RuleDto rule2 = new RuleDto();
    rule2.setRepositoryKey("repo");
    rule2.setRuleKey("key2");
    rule2.setId(ruleId2);
    rule2.setParentId(ruleId1);
    List<RuleDto> rules = ImmutableList.of(rule1, rule2);

    RuleParamDto paramRule2 = new RuleParamDto();
    paramRule2.setName("name");
    paramRule2.setRuleId(ruleId2);
    List<RuleParamDto> params = ImmutableList.of(paramRule2);

    when(ruleDao.selectNonManual()).thenReturn(rules);
    when(ruleDao.selectParameters()).thenReturn(params);
    registry.bulkRegisterRules();
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(2);
  }

  @Test
  public void should_index_and_reindex_single_rule() {
    RuleDto rule = new RuleDto();
    rule.setRepositoryKey("repo");
    rule.setRuleKey("key");
    int id = 3;
    rule.setId((long) id);
    when(ruleDao.selectById((long) id)).thenReturn(rule);
    registry.saveOrUpdate(id);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(1);
    rule.setName("polop");
    registry.saveOrUpdate(id);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(1);
  }

  @Test
  public void should_update_existing_rules_and_forget_deleted_rules() {
    long ruleId1 = 1L;
    RuleDto rule1 = new RuleDto();
    rule1.setRepositoryKey("xoo");
    rule1.setRuleKey("key1");
    rule1.setId(ruleId1);
    long ruleId2 = 2L;
    RuleDto rule2 = new RuleDto();
    rule2.setRepositoryKey("xoo");
    rule2.setRuleKey("key2");
    rule2.setId(ruleId2);
    rule2.setParentId(ruleId1);
    List<RuleDto> rules = ImmutableList.of(rule1, rule2);

    assertThat(esSetup.exists("rules", "rule", "3")).isTrue();
    when(ruleDao.selectNonManual()).thenReturn(rules);
    registry.bulkRegisterRules();

    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "xoo")))
      .hasSize(2)
      .containsOnly((int) ruleId1, (int) ruleId2);
    assertThat(esSetup.exists("rules", "rule", "3")).isFalse();
  }
}
