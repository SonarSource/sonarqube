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
package org.sonar.server.qualityprofile;

import com.github.tlrx.elasticsearch.test.EsSetup;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.ESNode;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.test.TestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.index.query.FilterBuilders.hasChildFilter;
import static org.elasticsearch.index.query.FilterBuilders.hasParentFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ESActiveRuleTest {

  EsSetup esSetup;

  ESIndex searchIndex;

  ESActiveRule esActiveRule;

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    esSetup = new EsSetup(ImmutableSettings.builder().loadFromUrl(ESNode.class.getResource("config/elasticsearch.json")).build());
    esSetup.execute(EsSetup.deleteAll());

    ESNode node = mock(ESNode.class);
    when(node.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    Profiling profiling = new Profiling(settings);
    searchIndex = new ESIndex(node, profiling);
    searchIndex.start();

    RuleRegistry esRule = new RuleRegistry(searchIndex, null);
    esRule.start();
    esActiveRule = new ESActiveRule(searchIndex, activeRuleDao, myBatis, profiling);
    esActiveRule.start();

    esSetup.execute(
      EsSetup.index("rules", "rule", "1").withSource(testFileAsString("shared/rule1.json")),
      EsSetup.index("rules", "rule", "2").withSource(testFileAsString("shared/rule2.json")),
      EsSetup.index("rules", "rule", "3").withSource(testFileAsString("shared/rule3.json"))
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
  public void bulk_index_active_rules() throws IOException {
    List<ActiveRuleDto> activeRules = newArrayList(new ActiveRuleDto().setId(1).setProfileId(10).setRuleId(1).setSeverity(Severity.MAJOR).setParentId(5));
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    paramsByActiveRule.putAll(1, newArrayList(new ActiveRuleParamDto().setId(1).setActiveRuleId(1).setRulesParameterId(1).setKey("key").setValue("RuleWithParameters")));

    esActiveRule.bulkIndexActiveRules(activeRules, paramsByActiveRule);
    assertThat(esSetup.exists("rules", "active_rule", "1"));

    SearchHit[] parentHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasChildFilter("active_rule", termFilter("profileId", 10))
    ).execute().actionGet().getHits().getHits();
    assertThat(parentHit).hasSize(1);
    assertThat(parentHit[0].getId()).isEqualTo("1");

    SearchHit[] childHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasParentFilter("rule", termFilter("key", "RuleWithParameters"))
    ).execute().actionGet().getHits().getHits();
    assertThat(childHit).hasSize(1);
    assertThat(childHit[0].getId()).isEqualTo("1");
  }

  @Test
  public void bulk_index_active_rules_from_ids() throws IOException {
    when(myBatis.openSession()).thenReturn(session);

    List<Integer> ids = newArrayList(1);
    when(activeRuleDao.selectByIds(ids, session)).thenReturn(
      newArrayList(new ActiveRuleDto().setId(1).setProfileId(10).setRuleId(1).setSeverity(Severity.MAJOR).setParentId(5)));
    when(activeRuleDao.selectParamsByActiveRuleIds(ids, session)).thenReturn(
      newArrayList(new ActiveRuleParamDto().setId(1).setActiveRuleId(1).setRulesParameterId(1).setKey("key").setValue("RuleWithParameters")));

    esActiveRule.bulkIndexActiveRules(ids);
    assertThat(esSetup.exists("rules", "active_rule", "1"));

    SearchHit[] parentHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasChildFilter("active_rule", termFilter("profileId", 10))
    ).execute().actionGet().getHits().getHits();
    assertThat(parentHit).hasSize(1);
    assertThat(parentHit[0].getId()).isEqualTo("1");

    SearchHit[] childHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasParentFilter("rule", termFilter("key", "RuleWithParameters"))
    ).execute().actionGet().getHits().getHits();
    assertThat(childHit).hasSize(1);
    assertThat(childHit[0].getId()).isEqualTo("1");
  }

  @Test
  public void save_active_rule() throws IOException {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(1).setProfileId(10).setRuleId(1).setSeverity(Severity.MAJOR);
    ArrayList<ActiveRuleParamDto> params = newArrayList(new ActiveRuleParamDto().setId(1).setActiveRuleId(1).setRulesParameterId(1).setKey("key").setValue("RuleWithParameters"));

    esActiveRule.save(activeRule, params);
    assertThat(esSetup.exists("rules", "active_rule", "1"));

    SearchHit[] parentHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasChildFilter("active_rule", termFilter("profileId", 10))
    ).execute().actionGet().getHits().getHits();
    assertThat(parentHit).hasSize(1);
    assertThat(parentHit[0].getId()).isEqualTo("1");

    SearchHit[] childHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasParentFilter("rule", termFilter("key", "RuleWithParameters"))
    ).execute().actionGet().getHits().getHits();
    assertThat(childHit).hasSize(1);
    assertThat(childHit[0].getId()).isEqualTo("1");
  }

  @Test
  public void delete_active_rules_from_profile() throws Exception {
    esSetup.client().prepareBulk()
      // On profile 1
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("1").source(testFileAsString("delete_from_profile/active_rule25.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("3").source(testFileAsString("delete_from_profile/active_rule2702.json")))
        // On profile 2
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("2").source(testFileAsString("delete_from_profile/active_rule523.json")))
      .setRefresh(true)
      .execute().actionGet();

    esActiveRule.deleteActiveRulesFromProfile(1);
    assertThat(!esSetup.exists("rules", "active_rule", "25"));
    assertThat(!esSetup.exists("rules", "active_rule", "2702"));
    assertThat(esSetup.exists("rules", "active_rule", "523"));
  }

  @Test
  public void should_delete_from_integer_ids() throws Exception {
    esSetup.client().prepareBulk()
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("1").source(testFileAsString("delete_from_profile/active_rule25.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("3").source(testFileAsString("delete_from_profile/active_rule2702.json")))
      .setRefresh(true)
      .execute().actionGet();
    esActiveRule.deleteActiveRules(ImmutableList.of(25, 2702));
  }

  @Test
  public void should_not_fail_on_empty_delete_list() {
    esActiveRule.deleteActiveRules(ImmutableList.<Integer> of());
  }

  @Test
  public void bulk_index_active_rules_checking_into_db() throws IOException {
    List<ActiveRuleDto> activeRules = newArrayList(new ActiveRuleDto().setId(1).setProfileId(10).setRuleId(1).setSeverity(Severity.MAJOR).setParentId(5)
      .setNoteData("polop").setNoteCreatedAt(new Date()).setNoteUserLogin("godin"));

    SqlSession session = mock(SqlSession.class);
    when(myBatis.openSession()).thenReturn(session);
    when(activeRuleDao.selectAll(session)).thenReturn(activeRules);
    when(activeRuleDao.selectAllParams(session)).thenReturn(Lists.<ActiveRuleParamDto> newArrayList());

    esActiveRule.bulkRegisterActiveRules();
    assertThat(esSetup.exists("rules", "active_rule", "1"));

    SearchHit[] parentHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasChildFilter("active_rule", termFilter("profileId", 10))
    ).execute().actionGet().getHits().getHits();
    assertThat(parentHit).hasSize(1);
    assertThat(parentHit[0].getId()).isEqualTo("1");

    SearchHit[] childHit = esSetup.client().prepareSearch("rules").setPostFilter(
      hasParentFilter("rule", termFilter("key", "RuleWithParameters"))
    ).execute().actionGet().getHits().getHits();
    assertThat(childHit).hasSize(1);
    assertThat(childHit[0].getId()).isEqualTo("1");
  }

  private String testFileAsString(String testFile) throws Exception {
    return IOUtils.toString(TestUtils.getResource(getClass(), testFile).toURI());
  }

}
