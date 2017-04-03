/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule.index;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleIteratorForSingleChunkTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private RuleDefinitionDto templateRule;
  private RuleDefinitionDto customRule;

  @Before
  public void setUp() throws Exception {
    templateRule = new RuleDefinitionDto()
        .setRuleKey("S001")
        .setRepositoryKey("xoo")
        .setConfigKey("S1")
        .setName("Null Pointer")
        .setDescription("S001 desc")
        .setDescriptionFormat(RuleDto.Format.HTML)
        .setLanguage("xoo")
        .setSeverity(Severity.BLOCKER)
        .setStatus(RuleStatus.READY)
        .setIsTemplate(true)
        .setSystemTags(newHashSet("cwe"))
        .setType(RuleType.BUG)
        .setCreatedAt(1500000000000L)
        .setUpdatedAt(1600000000000L);

    customRule = new RuleDefinitionDto()
        .setRuleKey("S002")
        .setRepositoryKey("xoo")
        .setConfigKey("S2")
        .setName("Slow")
        .setDescription("*S002 desc*")
        .setDescriptionFormat(RuleDto.Format.MARKDOWN)
        .setLanguage("xoo")
        .setSeverity(Severity.MAJOR)
        .setStatus(RuleStatus.BETA)
        .setIsTemplate(false)
        .setType(RuleType.CODE_SMELL)
        .setCreatedAt(2000000000000L)
        .setUpdatedAt(2100000000000L);
  }

  @Test
  public void iterator_over_one_rule() {
    dbTester.rules().insert(templateRule);

    List<RuleDocWithSystemScope> results = getResults();

    assertThat(results).hasSize(1);

    RuleDocWithSystemScope ruleDocWithSystemScope = getRuleDoc(results, templateRule.getRuleKey());
    RuleDoc templateDoc = ruleDocWithSystemScope.getRuleDoc();
    RuleExtensionDoc templateExtensionDoc = ruleDocWithSystemScope.getRuleExtensionDoc();
    assertThat(templateDoc).isNotNull();
    assertThat(templateDoc.key()).isEqualTo(RuleKey.of("xoo", "S001"));
    assertThat(templateDoc.ruleKey()).isEqualTo("S001");
    assertThat(templateDoc.repository()).isEqualTo("xoo");
    assertThat(templateDoc.internalKey()).isEqualTo("S1");
    assertThat(templateDoc.name()).isEqualTo("Null Pointer");
    assertThat(templateDoc.htmlDescription()).isEqualTo("S001 desc");
    assertThat(templateDoc.language()).isEqualTo("xoo");
    assertThat(templateDoc.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(templateDoc.status()).isEqualTo(RuleStatus.READY);
    assertThat(templateDoc.isTemplate()).isTrue();
    assertThat(templateExtensionDoc.getTags()).containsOnly("cwe");
    assertThat(templateDoc.createdAt()).isEqualTo(1500000000000L);
    assertThat(templateDoc.updatedAt()).isEqualTo(1600000000000L);
  }

  @Test
  public void iterator_over_rules() {
    dbTester.rules().insert(templateRule);
    dbClient.ruleDao().insert(dbSession, customRule);
    dbSession.commit();

    List<RuleDocWithSystemScope> results = getResults();

    assertThat(results).hasSize(2);

    RuleDocWithSystemScope templateDocWithSystemScope = getRuleDoc(results, templateRule.getRuleKey());
    RuleDoc templateDoc = templateDocWithSystemScope.getRuleDoc();
    RuleExtensionDoc templateExtensionDoc = templateDocWithSystemScope.getRuleExtensionDoc();
    assertThat(templateDoc.key()).isEqualTo(RuleKey.of("xoo", "S001"));
    assertThat(templateDoc.ruleKey()).isEqualTo("S001");
    assertThat(templateDoc.repository()).isEqualTo("xoo");
    assertThat(templateDoc.internalKey()).isEqualTo("S1");
    assertThat(templateDoc.name()).isEqualTo("Null Pointer");
    assertThat(templateDoc.htmlDescription()).isEqualTo("S001 desc");
    assertThat(templateDoc.language()).isEqualTo("xoo");
    assertThat(templateDoc.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(templateDoc.status()).isEqualTo(RuleStatus.READY);
    assertThat(templateDoc.isTemplate()).isTrue();
    assertThat(templateExtensionDoc.getTags()).containsOnly("cwe");
    assertThat(templateDoc.createdAt()).isEqualTo(1500000000000L);
    assertThat(templateDoc.updatedAt()).isEqualTo(1600000000000L);

    RuleDocWithSystemScope customDocWithSystemScope = getRuleDoc(results, customRule.getRuleKey());
    RuleDoc customDoc = customDocWithSystemScope.getRuleDoc();
    RuleExtensionDoc customExtensionDoc = customDocWithSystemScope.getRuleExtensionDoc();
    assertThat(customDoc.key()).isEqualTo(RuleKey.of("xoo", "S002"));
    assertThat(customDoc.ruleKey()).isEqualTo("S002");
    assertThat(customDoc.repository()).isEqualTo("xoo");
    assertThat(customDoc.internalKey()).isEqualTo("S2");
    assertThat(customDoc.name()).isEqualTo("Slow");
    assertThat(customDoc.htmlDescription()).isEqualTo("<strong>S002 desc</strong>");
    assertThat(customDoc.language()).isEqualTo("xoo");
    assertThat(customDoc.severity()).isEqualTo(Severity.MAJOR);
    assertThat(customDoc.status()).isEqualTo(RuleStatus.BETA);
    assertThat(customDoc.isTemplate()).isFalse();
    assertThat(customExtensionDoc.getTags()).isEmpty();
    assertThat(customDoc.createdAt()).isEqualTo(2000000000000L);
    assertThat(customDoc.updatedAt()).isEqualTo(2100000000000L);
  }

  @Test
  public void custom_rule() {
    dbTester.rules().insert(templateRule);
    dbClient.ruleDao().insert(dbSession, customRule.setTemplateId(templateRule.getId()));
    dbSession.commit();

    List<RuleDocWithSystemScope> results = getResults();

    assertThat(results).hasSize(2);

    RuleDocWithSystemScope templateDocWithSystemScope = getRuleDoc(results, templateRule.getRuleKey());
    RuleDoc templateDoc = templateDocWithSystemScope.getRuleDoc();
    assertThat(templateDoc.isTemplate()).isTrue();
    assertThat(templateDoc.templateKey()).isNull();

    RuleDocWithSystemScope customDocWithSystemScope = getRuleDoc(results, customRule.getRuleKey());
    RuleDoc customDoc = customDocWithSystemScope.getRuleDoc();
    assertThat(customDoc.isTemplate()).isFalse();
    assertThat(customDoc.templateKey()).isEqualTo(RuleKey.of("xoo", "S001"));
  }

  @Test
  public void removed_rule_is_returned() {
    dbTester.rules().insert(templateRule.setStatus(RuleStatus.REMOVED));
    dbSession.commit();

    List<RuleDocWithSystemScope> results = getResults();

    assertThat(results).hasSize(1);
  }

  private List<RuleDocWithSystemScope> getResults() {
    return Lists.newArrayList(new RuleIteratorForSingleChunk(dbTester.getDbClient(), null));
  }

  private RuleDocWithSystemScope getRuleDoc(List<RuleDocWithSystemScope> results, String ruleKey) {
    RuleDocWithSystemScope rule;
    rule = results.stream()
      .filter(r -> ruleKey.equals(r.getRuleDoc().key().rule()))
      .findAny()
      .orElseThrow(() -> new NotFoundException("Rule not found in results"));
    return rule;
  }
}
