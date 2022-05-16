/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.rule;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.rule.RuleDto.Scope;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.rule.RuleStatus.REMOVED;
import static org.sonar.db.rule.RuleTesting.newRuleMetadata;

public class RuleDaoTest {
  private static final String UNKNOWN_RULE_UUID = "unknown-uuid";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final RuleDao underTest = db.getDbClient().ruleDao();

  @Test
  public void selectByKey() {
    RuleDto ruleDto = db.rules().insert();
    RuleMetadataDto metadata = newRuleMetadata(ruleDto);
    db.rules().insertRule(ruleDto, metadata);

    assertThat(underTest.selectByKey(db.getSession(), RuleKey.of("foo", "bar")))
      .isEmpty();
    RuleDto rule = underTest.selectByKey(db.getSession(), ruleDto.getKey()).get();
    assertEquals(rule, ruleDto);
    verifyMetadata(rule.getMetadata(), metadata);
  }

  @Test
  public void selectByKey_return_rule() {
    RuleDto ruleDto = db.rules().insert();

    assertThat(underTest.selectByKey(db.getSession(), ruleDto.getKey())).isNotEmpty();
  }

  @Test
  public void selectByKey_returns_metadata() {
    RuleDto ruleDto = db.rules().insert();
    RuleMetadataDto ruleMetadata = newRuleMetadata(ruleDto);
    db.rules().insertRule(ruleDto, ruleMetadata);

    RuleDto rule = underTest.selectByKey(db.getSession(), ruleDto.getKey()).get();
    verifyMetadata(rule.getMetadata(), ruleMetadata);
  }

  @Test
  public void selectDefinitionByKey() {
    RuleDto rule = db.rules().insert();

    assertThat(underTest.selectByKey(db.getSession(), RuleKey.of("NOT", "FOUND"))).isEmpty();

    Optional<RuleDto> reloaded = underTest.selectByKey(db.getSession(), rule.getKey());
    assertThat(reloaded).isPresent();
  }

