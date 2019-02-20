/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE_EXTENSION;

public class RuleIndexerTest {

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public DbTester dbTester = DbTester.create();

  private DbClient dbClient = dbTester.getDbClient();
  private final RuleIndexer underTest = new RuleIndexer(es.client(), dbClient);
  private DbSession dbSession = dbTester.getSession();
  private RuleDefinitionDto rule = new RuleDefinitionDto()
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
    .setScope(Scope.ALL)
    .setCreatedAt(1500000000000L)
    .setUpdatedAt(1600000000000L);

  @Test
  public void index_nothing() {
    underTest.index(dbSession, emptyList());
    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(0L);
  }

  @Test
  public void index() {
    dbClient.ruleDao().insert(dbSession, rule);
    underTest.commitAndIndex(dbSession, rule.getId());

    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(1);
  }

  @Test
  public void removed_rule_is_not_removed_from_index() {
    // Create and Index rule
    dbClient.ruleDao().insert(dbSession, rule.setStatus(RuleStatus.READY));
    dbSession.commit();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId());
    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(1);

    // Remove rule
    dbTester.getDbClient().ruleDao().update(dbTester.getSession(), rule.setStatus(RuleStatus.READY).setUpdatedAt(2000000000000L));
    underTest.commitAndIndex(dbTester.getSession(), rule.getId());

    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(1);
  }

  @Test
  public void index_rule_extension_with_long_id() {
    RuleDefinitionDto rule = dbTester.rules().insert(r -> r.setRuleKey(RuleTesting.randomRuleKeyOfMaximumLength()));
    underTest.commitAndIndex(dbTester.getSession(), rule.getId());
    OrganizationDto organization = dbTester.organizations().insert();
    RuleMetadataDto metadata = RuleTesting.newRuleMetadata(rule, organization).setTags(ImmutableSet.of("bla"));
    dbTester.getDbClient().ruleDao().insertOrUpdate(dbTester.getSession(), metadata);
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    RuleExtensionDoc doc = new RuleExtensionDoc()
      .setRuleId(rule.getId())
      .setScope(RuleExtensionScope.organization(organization.getUuid()));
    assertThat(
      es.client()
        .prepareSearch(TYPE_RULE_EXTENSION.getMainType())
        .setQuery(termQuery("_id", doc.getId()))
        .get()
        .getHits()
        .getHits()[0]
        .getId()).isEqualTo(doc.getId());
  }

  @Test
  public void delete_rule_extension_from_index_when_setting_rule_tags_to_empty() {
    RuleDefinitionDto rule = dbTester.rules().insert(r -> r.setRuleKey(RuleTesting.randomRuleKeyOfMaximumLength()));
    underTest.commitAndIndex(dbTester.getSession(), rule.getId());
    OrganizationDto organization = dbTester.organizations().insert();
    RuleMetadataDto metadata = RuleTesting.newRuleMetadata(rule, organization).setTags(ImmutableSet.of("bla"));
    dbTester.getDbClient().ruleDao().insertOrUpdate(dbTester.getSession(), metadata);
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    // index tags
    RuleExtensionDoc doc = new RuleExtensionDoc()
      .setRuleId(rule.getId())
      .setScope(RuleExtensionScope.organization(organization.getUuid()));
    assertThat(es.getIds(TYPE_RULE_EXTENSION)).contains(doc.getId());

    // update db table "rules_metadata" with empty tags and delete tags from index
    metadata = RuleTesting.newRuleMetadata(rule, organization).setTags(emptySet());
    dbTester.getDbClient().ruleDao().insertOrUpdate(dbTester.getSession(), metadata);
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);
    assertThat(es.getIds(TYPE_RULE_EXTENSION)).doesNotContain(doc.getId());
  }

  @Test
  public void index_long_rule_description() {
    String description = IntStream.range(0, 100000).map(i -> i % 100).mapToObj(Integer::toString).collect(Collectors.joining(" "));
    RuleDefinitionDto rule = dbTester.rules().insert(r -> r.setDescription(description));
    underTest.commitAndIndex(dbTester.getSession(), rule.getId());

    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(1);
  }
}
