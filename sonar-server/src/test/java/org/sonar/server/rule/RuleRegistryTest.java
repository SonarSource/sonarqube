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
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.Rule;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.jpa.session.DatabaseSessionFactory;
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
  private DatabaseSessionFactory sessionFactory;
  private RuleI18nManager ruleI18nManager;
  RuleRegistry registry;

  @Before
  public void setUp() throws Exception {
    sessionFactory = mock(DatabaseSessionFactory.class);
    ruleI18nManager = mock(RuleI18nManager.class);

    esSetup = new EsSetup();
    esSetup.execute(EsSetup.deleteAll());

    SearchNode node = mock(SearchNode.class);
    when(node.client()).thenReturn(esSetup.client());
    searchIndex = new SearchIndex(node);
    searchIndex.start();

    registry = new RuleRegistry(searchIndex, sessionFactory, ruleI18nManager);
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

  @Test
  public void should_index_all_rules() {
    DatabaseSession session = mock(DatabaseSession.class);
    when(sessionFactory.getSession()).thenReturn(session);
    Rule rule1 = Rule.create("repo", "key1");
    rule1.setId(3);
    Rule rule2 = Rule.create("repo", "key2");
    rule2.createParameter("param");
    rule2.setId(4);
    rule2.setParent(rule1);
    List<Rule> rules = ImmutableList.of(rule1, rule2);
    when(session.getResults(Rule.class)).thenReturn(rules);
    registry.bulkRegisterRules();
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(2);
  }

  @Test
  public void should_index_and_reindex_single_rule() {
    DatabaseSession session = mock(DatabaseSession.class);
    when(sessionFactory.getSession()).thenReturn(session);
    Rule rule = Rule.create("repo", "key");
    int id = 3;
    rule.setId(id);
    when(session.getEntity(Rule.class, id)).thenReturn(rule);
    registry.saveOrUpdate(id);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(1);
    rule.setName("polop");
    registry.saveOrUpdate(id);
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "repo"))).hasSize(1);
  }
}