  @Test
  public void selectMetadataByKey() {
    RuleDto rule1 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(rule1);

    assertThat(underTest.selectByKeys(db.getSession(), singletonList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    Optional<RuleMetadataDto> rulesMetadata = underTest.selectMetadataByKey(db.getSession(), rule1.getKey());
    assertThat(rulesMetadata).isPresent();
    assertThat(rulesMetadata.get().getRuleUuid()).isEqualTo(rule1.getUuid());
  }

  @Test
  public void selectByUuid() {
    RuleDto ruleDto = db.rules().insert();
    RuleMetadataDto metadata = newRuleMetadata(ruleDto);
    RuleDto expected = db.rules().insertRule(ruleDto, metadata);

    assertThat(underTest.selectByUuid(expected.getUuid() + 500, db.getSession())).isEmpty();
    RuleDto rule = underTest.selectByUuid(expected.getUuid(), db.getSession()).get();
    assertEquals(rule, ruleDto);
    verifyMetadata(rule.getMetadata(), metadata);
  }

  @Test
  public void selectByUuidWithDifferentsValuesOfBooleans() {
    for (int i = 0; i < 3; i++) {
      int indexBoolean = i;
      RuleDto ruleDto = db.rules().insert((ruleDto1 -> {
        ruleDto1.setIsTemplate(indexBoolean == 0);
        ruleDto1.setIsExternal(indexBoolean == 1);
        ruleDto1.setIsAdHoc(indexBoolean == 2);
      }));
      RuleMetadataDto metadata = newRuleMetadata(ruleDto);
      RuleDto expected = db.rules().insertRule(ruleDto, metadata);

      assertThat(underTest.selectByUuid(expected.getUuid() + 500, db.getSession())).isEmpty();
      RuleDto rule = underTest.selectByUuid(expected.getUuid(), db.getSession()).get();
      assertEquals(rule, ruleDto);
      verifyMetadata(rule.getMetadata(), metadata);
    }
  }

  @Test
  public void selectDefinitionByUuid() {
    RuleDto rule = db.rules().insert();

    assertThat(underTest.selectByUuid(UNKNOWN_RULE_UUID, db.getSession())).isEmpty();
    Optional<RuleDto> ruleDtoOptional = underTest.selectByUuid(rule.getUuid(), db.getSession());
    assertThat(ruleDtoOptional).isPresent();
  }

  @Test
  public void selectByUuids() {
    RuleDto rule1 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(rule1);
    RuleDto rule2 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(rule2);
    RuleDto removedRule = db.rules().insert(r -> r.setStatus(REMOVED));
    db.rules().insertOrUpdateMetadata(removedRule);

    assertThat(underTest.selectByUuids(db.getSession(), singletonList(rule1.getUuid()))).hasSize(1);
    assertThat(underTest.selectByUuids(db.getSession(), asList(rule1.getUuid(), rule2.getUuid()))).hasSize(2);
    assertThat(underTest.selectByUuids(db.getSession(), asList(rule1.getUuid(), rule2.getUuid(), UNKNOWN_RULE_UUID))).hasSize(2);
    assertThat(underTest.selectByUuids(db.getSession(), asList(rule1.getUuid(), rule2.getUuid(), removedRule.getUuid()))).hasSize(3);
    assertThat(underTest.selectByUuids(db.getSession(), singletonList(UNKNOWN_RULE_UUID))).isEmpty();
  }

  @Test
  public void selectDefinitionByUuids() {
    RuleDto rule1 = db.rules().insert();
    RuleDto rule2 = db.rules().insert();

    assertThat(underTest.selectByUuids(db.getSession(), singletonList(rule1.getUuid()))).hasSize(1);
    assertThat(underTest.selectByUuids(db.getSession(), asList(rule1.getUuid(), rule2.getUuid()))).hasSize(2);
    assertThat(underTest.selectByUuids(db.getSession(), asList(rule1.getUuid(), rule2.getUuid(), UNKNOWN_RULE_UUID))).hasSize(2);
    assertThat(underTest.selectByUuids(db.getSession(), singletonList(UNKNOWN_RULE_UUID))).isEmpty();
  }

  @Test
  public void selectOrFailByKey() {
    RuleDto rule1 = db.rules().insert();
    db.rules().insert();

    RuleDto rule = underTest.selectOrFailByKey(db.getSession(), rule1.getKey());
    assertThat(rule.getUuid()).isEqualTo(rule1.getUuid());
  }

  @Test
  public void selectOrFailByKey_fails_if_rule_not_found() {
    assertThatThrownBy(() -> underTest.selectOrFailByKey(db.getSession(), RuleKey.of("NOT", "FOUND")))
      .isInstanceOf(RowNotFoundException.class)
      .hasMessage("Rule with key 'NOT:FOUND' does not exist");
  }

  @Test
  public void selectOrFailDefinitionByKey_fails_if_rule_not_found() {
    assertThatThrownBy(() -> underTest.selectOrFailByKey(db.getSession(), RuleKey.of("NOT", "FOUND")))
      .isInstanceOf(RowNotFoundException.class)
      .hasMessage("Rule with key 'NOT:FOUND' does not exist");
  }

  @Test
  public void selectByKeys() {
    RuleDto rule1 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(rule1);
    RuleDto rule2 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(rule2);

    assertThat(underTest.selectByKeys(db.getSession(), Collections.emptyList())).isEmpty();
    assertThat(underTest.selectByKeys(db.getSession(), singletonList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDto> rules = underTest.selectByKeys(db.getSession(), asList(rule1.getKey(), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getUuid()).isEqualTo(rule1.getUuid());
  }

  @Test
  public void selectDefinitionByKeys() {
    RuleDto rule = db.rules().insert();

    assertThat(underTest.selectByKeys(db.getSession(), Collections.emptyList())).isEmpty();
    assertThat(underTest.selectByKeys(db.getSession(), singletonList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDto> rules = underTest.selectByKeys(db.getSession(), asList(rule.getKey(), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getUuid()).isEqualTo(rule.getUuid());
  }

  @Test
  public void selectAll() {
    RuleDto rule1 = db.rules().insertRule();
    RuleDto rule2 = db.rules().insertRule();
    RuleDto rule3 = db.rules().insertRule();

    assertThat(underTest.selectAll(db.getSession()))
      .extracting(RuleDto::getUuid)
      .containsOnly(rule1.getUuid(), rule2.getUuid(), rule3.getUuid());
  }

  @Test
  public void selectAll_returns_metadata() {
    RuleDto ruleDto = db.rules().insert();
    RuleMetadataDto expected = newRuleMetadata(ruleDto);
    db.rules().insertRule(ruleDto, expected);

    List<RuleDto> rules = underTest.selectAll(db.getSession());
    assertThat(rules).hasSize(1);

    verifyMetadata(rules.iterator().next().getMetadata(), expected);
  }

  private void assertEquals(RuleDto actual, RuleDto expected) {
    assertThat(actual.getUuid()).isEqualTo(expected.getUuid());
    assertThat(actual.getRepositoryKey()).isEqualTo(expected.getRepositoryKey());
    assertThat(actual.getRuleKey()).isEqualTo(expected.getRuleKey());
    assertThat(actual.getKey()).isEqualTo(expected.getKey());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getConfigKey()).isEqualTo(expected.getConfigKey());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getSeverityString()).isEqualTo(expected.getSeverityString());
    assertThat(actual.isExternal()).isEqualTo(expected.isExternal());
    assertThat(actual.isTemplate()).isEqualTo(expected.isTemplate());
    assertThat(actual.isCustomRule()).isEqualTo(expected.isCustomRule());
    assertThat(actual.getLanguage()).isEqualTo(expected.getLanguage());
    assertThat(actual.getTemplateUuid()).isEqualTo(expected.getTemplateUuid());
    assertThat(actual.getDefRemediationFunction()).isEqualTo(expected.getDefRemediationFunction());
    assertThat(actual.getDefRemediationGapMultiplier()).isEqualTo(expected.getDefRemediationGapMultiplier());
    assertThat(actual.getDefRemediationBaseEffort()).isEqualTo(expected.getDefRemediationBaseEffort());
    assertThat(actual.getGapDescription()).isEqualTo(expected.getGapDescription());
    assertThat(actual.getSystemTags()).isEqualTo(expected.getSystemTags());
    assertThat(actual.getSecurityStandards()).isEqualTo(expected.getSecurityStandards());
    assertThat(actual.getType()).isEqualTo(expected.getType());
    assertThat(actual.getDescriptionFormat()).isEqualTo(expected.getDescriptionFormat());
    assertThat(actual.getRuleDescriptionSectionDtos()).usingRecursiveFieldByFieldElementComparator()
      .isEqualTo(expected.getRuleDescriptionSectionDtos());
  }

  private static void verifyMetadata(RuleMetadataDto metadata, RuleMetadataDto expected) {
    assertThat(metadata.getRemediationBaseEffort()).isEqualTo(expected.getRemediationBaseEffort());
    assertThat(metadata.getRemediationFunction()).isEqualTo(expected.getRemediationFunction());
    assertThat(metadata.getRemediationGapMultiplier()).isEqualTo(expected.getRemediationGapMultiplier());
    assertThat(metadata.getTags()).isEqualTo(expected.getTags());
    assertThat(metadata.getNoteData()).isEqualTo(expected.getNoteData());
    assertThat(metadata.getNoteCreatedAt()).isEqualTo(expected.getNoteCreatedAt());
    assertThat(metadata.getNoteUpdatedAt()).isEqualTo(expected.getNoteUpdatedAt());
    assertThat(metadata.getAdHocName()).isEqualTo(expected.getAdHocName());
    assertThat(metadata.getAdHocDescription()).isEqualTo(expected.getAdHocDescription());
    assertThat(metadata.getAdHocSeverity()).isEqualTo(expected.getAdHocSeverity());
    assertThat(metadata.getAdHocType()).isEqualTo(expected.getAdHocType());
  }

  @Test
  public void selectAllDefinitions() {
    RuleDto rule1 = db.rules().insert();
    RuleDto rule2 = db.rules().insert();
    RuleDto removedRule = db.rules().insert(r -> r.setStatus(REMOVED));

    List<RuleDto> ruleDtos = underTest.selectAll(db.getSession());

    assertThat(ruleDtos).extracting(RuleDto::getUuid).containsOnly(rule1.getUuid(), rule2.getUuid(), removedRule.getUuid());
  }

  @Test
  public void selectEnabled_with_ResultHandler() {
    RuleDto rule = db.rules().insert();
    db.rules().insert(r -> r.setStatus(REMOVED));

    List<RuleDto> rules = underTest.selectEnabled(db.getSession());

    assertThat(rules.size()).isOne();
    RuleDto ruleDto = rules.get(0);
    assertThat(ruleDto.getUuid()).isEqualTo(rule.getUuid());
  }

  @Test
  public void selectByTypeAndLanguages() {
    RuleDto rule1 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S001"))
        .setConfigKey("S1")
        .setType(RuleType.VULNERABILITY)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule1);

    RuleDto rule2 = db.rules().insert(
      r -> r.setKey(RuleKey.of("js", "S002"))
        .setType(RuleType.SECURITY_HOTSPOT)
        .setLanguage("js"));
    db.rules().insertOrUpdateMetadata(rule2);

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.VULNERABILITY.getDbConstant()), singletonList("java")))
      .extracting(RuleDto::getUuid, RuleDto::getLanguage, RuleDto::getType)
      .containsExactly(tuple(rule1.getUuid(), "java", RuleType.VULNERABILITY.getDbConstant()));

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.SECURITY_HOTSPOT.getDbConstant()), singletonList("js")))
      .extracting(RuleDto::getUuid, RuleDto::getLanguage, RuleDto::getType)
      .containsExactly(tuple(rule2.getUuid(), "js", RuleType.SECURITY_HOTSPOT.getDbConstant()));

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.SECURITY_HOTSPOT.getDbConstant()), singletonList("java"))).isEmpty();
    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.VULNERABILITY.getDbConstant()), singletonList("js"))).isEmpty();
  }

  @Test
  public void selectByTypeAndLanguages_return_nothing_when_no_rule_on_languages() {
    RuleDto rule1 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S001"))
        .setConfigKey("S1")
        .setType(RuleType.VULNERABILITY)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule1);

    RuleDto rule2 = db.rules().insert(
      r -> r.setKey(RuleKey.of("js", "S002"))
        .setType(RuleType.VULNERABILITY)
        .setLanguage("js"));
    db.rules().insertOrUpdateMetadata(rule2);

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.VULNERABILITY.getDbConstant()), singletonList("cpp")))
      .isEmpty();
  }

  @Test
  public void selectByTypeAndLanguages_return_nothing_when_no_rule_with_type() {
    RuleDto rule1 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S001"))
        .setConfigKey("S1")
        .setType(RuleType.VULNERABILITY)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule1);

    RuleDto rule2 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S002"))
        .setType(RuleType.SECURITY_HOTSPOT)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule2);

    RuleDto rule3 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S003"))
        .setType(RuleType.CODE_SMELL)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule3);

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.BUG.getDbConstant()), singletonList("java")))
      .isEmpty();
  }

  @Test
  public void selectByTypeAndLanguages_ignores_external_rules() {
    RuleDto rule1 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S001"))
        .setConfigKey("S1")
        .setType(RuleType.VULNERABILITY)
        .setIsExternal(true)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule1);

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.VULNERABILITY.getDbConstant()), singletonList("java")))
      .extracting(RuleDto::getUuid, RuleDto::getLanguage, RuleDto::getType)
      .isEmpty();
  }

  @Test
  public void selectByTypeAndLanguages_ignores_template_rules() {
    RuleDto rule1 = db.rules().insert(
      r -> r.setKey(RuleKey.of("java", "S001"))
        .setConfigKey("S1")
        .setType(RuleType.VULNERABILITY)
        .setIsTemplate(true)
        .setLanguage("java"));
    db.rules().insertOrUpdateMetadata(rule1);

    assertThat(underTest.selectByTypeAndLanguages(db.getSession(), singletonList(RuleType.VULNERABILITY.getDbConstant()), singletonList("java")))
      .extracting(RuleDto::getUuid, RuleDto::getLanguage, RuleDto::getType)
      .isEmpty();
  }

  @Test
  public void select_by_query() {
    RuleDto rule1 = db.rules().insert(r -> r.setKey(RuleKey.of("java", "S001")).setConfigKey("S1"));
    db.rules().insertOrUpdateMetadata(rule1);
    RuleDto rule2 = db.rules().insert(r -> r.setKey(RuleKey.of("java", "S002")));
    db.rules().insertOrUpdateMetadata(rule2);
    RuleDto removedRule = db.rules().insert(r -> r.setStatus(REMOVED));

    assertThat(underTest.selectByQuery(db.getSession(), RuleQuery.create())).hasSize(2);
    assertThat(underTest.selectByQuery(db.getSession(), RuleQuery.create().withKey("S001"))).hasSize(1);
    assertThat(underTest.selectByQuery(db.getSession(), RuleQuery.create().withConfigKey("S1"))).hasSize(1);
    assertThat(underTest.selectByQuery(db.getSession(), RuleQuery.create().withRepositoryKey("java"))).hasSize(2);
    assertThat(underTest.selectByQuery(db.getSession(),
      RuleQuery.create().withKey("S001").withConfigKey("S1").withRepositoryKey("java"))).hasSize(1);
  }

  @Test
  public void insert() {
    RuleDescriptionSectionDto sectionDto = createDefaultRuleDescriptionSection();
    RuleDto newRule = new RuleDto()
      .setUuid("rule-uuid")
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .addRuleDescriptionSectionDto(sectionDto)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setLanguage("dart")
      .setTemplateUuid("uuid-3")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setSecurityStandards(newHashSet("owaspTop10:a1", "cwe:123"))
      .setType(RuleType.BUG)
      .setScope(Scope.ALL)
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(2_000_000_000_000L);
    underTest.insert(db.getSession(), newRule);
    db.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getUuid()).isNotNull();
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isZero();
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.isExternal()).isTrue();
    assertThat(ruleDto.isAdHoc()).isTrue();
    assertThat(ruleDto.getTemplateUuid()).isEqualTo("uuid-3");
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getSecurityStandards()).containsOnly("owaspTop10:a1", "cwe:123");
    assertThat(ruleDto.getScope()).isEqualTo(Scope.ALL);
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2_000_000_000_000L);
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getRuleDescriptionSectionDtos()).usingRecursiveFieldByFieldElementComparator()
      .containsOnly(sectionDto);
  }

  @Test
  public void update_RuleDefinitionDto() {
    RuleDto rule = db.rules().insert();
    RuleDescriptionSectionDto sectionDto = createDefaultRuleDescriptionSection();
    RuleDto ruleToUpdate = new RuleDto()
      .setUuid(rule.getUuid())
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .addRuleDescriptionSectionDto(sectionDto)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setLanguage("dart")
      .setTemplateUuid("uuid-3")
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setSecurityStandards(newHashSet("owaspTop10:a1", "cwe:123"))
      .setScope(Scope.ALL)
      .setType(RuleType.BUG)
      .setUpdatedAt(2_000_000_000_000L);

    underTest.update(db.getSession(), ruleToUpdate);
    db.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isZero();
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.isExternal()).isTrue();
    assertThat(ruleDto.isAdHoc()).isTrue();
    assertThat(ruleDto.getTemplateUuid()).isEqualTo("uuid-3");
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getSecurityStandards()).containsOnly("owaspTop10:a1", "cwe:123");
    assertThat(ruleDto.getScope()).isEqualTo(Scope.ALL);
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(rule.getCreatedAt());
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2_000_000_000_000L);
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);

    assertThat(ruleDto.getRuleDescriptionSectionDtos()).usingRecursiveFieldByFieldElementComparator()
      .containsOnly(sectionDto);
  }

  @Test
  public void update_rule_sections_add_new_section() {
    RuleDto rule = db.rules().insert();
    RuleDescriptionSectionDto existingSection = rule.getRuleDescriptionSectionDtos().iterator().next();
    RuleDescriptionSectionDto newSection = RuleDescriptionSectionDto.builder()
      .uuid(randomAlphanumeric(20))
      .key("new_key")
      .content(randomAlphanumeric(1000))
      .build();

    rule.addRuleDescriptionSectionDto(newSection);

    underTest.update(db.getSession(), rule);
    db.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()));

    assertThat(ruleDto.getRuleDescriptionSectionDtos())
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(newSection, existingSection);
  }

  @Test
  public void update_rule_sections_replaces_section() {
    RuleDto rule = db.rules().insert();
    RuleDescriptionSectionDto existingSection = rule.getRuleDescriptionSectionDtos().iterator().next();
    RuleDescriptionSectionDto replacingSection = RuleDescriptionSectionDto.builder()
      .uuid(randomAlphanumeric(20))
      .key(existingSection.getKey())
      .content(randomAlphanumeric(1000))
      .build();

    rule.addOrReplaceRuleDescriptionSectionDto(replacingSection);

    underTest.update(db.getSession(), rule);
    db.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()));

    assertThat(ruleDto.getRuleDescriptionSectionDtos())
      .usingRecursiveFieldByFieldElementComparator()
      .containsOnly(replacingSection);
  }

  @Test
  public void update_RuleMetadataDto_inserts_row_in_RULE_METADATA_if_not_exists_yet() {
    RuleDto rule = db.rules().insert();

    long createdAtBeforeUpdate = rule.getCreatedAt();

    rule.setNoteData("My note");
    rule.setNoteUserUuid("admin");
    rule.setNoteCreatedAt(DateUtils.parseDate("2013-12-19").getTime());
    rule.setNoteUpdatedAt(DateUtils.parseDate("2013-12-20").getTime());
    rule.setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString());
    rule.setRemediationGapMultiplier("1h");
    rule.setRemediationBaseEffort("5min");
    rule.setTags(newHashSet("tag1", "tag2"));
    rule.setAdHocName("ad hoc name");
    rule.setAdHocDescription("ad hoc desc");
    rule.setAdHocSeverity(Severity.BLOCKER);
    rule.setAdHocType(RuleType.CODE_SMELL);
    rule.setCreatedAt(3_500_000_000_000L);
    rule.setUpdatedAt(4_000_000_000_000L);

    underTest.update(db.getSession(), rule);
    db.getSession().commit();

    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), rule.getKey());
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserUuid()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getAdHocName()).isEqualTo("ad hoc name");
    assertThat(ruleDto.getAdHocDescription()).isEqualTo("ad hoc desc");
    assertThat(ruleDto.getAdHocSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(ruleDto.getAdHocType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(ruleDto.getSecurityStandards()).isEmpty();
    assertThat(ruleDto.getCreatedAt()).isEqualTo(createdAtBeforeUpdate);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(4_000_000_000_000L);
    // Info from rule definition
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo(rule.getDefRemediationFunction());
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo(rule.getDefRemediationGapMultiplier());
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo(rule.getDefRemediationBaseEffort());
    assertThat(ruleDto.getGapDescription()).isEqualTo(rule.getGapDescription());
    assertThat(ruleDto.getSystemTags()).containsAll(rule.getSystemTags());
    assertThat(ruleDto.getType()).isEqualTo(rule.getType());
  }

  @Test
  public void update_RuleMetadataDto_updates_row_in_RULE_METADATA_if_already_exists() {
    RuleDto rule = db.rules().insert();
    rule.setAdHocDescription("ad-hoc-desc");
    rule.setCreatedAt(3_500_000_000_000L);
    rule.setUpdatedAt(4_000_000_000_000L);

    RuleDto ruleDtoBeforeUpdate = underTest.selectOrFailByKey(db.getSession(), rule.getKey());

    underTest.update(db.getSession(), rule);
    db.commit();

    assertThat(db.countRowsOfTable("RULES_METADATA")).isOne();
    RuleDto ruleDtoAfterUpdate = underTest.selectOrFailByKey(db.getSession(), rule.getKey());
    assertThat(ruleDtoAfterUpdate.getNoteData()).isNull();
    assertThat(ruleDtoAfterUpdate.getNoteUserUuid()).isNull();
    assertThat(ruleDtoAfterUpdate.getNoteCreatedAt()).isNull();
    assertThat(ruleDtoAfterUpdate.getNoteUpdatedAt()).isNull();
    assertThat(ruleDtoAfterUpdate.getRemediationFunction()).isNull();
    assertThat(ruleDtoAfterUpdate.getRemediationGapMultiplier()).isNull();
    assertThat(ruleDtoAfterUpdate.getRemediationBaseEffort()).isNull();
    assertThat(ruleDtoAfterUpdate.getTags()).isEmpty();
    assertThat(ruleDtoAfterUpdate.getAdHocName()).isNull();
    assertThat(ruleDtoAfterUpdate.getAdHocDescription()).isEqualTo("ad-hoc-desc");
    assertThat(ruleDtoAfterUpdate.getAdHocSeverity()).isNull();
    assertThat(ruleDtoAfterUpdate.getAdHocType()).isNull();
    assertThat(ruleDtoAfterUpdate.getSecurityStandards()).isEmpty();
    assertThat(ruleDtoAfterUpdate.getCreatedAt()).isEqualTo(ruleDtoBeforeUpdate.getCreatedAt());
    assertThat(ruleDtoAfterUpdate.getUpdatedAt()).isEqualTo(4_000_000_000_000L);

    rule.setNoteData("My note");
    rule.setNoteUserUuid("admin");
    rule.setNoteCreatedAt(DateUtils.parseDate("2013-12-19").getTime());
    rule.setNoteUpdatedAt(DateUtils.parseDate("2013-12-20").getTime());
    rule.setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString());
    rule.setRemediationGapMultiplier("1h");
    rule.setRemediationBaseEffort("5min");
    rule.setTags(newHashSet("tag1", "tag2"));
    rule.setAdHocName("ad hoc name");
    rule.setAdHocDescription("ad hoc desc");
    rule.setAdHocSeverity(Severity.BLOCKER);
    rule.setAdHocType(RuleType.CODE_SMELL);
    rule.setCreatedAt(6_500_000_000_000L);
    rule.setUpdatedAt(7_000_000_000_000L);
    underTest.update(db.getSession(), rule);
    db.commit();

    ruleDtoAfterUpdate = underTest.selectOrFailByKey(db.getSession(), rule.getKey());
    assertThat(ruleDtoAfterUpdate.getNoteData()).isEqualTo("My note");
    assertThat(ruleDtoAfterUpdate.getNoteUserUuid()).isEqualTo("admin");
    assertThat(ruleDtoAfterUpdate.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDtoAfterUpdate.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDtoAfterUpdate.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDtoAfterUpdate.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDtoAfterUpdate.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDtoAfterUpdate.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDtoAfterUpdate.getAdHocName()).isEqualTo("ad hoc name");
    assertThat(ruleDtoAfterUpdate.getAdHocDescription()).isEqualTo("ad hoc desc");
    assertThat(ruleDtoAfterUpdate.getAdHocSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(ruleDtoAfterUpdate.getAdHocType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(ruleDtoAfterUpdate.getSecurityStandards()).isEmpty();
    assertThat(ruleDtoAfterUpdate.getCreatedAt()).isEqualTo(ruleDtoBeforeUpdate.getCreatedAt());
    assertThat(ruleDtoAfterUpdate.getUpdatedAt()).isEqualTo(7_000_000_000_000L);
  }

  @Test
  public void select_all_rule_params() {
    RuleDto rule1 = db.rules().insert();
    RuleParamDto ruleParam1 = db.rules().insertRuleParam(rule1);
    RuleParamDto ruleParam12 = db.rules().insertRuleParam(rule1);

    RuleDto rule2 = db.rules().insert();
    RuleParamDto ruleParam2 = db.rules().insertRuleParam(rule2);

    RuleDto rule3 = db.rules().insert();
    RuleParamDto ruleParam3 = db.rules().insertRuleParam(rule3);

    List<RuleParamDto> ruleDtos = underTest.selectAllRuleParams(db.getSession());

    assertThat(ruleDtos.size()).isEqualTo(4);
    assertThat(ruleDtos).extracting(RuleParamDto::getUuid)
      .containsExactlyInAnyOrder(ruleParam1.getUuid(), ruleParam12.getUuid(),
        ruleParam2.getUuid(), ruleParam3.getUuid());
  }

  @Test
  public void select_parameters_by_rule_key() {
    RuleDto rule = db.rules().insert();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule);

    List<RuleParamDto> ruleDtos = underTest.selectRuleParamsByRuleKey(db.getSession(), rule.getKey());

    assertThat(ruleDtos.size()).isOne();
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getUuid()).isEqualTo(ruleParam.getUuid());
    assertThat(ruleDto.getName()).isEqualTo(ruleParam.getName());
    assertThat(ruleDto.getDescription()).isEqualTo(ruleParam.getDescription());
    assertThat(ruleDto.getType()).isEqualTo(ruleParam.getType());
    assertThat(ruleDto.getRuleUuid()).isEqualTo(rule.getUuid());
  }

  @Test
  public void select_parameters_by_rule_keys() {
    RuleDto rule1 = db.rules().insert();
    db.rules().insertRuleParam(rule1);
    RuleDto rule2 = db.rules().insert();
    db.rules().insertRuleParam(rule2);

    assertThat(underTest.selectRuleParamsByRuleKeys(db.getSession(),
      Arrays.asList(rule1.getKey(), rule2.getKey()))).hasSize(2);

    assertThat(underTest.selectRuleParamsByRuleKeys(db.getSession(),
      singletonList(RuleKey.of("unknown", "Unknown")))).isEmpty();
  }

  @Test
  public void insert_parameter() {
    RuleDto ruleDefinitionDto = db.rules().insert();

    RuleParamDto orig = RuleParamDto.createFor(ruleDefinitionDto)
      .setName("max")
      .setType("INTEGER")
      .setDefaultValue("30")
      .setDescription("My Parameter");

    underTest.insertRuleParam(db.getSession(), ruleDefinitionDto, orig);

    List<RuleParamDto> ruleParamDtos = underTest.selectRuleParamsByRuleKey(db.getSession(), ruleDefinitionDto.getKey());
    assertThat(ruleParamDtos).hasSize(1);

    RuleParamDto loaded = ruleParamDtos.get(0);
    assertThat(loaded.getRuleUuid()).isEqualTo(orig.getRuleUuid());
    assertThat(loaded.getName()).isEqualTo(orig.getName());
    assertThat(loaded.getType()).isEqualTo(orig.getType());
    assertThat(loaded.getDefaultValue()).isEqualTo(orig.getDefaultValue());
    assertThat(loaded.getDescription()).isEqualTo(orig.getDescription());
  }

  @Test
  public void should_fail_to_insert_duplicate_parameter() {
    RuleDto ruleDefinitionDto = db.rules().insert();

    RuleParamDto param = RuleParamDto.createFor(ruleDefinitionDto)
      .setName("max")
      .setType("INTEGER")
      .setDefaultValue("30")
      .setDescription("My Parameter");

    underTest.insertRuleParam(db.getSession(), ruleDefinitionDto, param);

    assertThatThrownBy(() -> underTest.insertRuleParam(db.getSession(), ruleDefinitionDto, param))
      .isInstanceOf(PersistenceException.class);
  }

  @Test
  public void update_parameter() {
    RuleDto rule = db.rules().insert();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule);

    List<RuleParamDto> params = underTest.selectRuleParamsByRuleKey(db.getSession(), rule.getKey());
    assertThat(params).hasSize(1);
    RuleParamDto param = new RuleParamDto()
      .setUuid(ruleParam.getUuid())
      .setRuleUuid(rule.getUuid())
      // Name will not be updated
      .setName("format")
      .setType("STRING")
      .setDefaultValue("^[a-z]+(\\.[a-z][a-z0-9]*)*$")
      .setDescription("Regular expression used to check the package names against.");

    underTest.updateRuleParam(db.getSession(), rule, param);

    assertThat(underTest.selectRuleParamsByRuleKey(db.getSession(), rule.getKey()))
      .extracting(RuleParamDto::getName, RuleParamDto::getType, RuleParamDto::getDefaultValue, RuleParamDto::getDescription)
      .containsExactlyInAnyOrder(tuple(ruleParam.getName(), "STRING", "^[a-z]+(\\.[a-z][a-z0-9]*)*$", "Regular expression used to check the package names against."));
  }

  @Test
  public void delete_parameter() {
    RuleDto rule = db.rules().insert();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule);
    assertThat(underTest.selectRuleParamsByRuleKey(db.getSession(), rule.getKey())).hasSize(1);

    underTest.deleteRuleParam(db.getSession(), ruleParam.getUuid());

    assertThat(underTest.selectRuleParamsByRuleKey(db.getSession(), rule.getKey())).isEmpty();
  }

  @Test
  public void scrollIndexingRules_on_empty_table() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();

    underTest.selectIndexingRules(db.getSession(), accumulator);

    assertThat(accumulator.list).isEmpty();
  }

  @Test
  public void scrollIndexingRules() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDescriptionSectionDto ruleDescriptionSectionDto = RuleDescriptionSectionDto.builder()
      .key("DESC")
      .uuid("uuid")
      .content("my description")
      .build();
    RuleDto r1 = db.rules().insert(r -> {
      r.addRuleDescriptionSectionDto(ruleDescriptionSectionDto);
    });
    RuleDto r2 = db.rules().insert(r -> r.setIsExternal(true));

    underTest.selectIndexingRules(db.getSession(), accumulator);

    RuleForIndexingDto firstRule = findRuleForIndexingWithUuid(accumulator, r1.getUuid());
    RuleForIndexingDto secondRule = findRuleForIndexingWithUuid(accumulator, r2.getUuid());

    assertThat(Arrays.asList(firstRule, secondRule))
      .extracting(RuleForIndexingDto::getUuid, RuleForIndexingDto::getRuleKey)
      .containsExactlyInAnyOrder(tuple(r1.getUuid(), r1.getKey()), tuple(r2.getUuid(), r2.getKey()));
    Iterator<RuleForIndexingDto> it = accumulator.list.iterator();

    assertThat(firstRule.getRepository()).isEqualTo(r1.getRepositoryKey());
    assertThat(firstRule.getPluginRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(firstRule.getName()).isEqualTo(r1.getName());
    assertThat(firstRule.getRuleDescriptionSectionsDtos().stream()
      .filter(s -> s.getKey().equals(ruleDescriptionSectionDto.getKey()))
      .collect(MoreCollectors.onlyElement()))
        .usingRecursiveComparison()
        .isEqualTo(ruleDescriptionSectionDto);
    assertThat(firstRule.getDescriptionFormat()).isEqualTo(r1.getDescriptionFormat());
    assertThat(firstRule.getSeverity()).isEqualTo(r1.getSeverity());
    assertThat(firstRule.getStatus()).isEqualTo(r1.getStatus());
    assertThat(firstRule.isExternal()).isFalse();
    assertThat(firstRule.isTemplate()).isEqualTo(r1.isTemplate());
    assertThat(firstRule.getSystemTags()).isEqualTo(r1.getSystemTags());
    assertThat(firstRule.getSecurityStandards()).isEqualTo(r1.getSecurityStandards());
    assertThat(firstRule.getTemplateRuleKey()).isNull();
    assertThat(firstRule.getTemplateRepository()).isNull();
    assertThat(firstRule.getInternalKey()).isEqualTo(r1.getConfigKey());
    assertThat(firstRule.getLanguage()).isEqualTo(r1.getLanguage());
    assertThat(firstRule.getType()).isEqualTo(r1.getType());
    assertThat(firstRule.getCreatedAt()).isEqualTo(r1.getCreatedAt());
    assertThat(firstRule.getUpdatedAt()).isEqualTo(r1.getUpdatedAt());

    assertThat(secondRule.isExternal()).isTrue();
  }

  @Test
  public void scrollIndexingRules_maps_rule_definition_fields_for_regular_rule_and_template_rule() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDto r1 = db.rules().insert();
    r1.setTags(Set.of("t1", "t2"));
    r1 = db.rules().update(r1);

    String r1Uuid = r1.getUuid();
    RuleDto r2 = db.rules().insert(rule -> rule.setTemplateUuid(r1Uuid));

    underTest.selectIndexingRules(db.getSession(), accumulator);

    assertThat(accumulator.list).hasSize(2);
    RuleForIndexingDto firstRule = findRuleForIndexingWithUuid(accumulator, r1.getUuid());
    RuleForIndexingDto secondRule = findRuleForIndexingWithUuid(accumulator, r2.getUuid());

    assertRuleDefinitionFieldsAreEquals(r1, firstRule);
    assertRuleMetadataFieldsAreEquals(r1.getMetadata(), firstRule);
    assertThat(firstRule.getTemplateRuleKey()).isNull();
    assertThat(firstRule.getTemplateRepository()).isNull();
    assertRuleDefinitionFieldsAreEquals(r2, secondRule);
    assertThat(secondRule.getTemplateRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(secondRule.getTemplateRepository()).isEqualTo(r1.getRepositoryKey());
  }

  @NotNull
  private static RuleForIndexingDto findRuleForIndexingWithUuid(Accumulator<RuleForIndexingDto> accumulator, String uuid) {
    return accumulator.list.stream()
      .filter(rule -> rule.getUuid().equals(uuid))
      .findFirst().orElseThrow();
  }

  @Test
  public void scrollIndexingRulesByKeys() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDto r1 = db.rules().insert();
    db.rules().insert();

    underTest.selectIndexingRulesByKeys(db.getSession(), singletonList(r1.getUuid()), accumulator);

    assertThat(accumulator.list)
      .extracting(RuleForIndexingDto::getUuid, RuleForIndexingDto::getRuleKey)
      .containsExactlyInAnyOrder(tuple(r1.getUuid(), r1.getKey()));
  }

  @Test
  public void scrollIndexingRulesByKeys_maps_rule_definition_fields_for_regular_rule_and_template_rule() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDto r1 = db.rules().insert();
    RuleDto r2 = db.rules().insert(rule -> rule.setTemplateUuid(r1.getUuid()));

    underTest.selectIndexingRulesByKeys(db.getSession(), Arrays.asList(r1.getUuid(), r2.getUuid()), accumulator);

    assertThat(accumulator.list).hasSize(2);
    RuleForIndexingDto firstRule = accumulator.list.stream().filter(t -> t.getUuid().equals(r1.getUuid())).findFirst().get();
    RuleForIndexingDto secondRule = accumulator.list.stream().filter(t -> t.getUuid().equals(r2.getUuid())).findFirst().get();

    assertRuleDefinitionFieldsAreEquals(r1, firstRule);
    assertThat(firstRule.getTemplateRuleKey()).isNull();
    assertThat(firstRule.getTemplateRepository()).isNull();
    assertRuleDefinitionFieldsAreEquals(r2, secondRule);
    assertThat(secondRule.getTemplateRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(secondRule.getTemplateRepository()).isEqualTo(r1.getRepositoryKey());
  }

  private void assertRuleDefinitionFieldsAreEquals(RuleDto r1, RuleForIndexingDto ruleForIndexing) {
    assertThat(ruleForIndexing.getUuid()).isEqualTo(r1.getUuid());
    assertThat(ruleForIndexing.getRuleKey()).isEqualTo(r1.getKey());
    assertThat(ruleForIndexing.getRepository()).isEqualTo(r1.getRepositoryKey());
    assertThat(ruleForIndexing.getPluginRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(ruleForIndexing.getName()).isEqualTo(r1.getName());
    assertThat(ruleForIndexing.getRuleDescriptionSectionsDtos())
      .usingRecursiveComparison()
      .isEqualTo(r1.getRuleDescriptionSectionDtos());
    assertThat(ruleForIndexing.getDescriptionFormat()).isEqualTo(r1.getDescriptionFormat());
    assertThat(ruleForIndexing.getSeverity()).isEqualTo(r1.getSeverity());
    assertThat(ruleForIndexing.getSeverityAsString()).isEqualTo(SeverityUtil.getSeverityFromOrdinal(r1.getSeverity()));
    assertThat(ruleForIndexing.getStatus()).isEqualTo(r1.getStatus());
    assertThat(ruleForIndexing.isTemplate()).isEqualTo(r1.isTemplate());
    assertThat(ruleForIndexing.getSystemTags()).isEqualTo(r1.getSystemTags());
    assertThat(ruleForIndexing.getSecurityStandards()).isEqualTo(r1.getSecurityStandards());
    assertThat(ruleForIndexing.getInternalKey()).isEqualTo(r1.getConfigKey());
    assertThat(ruleForIndexing.getLanguage()).isEqualTo(r1.getLanguage());
    assertThat(ruleForIndexing.getType()).isEqualTo(r1.getType());
    assertThat(ruleForIndexing.getTypeAsRuleType()).isEqualTo(RuleType.valueOf(r1.getType()));
    assertThat(ruleForIndexing.getCreatedAt()).isEqualTo(r1.getCreatedAt());
    assertThat(ruleForIndexing.getUpdatedAt()).isEqualTo(r1.getUpdatedAt());
  }

  private static void assertRuleMetadataFieldsAreEquals(RuleMetadataDto r1Metadatas, RuleForIndexingDto firstRule) {
    assertThat(r1Metadatas.getTags()).isEqualTo(firstRule.getTags());
  }

  @Test
  public void scrollIndexingRulesByKeys_scrolls_nothing_if_key_does_not_exist() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    db.rules().insert();
    String nonExistingRuleUuid = "non-existing-uuid";

    underTest.selectIndexingRulesByKeys(db.getSession(), singletonList(nonExistingRuleUuid), accumulator);

    assertThat(accumulator.list).isEmpty();
  }

  @Test
  public void scrollIndexingRuleExtensionsByIds() {
    Accumulator<RuleExtensionForIndexingDto> accumulator = new Accumulator<>();
    RuleDto r1 = db.rules().insert();
    RuleMetadataDto r1Extension = db.rules().insertOrUpdateMetadata(r1, r -> r.setTagsField("t1,t2"));
    RuleDto r2 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(r2, r -> r.setTagsField("t1,t3"));

    underTest.scrollIndexingRuleExtensionsByIds(db.getSession(), singletonList(r1.getUuid()), accumulator);

    assertThat(accumulator.list)
      .extracting(RuleExtensionForIndexingDto::getRuleUuid, RuleExtensionForIndexingDto::getRuleKey, RuleExtensionForIndexingDto::getTags)
      .containsExactlyInAnyOrder(
        tuple(r1.getUuid(), r1.getKey(), r1Extension.getTagsAsString()));
  }

  @Test
  public void selectAllDeprecatedRuleKeys() {
    RuleDto r1 = db.rules().insert();
    RuleDto r2 = db.rules().insert();

    db.rules().insertDeprecatedKey(r -> r.setRuleUuid(r1.getUuid()));
    db.rules().insertDeprecatedKey(r -> r.setRuleUuid(r2.getUuid()));

    db.getSession().commit();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeyDtos).hasSize(2);
  }

  @Test
  public void selectDeprecatedRuleKeysByRuleUuids() {
    RuleDto r1 = db.rules().insert();
    RuleDto r2 = db.rules().insert();
    RuleDto r3 = db.rules().insert();
    RuleDto r4 = db.rules().insert();

    DeprecatedRuleKeyDto drk1 = db.rules().insertDeprecatedKey(r -> r.setRuleUuid(r1.getUuid()));
    DeprecatedRuleKeyDto drk2 = db.rules().insertDeprecatedKey(r -> r.setRuleUuid(r1.getUuid()));
    DeprecatedRuleKeyDto drk3 = db.rules().insertDeprecatedKey(r -> r.setRuleUuid(r2.getUuid()));
    db.rules().insertDeprecatedKey(r -> r.setRuleUuid(r4.getUuid()));

    db.getSession().commit();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectDeprecatedRuleKeysByRuleUuids(
      db.getSession(), ImmutableSet.of(r1.getUuid(), r2.getUuid(), r3.getUuid()));
    assertThat(deprecatedRuleKeyDtos)
      .extracting(DeprecatedRuleKeyDto::getUuid)
      .containsExactlyInAnyOrder(drk1.getUuid(), drk2.getUuid(), drk3.getUuid());
  }

  @Test
  public void selectAllDeprecatedRuleKeys_return_values_even_if_there_is_no_rule() {
    db.rules().insertDeprecatedKey();
    db.rules().insertDeprecatedKey();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeyDtos).hasSize(2);
    assertThat(deprecatedRuleKeyDtos)
      .extracting(DeprecatedRuleKeyDto::getNewRepositoryKey, DeprecatedRuleKeyDto::getNewRuleKey)
      .containsExactly(
        tuple(null, null),
        tuple(null, null));
  }

  @Test
  public void deleteDeprecatedRuleKeys_with_empty_list_has_no_effect() {
    db.rules().insertDeprecatedKey();
    db.rules().insertDeprecatedKey();

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);

    underTest.deleteDeprecatedRuleKeys(db.getSession(), emptyList());

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);
  }

  @Test
  public void deleteDeprecatedRuleKeys_with_non_existing_uuid_has_no_effect() {
    db.rules().insertDeprecatedKey(d -> d.setUuid("A1"));
    db.rules().insertDeprecatedKey(d -> d.setUuid("A2"));

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);

    underTest.deleteDeprecatedRuleKeys(db.getSession(), asList("B1", "B2"));

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);
  }

  @Test
  public void deleteDeprecatedRuleKeys() {
    DeprecatedRuleKeyDto deprecatedRuleKeyDto1 = db.rules().insertDeprecatedKey();
    db.rules().insertDeprecatedKey();

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);

    underTest.deleteDeprecatedRuleKeys(db.getSession(), singletonList(deprecatedRuleKeyDto1.getUuid()));
    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(1);
  }

  @Test
  public void insertDeprecatedRuleKey() {
    RuleDto r1 = db.rules().insert();
    DeprecatedRuleKeyDto deprecatedRuleKeyDto = db.rules().insertDeprecatedKey(d -> d.setRuleUuid(r1.getUuid()));

    db.getSession().commit();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeyDtos).hasSize(1);

    DeprecatedRuleKeyDto deprecatedRuleKeyDto1 = deprecatedRuleKeyDtos.iterator().next();
    assertThat(deprecatedRuleKeyDto1.getOldRepositoryKey()).isEqualTo(deprecatedRuleKeyDto.getOldRepositoryKey());
    assertThat(deprecatedRuleKeyDto1.getOldRuleKey()).isEqualTo(deprecatedRuleKeyDto.getOldRuleKey());
    assertThat(deprecatedRuleKeyDto1.getNewRepositoryKey()).isEqualTo(r1.getRepositoryKey());
    assertThat(deprecatedRuleKeyDto1.getNewRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(deprecatedRuleKeyDto1.getUuid()).isEqualTo(deprecatedRuleKeyDto.getUuid());
    assertThat(deprecatedRuleKeyDto1.getCreatedAt()).isEqualTo(deprecatedRuleKeyDto.getCreatedAt());
    assertThat(deprecatedRuleKeyDto1.getRuleUuid()).isEqualTo(r1.getUuid());
  }

  @Test
  public void insertDeprecatedRuleKey_with_same_RuleKey_should_fail() {
    String repositoryKey = randomAlphanumeric(50);
    String ruleKey = randomAlphanumeric(50);
    db.rules().insertDeprecatedKey(d -> d.setOldRepositoryKey(repositoryKey)
      .setOldRuleKey(ruleKey));

    assertThatThrownBy(() -> {
      db.rules().insertDeprecatedKey(d -> d.setOldRepositoryKey(repositoryKey)
        .setOldRuleKey(ruleKey));
    })
      .isInstanceOf(PersistenceException.class);
  }

  private static RuleDescriptionSectionDto createDefaultRuleDescriptionSection() {
    return RuleDescriptionSectionDto.createDefaultRuleDescriptionSection(UuidFactoryFast.getInstance().create(), RandomStringUtils.randomAlphanumeric(1000));
  }

  private static class Accumulator<T> implements Consumer<T> {
    private final List<T> list = new ArrayList<>();

    @Override
    public void accept(T dto) {
      list.add(dto);
    }
  }
}
