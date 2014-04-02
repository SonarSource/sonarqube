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

import com.github.tlrx.elasticsearch.test.EsSetup;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.rule.*;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.ESNode;
import org.sonar.test.TestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleRegistryTest {

  EsSetup esSetup;

  ESIndex searchIndex;

  @Mock
  MyBatis myBatis;

  @Mock
  RuleDao ruleDao;

  @Mock
  CharacteristicDao characteristicDao;

  @Mock
  SqlSession session;

  RuleRegistry registry;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    esSetup = new EsSetup(ImmutableSettings.builder().loadFromUrl(ESNode.class.getResource("config/elasticsearch.json"))
      .build());
    esSetup.execute(EsSetup.deleteAll());

    ESNode node = mock(ESNode.class);
    when(node.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    Profiling profiling = new Profiling(settings);
    searchIndex = new ESIndex(node, profiling);
    searchIndex.start();

    registry = new RuleRegistry(searchIndex, myBatis, ruleDao, characteristicDao);
    registry.start();

    esSetup.execute(
      EsSetup.index("rules", "rule", "1").withSource(testFileAsString("shared/rule1.json")),
      EsSetup.index("rules", "rule", "2").withSource(testFileAsString("shared/rule2.json")),
      // rule 3 is removed
      EsSetup.index("rules", "rule", "3").withSource(testFileAsString("shared/removed_rule.json"))
    );
    esSetup.client().admin().cluster().prepareHealth(RuleRegistry.INDEX_RULES).setWaitForGreenStatus().execute().actionGet();
  }

  @After
  public void tearDown() {
    searchIndex.stop();
    esSetup.terminate();
  }

  @Test
  public void register_mapping_at_startup() {
    assertThat(esSetup.exists("rules")).isTrue();
    assertThat(esSetup.client().admin().indices().prepareTypesExists("rules").setTypes("rule").execute().actionGet().isExists()).isTrue();
  }

  @Test
  public void index_new_rules() {
    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(3).setRepositoryKey("repo").setRuleKey("key").setSeverity(Severity.MINOR).setNoteData("noteData").setNoteUserLogin("userLogin")
        .setDefaultSubCharacteristicId(11).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationCoefficient("1h").setDefaultRemediationOffset("15min")
    ));

    when(ruleDao.selectParametersByRuleIds(newArrayList(3), session)).thenReturn(newArrayList(
      new RuleParamDto().setRuleId(3).setName("name")
    ));

    when(ruleDao.selectTagsByRuleIds(newArrayList(3), session)).thenReturn(newArrayList(
      new RuleRuleTagDto().setRuleId(3).setTag("tag1").setType(RuleTagType.SYSTEM),
      new RuleRuleTagDto().setRuleId(3).setTag("tag2").setType(RuleTagType.SYSTEM),
      new RuleRuleTagDto().setRuleId(3).setTag("tag").setType(RuleTagType.ADMIN)));

    when(characteristicDao.selectCharacteristicsByIds(newHashSet(11), session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(11).setKey("MODULARITY").setName("Modularity").setParentId(10)));
    when(characteristicDao.selectCharacteristicsByIds(newHashSet(10), session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(10).setKey("REUSABILITY").setName("Reusability")));

    registry.reindex();

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(3)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_ID)).isEqualTo(3);
    assertThat(ruleDocument.get(RuleDocument.FIELD_REPOSITORY_KEY)).isEqualTo("repo");
    assertThat(ruleDocument.get(RuleDocument.FIELD_KEY)).isEqualTo("key");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SEVERITY)).isEqualTo("MINOR");
    assertThat(ruleDocument.get(RuleDocument.FIELD_NOTE)).isNotNull();

    assertThat((List<String>) ruleDocument.get(RuleDocument.FIELD_PARAMS)).hasSize(1);
    assertThat((List<String>) ruleDocument.get(RuleDocument.FIELD_SYSTEM_TAGS)).hasSize(2);
    assertThat((List<String>) ruleDocument.get(RuleDocument.FIELD_ADMIN_TAGS)).hasSize(1);

    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_ID)).isEqualTo(10);
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_KEY)).isEqualTo("REUSABILITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_ID)).isEqualTo(11);
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_KEY)).isEqualTo("MODULARITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_FUNCTION)).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_COEFFICIENT)).isEqualTo("1h");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_OFFSET)).isEqualTo("15min");
  }

  @Test
  public void reindex_existing_rules() {
    assertThat(esSetup.exists("rules", "rule", "3")).isTrue();

    // Update severity to MAJOR
    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(3).setRepositoryKey("repo").setRuleKey("key").setSeverity(Severity.MAJOR)
    ));

    registry.reindex();

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(3)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_ID)).isEqualTo(3);
    assertThat(ruleDocument.get(RuleDocument.FIELD_REPOSITORY_KEY)).isEqualTo("repo");
    assertThat(ruleDocument.get(RuleDocument.FIELD_KEY)).isEqualTo("key");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SEVERITY)).isEqualTo("MAJOR");
  }

  @Test
  public void index_overridden_characteristic_if_both_default_and_overridden_characteristics_exists_when_indexing_rules() {
    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(10).setRepositoryKey("repo").setRuleKey("key1").setSeverity(Severity.MINOR)
        // default and overridden debt values are set
        .setDefaultSubCharacteristicId(11).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(13).setRemediationFunction("LINEAR_OFFSET").setRemediationCoefficient("1h").setRemediationOffset("15min")
    ));

    // Characteristics
    when(characteristicDao.selectCharacteristicsByIds(newHashSet(10, 12), session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(10).setKey("REUSABILITY").setName("Reusability"),
      new CharacteristicDto().setId(12).setKey("PORTABILITY").setName("Portability")
    ));

    // Sub-characteristics
    when(characteristicDao.selectCharacteristicsByIds(newHashSet(11, 13), session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(11).setKey("MODULARITY").setName("Modularity").setParentId(10),
      new CharacteristicDto().setId(13).setKey("COMPILER").setName("Compiler").setParentId(12)
    ));

    registry.reindex();

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(10)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_ID)).isEqualTo(12);
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_KEY)).isEqualTo("PORTABILITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_ID)).isEqualTo(13);
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_KEY)).isEqualTo("COMPILER");

    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_FUNCTION)).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_COEFFICIENT)).isEqualTo("1h");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_OFFSET)).isEqualTo("15min");
  }

  @Test
  public void index_overridden_function_if_both_default_and_overridden_functions_exists_when_indexing_rules() {
    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(10).setRepositoryKey("repo").setRuleKey("key1").setSeverity(Severity.MINOR)
        // default and overridden debt values are set
        .setDefaultSubCharacteristicId(11).setDefaultRemediationFunction("CONSTANT_ISSUE").setDefaultRemediationOffset("15min")
        .setSubCharacteristicId(11).setRemediationFunction("LINEAR").setRemediationCoefficient("1h")
    ));

    when(characteristicDao.selectCharacteristicsByIds(newHashSet(11), session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(11).setKey("MODULARITY").setName("Modularity").setParentId(10)
    ));
    when(characteristicDao.selectCharacteristicsByIds(newHashSet(10), session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(10).setKey("REUSABILITY").setName("Reusability")
    ));

    registry.reindex();

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(10)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_ID)).isEqualTo(10);
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_KEY)).isEqualTo("REUSABILITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_ID)).isEqualTo(11);
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_KEY)).isEqualTo("MODULARITY");

    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_FUNCTION)).isEqualTo("LINEAR");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_COEFFICIENT)).isEqualTo("1h");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_OFFSET)).isNull();
  }

  @Test
  public void index_one_rule() {
    RuleDto ruleDto = new RuleDto().setId(3).setRepositoryKey("repo").setRuleKey("key").setSeverity(Severity.MINOR).setNoteData("noteData").setNoteUserLogin("userLogin")
      .setDefaultSubCharacteristicId(11).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationCoefficient("1h").setDefaultRemediationOffset("15min");

    when(ruleDao.selectParametersByRuleIds(newArrayList(3), session)).thenReturn(newArrayList(
      new RuleParamDto().setRuleId(3).setName("name")
    ));

    when(ruleDao.selectTagsByRuleIds(newArrayList(3), session)).thenReturn(newArrayList(
      new RuleRuleTagDto().setRuleId(3).setTag("tag1").setType(RuleTagType.SYSTEM),
      new RuleRuleTagDto().setRuleId(3).setTag("tag2").setType(RuleTagType.SYSTEM),
      new RuleRuleTagDto().setRuleId(3).setTag("tag").setType(RuleTagType.ADMIN)));

    when(characteristicDao.selectById(11, session)).thenReturn(new CharacteristicDto().setId(11).setKey("MODULARITY").setName("Modularity").setParentId(10));
    when(characteristicDao.selectById(10, session)).thenReturn(new CharacteristicDto().setId(10).setKey("REUSABILITY").setName("Reusability"));

    registry.reindex(ruleDto);

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(3)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_ID)).isEqualTo(3);
    assertThat(ruleDocument.get(RuleDocument.FIELD_REPOSITORY_KEY)).isEqualTo("repo");
    assertThat(ruleDocument.get(RuleDocument.FIELD_KEY)).isEqualTo("key");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SEVERITY)).isEqualTo("MINOR");
    assertThat(ruleDocument.get(RuleDocument.FIELD_NOTE)).isNotNull();

    assertThat((List<String>) ruleDocument.get(RuleDocument.FIELD_PARAMS)).hasSize(1);
    assertThat((List<String>) ruleDocument.get(RuleDocument.FIELD_SYSTEM_TAGS)).hasSize(2);
    assertThat((List<String>) ruleDocument.get(RuleDocument.FIELD_ADMIN_TAGS)).hasSize(1);

    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_ID)).isEqualTo(10);
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_KEY)).isEqualTo("REUSABILITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_ID)).isEqualTo(11);
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_KEY)).isEqualTo("MODULARITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_FUNCTION)).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_COEFFICIENT)).isEqualTo("1h");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_OFFSET)).isEqualTo("15min");
  }

  @Test
  public void reindex_existing_rule() {
    // Update severity to MAJOR
    RuleDto ruleDto = new RuleDto().setId(3).setRepositoryKey("repo").setRuleKey("key").setSeverity(Severity.MAJOR);

    assertThat(esSetup.exists("rules", "rule", "3")).isTrue();

    registry.reindex(ruleDto);

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(3)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_ID)).isEqualTo(3);
    assertThat(ruleDocument.get(RuleDocument.FIELD_REPOSITORY_KEY)).isEqualTo("repo");
    assertThat(ruleDocument.get(RuleDocument.FIELD_KEY)).isEqualTo("key");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SEVERITY)).isEqualTo("MAJOR");
  }

  @Test
  public void index_overridden_characteristic_if_both_default_and_overridden_characteristics_exists_when_indexing_one_rule() {
    RuleDto ruleDto = new RuleDto().setId(10).setRepositoryKey("repo").setRuleKey("key1").setSeverity(Severity.MINOR)
        // default and overridden debt values are set
        .setDefaultSubCharacteristicId(11).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("2h")
        .setSubCharacteristicId(13).setRemediationFunction("LINEAR_OFFSET").setRemediationCoefficient("1h").setRemediationOffset("15min");

    when(characteristicDao.selectById(12, session)).thenReturn(new CharacteristicDto().setId(12).setKey("REUSABILITY").setName("Reusability"));
    when(characteristicDao.selectById(13, session)).thenReturn(new CharacteristicDto().setId(13).setKey("MODULARITY").setName("Modularity").setParentId(12));

    registry.reindex(ruleDto);

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(10)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_ID)).isEqualTo(12);
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_KEY)).isEqualTo("REUSABILITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_ID)).isEqualTo(13);
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_KEY)).isEqualTo("MODULARITY");

    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_FUNCTION)).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_COEFFICIENT)).isEqualTo("1h");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_OFFSET)).isEqualTo("15min");
  }

  @Test
  public void index_overridden_function_if_both_default_and_overridden_functions_exists_when_indexing_one_rule() {
    RuleDto ruleDto = new RuleDto().setId(10).setRepositoryKey("repo").setRuleKey("key1").setSeverity(Severity.MINOR)
        // default and overridden debt values are set
        .setDefaultSubCharacteristicId(11).setDefaultRemediationFunction("CONSTANT_ISSUE").setDefaultRemediationOffset("15min")
        .setSubCharacteristicId(11).setRemediationFunction("LINEAR").setRemediationCoefficient("1h");

    when(characteristicDao.selectById(10, session)).thenReturn(new CharacteristicDto().setId(10).setKey("REUSABILITY").setName("Reusability"));
    when(characteristicDao.selectById(11, session)).thenReturn(new CharacteristicDto().setId(11).setKey("MODULARITY").setName("Modularity").setParentId(10));

    registry.reindex(ruleDto);

    Map<String, Object> ruleDocument = esSetup.client().prepareGet("rules", "rule", Integer.toString(10)).execute().actionGet().getSourceAsMap();
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_ID)).isEqualTo(10);
    assertThat(ruleDocument.get(RuleDocument.FIELD_CHARACTERISTIC_KEY)).isEqualTo("REUSABILITY");
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_ID)).isEqualTo(11);
    assertThat(ruleDocument.get(RuleDocument.FIELD_SUB_CHARACTERISTIC_KEY)).isEqualTo("MODULARITY");

    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_FUNCTION)).isEqualTo("LINEAR");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_COEFFICIENT)).isEqualTo("1h");
    assertThat(ruleDocument.get(RuleDocument.FIELD_REMEDIATION_OFFSET)).isNull();
  }

  @Test
  public void remove_all_rules_when_ro_rule_registered() {
    String[] ids = registry.reindex(session);
    registry.removeDeletedRules(ids);
    assertThat(registry.findIds(new HashMap<String, String>())).hasSize(0);
  }

  @Test
  public void update_existing_rules_and_forget_deleted_rules() {
    when(ruleDao.selectEnablesAndNonManual(session)).thenReturn(newArrayList(
      new RuleDto().setId(1).setRepositoryKey("xoo").setRuleKey("key1").setSeverity(Severity.MINOR),
      new RuleDto().setId(2).setRepositoryKey("xoo").setRuleKey("key2").setSeverity(Severity.MINOR)
    ));
    assertThat(esSetup.exists("rules", "rule", "3")).isTrue();

    String[] ids = registry.reindex(session);
    registry.removeDeletedRules(ids);

    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "xoo")))
      .hasSize(2)
      .containsOnly(1, 2);
    assertThat(esSetup.exists("rules", "rule", "3")).isFalse();
  }

  @Test
  public void find_rule_by_key() {
    assertThat(registry.findByKey(RuleKey.of("unknown", "RuleWithParameters"))).isNull();
    assertThat(registry.findByKey(RuleKey.of("xoo", "unknown"))).isNull();
    final Rule rule = registry.findByKey(RuleKey.of("xoo", "RuleWithParameters"));
    assertThat(rule).isNotNull();
    assertThat(rule.ruleKey().repository()).isEqualTo("xoo");
    assertThat(rule.ruleKey().rule()).isEqualTo("RuleWithParameters");
    assertThat(rule.params()).hasSize(5);
    assertThat(rule.adminTags()).hasSize(1);
    assertThat(rule.systemTags()).hasSize(2);
  }

  @Test
  public void filter_removed_rules() {
    assertThat(registry.findIds(new HashMap<String, String>())).containsOnly(1, 2);
  }

  @Test
  public void display_disabled_rule() {
    assertThat(registry.findIds(ImmutableMap.of("status", "BETA|REMOVED"))).containsOnly(2, 3);
  }

  @Test
  public void filter_on_name_or_key() throws Exception {
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters"))).containsOnly(1);
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "issue"))).containsOnly(1, 2);
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "issue line"))).containsOnly(2);
  }

  @Test
  public void filter_on_key() throws Exception {
    assertThat(registry.findIds(ImmutableMap.of("key", "OneIssuePerLine"))).containsOnly(2);
  }

  @Test
  public void filter_on_multiple_criteria() {
    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters", "key", "OneIssuePerLine"))).isEmpty();
    assertThat(registry.findIds(ImmutableMap.of("repositoryKey", "polop"))).isEmpty();

    assertThat(registry.findIds(ImmutableMap.of("nameOrKey", "parameters", "repositoryKey", "xoo"))).containsOnly(1);
  }

  @Test
  public void filter_on_multiple_values() {
    assertThat(registry.findIds(ImmutableMap.of("key", "RuleWithParameters|OneIssuePerLine"))).hasSize(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void reject_leading_wildcard() {
    registry.findIds(ImmutableMap.of("nameOrKey", "*ssue"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrap_parse_exceptions() {
    registry.findIds(ImmutableMap.of("nameOrKey", "\"'"));
  }

  @Test
  public void find_rules_by_name() {
    // Removed rule should not appear
    assertThat(registry.find(RuleQuery.builder().searchQuery("Removed rule").build()).results()).isEmpty();

    // Search is case insensitive
    assertThat(registry.find(RuleQuery.builder().searchQuery("one issue per line").build()).results()).hasSize(1);

    // Search is ngram based
    assertThat(registry.find(RuleQuery.builder().searchQuery("with param").build()).results()).hasSize(1);

    // Search works also with key
    assertThat(registry.find(RuleQuery.builder().searchQuery("OneIssuePerLine").build()).results()).hasSize(1);
  }

  @Test
  public void find_rules_by_languages() {
    assertThat(registry.find(RuleQuery.builder().languages(newArrayList("xoo")).build()).results()).hasSize(2);
    assertThat(registry.find(RuleQuery.builder().languages(newArrayList("unknown")).build()).results()).isEmpty();
  }

  @Test
  public void find_rules_by_repositories() {
    assertThat(registry.find(RuleQuery.builder().repositories(newArrayList("xoo")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().repositories(newArrayList("xoo", "xoo2")).build()).results()).hasSize(2);
    assertThat(registry.find(RuleQuery.builder().repositories(newArrayList("unknown")).build()).results()).isEmpty();
  }

  @Test
  public void find_rules_by_severities() {
    assertThat(registry.find(RuleQuery.builder().severities(newArrayList("MAJOR")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().severities(newArrayList("MAJOR", "MINOR")).build()).results()).hasSize(2);
    assertThat(registry.find(RuleQuery.builder().severities(newArrayList("unknown")).build()).results()).isEmpty();
  }

  @Test
  public void find_rules_by_statuses() {
    assertThat(registry.find(RuleQuery.builder().statuses(newArrayList("READY")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().statuses(newArrayList("READY", "BETA")).build()).results()).hasSize(2);
    assertThat(registry.find(RuleQuery.builder().statuses(newArrayList("unknown")).build()).results()).isEmpty();
  }

  @Test
  public void find_rules_by_tags() {
    assertThat(registry.find(RuleQuery.builder().tags(newArrayList("has-params")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().tags(newArrayList("keep-enabled")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().tags(newArrayList("has-params", "keep-enabled")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().tags(newArrayList("unknown")).build()).results()).isEmpty();
  }

  @Test
  public void find_rules_by_characteristics() {
    assertThat(registry.find(RuleQuery.builder().debtCharacteristics(newArrayList("MODULARITY")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().debtCharacteristics(newArrayList("REUSABILITY")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().debtCharacteristics(newArrayList("MODULARITY", "REUSABILITY")).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().debtCharacteristics(newArrayList("unknown")).build()).results()).isEmpty();
  }

  @Test
  public void find_rules_by_has_debt_characteristic() {
    assertThat(registry.find(RuleQuery.builder().hasDebtCharacteristic(null).build()).results()).hasSize(2);
    assertThat(registry.find(RuleQuery.builder().hasDebtCharacteristic(true).build()).results()).hasSize(1);
    assertThat(registry.find(RuleQuery.builder().hasDebtCharacteristic(false).build()).results()).hasSize(1);
  }

  @Test
  public void find_with_no_pagination() {
    assertThat(registry.find(RuleQuery.builder().pageSize(-1).build()).results()).hasSize(2);
  }

  private String testFileAsString(String testFile) throws Exception {
    return IOUtils.toString(TestUtils.getResource(getClass(), testFile).toURI());
  }

}
