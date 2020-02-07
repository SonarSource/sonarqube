/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
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
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE_EXTENSION;
import static org.sonar.server.security.SecurityStandards.CWES_BY_SQ_CATEGORY;
import static org.sonar.server.security.SecurityStandards.SQ_CATEGORY_KEYS_ORDERING;

@RunWith(DataProviderRunner.class)
public class RuleIndexerTest {

  public static final String VALID_HOTSPOT_RULE_DESCRIPTION = "acme\n" +
    "<h2>Ask Yourself Whether</h2>\n" +
    "bar\n" +
    "<h2>Recommended Secure Coding Practices</h2>\n" +
    "foo";
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();

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
    String description = IntStream.range(0, 100000).map(i -> i % 100).mapToObj(Integer::toString).collect(joining(" "));
    RuleDefinitionDto rule = dbTester.rules().insert(r -> r.setDescription(description));
    underTest.commitAndIndex(dbTester.getSession(), rule.getId());

    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(1);
  }

  @Test
  @UseDataProvider("twoDifferentCategoriesButOTHERS")
  public void log_a_warning_if_hotspot_rule_maps_to_multiple_SQCategories(SQCategory sqCategory1, SQCategory sqCategory2) {
    Set<String> standards = Stream.of(sqCategory1, sqCategory2)
      .flatMap(t -> CWES_BY_SQ_CATEGORY.get(t).stream().map(e -> "cwe:" + e))
      .collect(toSet());
    SecurityStandards securityStandards = SecurityStandards.fromSecurityStandards(standards);
    RuleDefinitionDto rule = dbTester.rules().insert(RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setSecurityStandards(standards)
      .setDescription(VALID_HOTSPOT_RULE_DESCRIPTION));
    OrganizationDto organization = dbTester.organizations().insert();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .isEqualTo(format(
        "Rule %s with CWEs '%s' maps to multiple SQ Security Categories: %s",
        rule.getKey(),
        String.join(", ", securityStandards.getCwe()),
        ImmutableSet.of(sqCategory1, sqCategory2).stream()
          .map(SQCategory::getKey)
          .sorted(SQ_CATEGORY_KEYS_ORDERING)
          .collect(joining(", "))));
  }

  @DataProvider
  public static Object[][] twoDifferentCategoriesButOTHERS() {
    EnumSet<SQCategory> sqCategories = EnumSet.allOf(SQCategory.class);
    sqCategories.remove(SQCategory.OTHERS);

    // pick two random categories
    Random random = new Random();
    SQCategory sqCategory1 = sqCategories.toArray(new SQCategory[0])[random.nextInt(sqCategories.size())];
    sqCategories.remove(sqCategory1);
    SQCategory sqCategory2 = sqCategories.toArray(new SQCategory[0])[random.nextInt(sqCategories.size())];
    return new Object[][] {
      {sqCategory1, sqCategory2}
    };
  }

  @Test
  @UseDataProvider("nullEmptyOrNoTitleDescription")
  public void log_a_warning_when_hotspot_rule_description_is_null_or_empty(@Nullable String description) {
    RuleDefinitionDto rule = dbTester.rules().insert(RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setDescription(description));
    OrganizationDto organization = dbTester.organizations().insert();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .isEqualTo(format(
        "Description of Security Hotspot Rule %s can't be fully parsed: What is the risk?=missing, Are you vulnerable?=missing, How to fix it=missing",
        rule.getKey()));
  }

  @DataProvider
  public static Object[][] nullEmptyOrNoTitleDescription() {
    return new Object[][] {
      {null},
      {""},
    };
  }

  @Test
  public void log_a_warning_when_hotspot_rule_description_has_none_of_the_key_titles() {
    RuleDefinitionDto rule = dbTester.rules().insert(RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setDescription(randomAlphabetic(30)));
    OrganizationDto organization = dbTester.organizations().insert();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .isEqualTo(format(
        "Description of Security Hotspot Rule %s can't be fully parsed: What is the risk?=ok, Are you vulnerable?=missing, How to fix it=missing",
        rule.getKey()));
  }

  @Test
  public void log_a_warning_when_hotspot_rule_description_is_missing_fixIt_tab_content() {
    RuleDefinitionDto rule = dbTester.rules().insert(RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setDescription("bar\n" +
        "<h2>Ask Yourself Whether</h2>\n" +
        "foo"));
    OrganizationDto organization = dbTester.organizations().insert();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .isEqualTo(format(
        "Description of Security Hotspot Rule %s can't be fully parsed: What is the risk?=ok, Are you vulnerable?=ok, How to fix it=missing",
        rule.getKey()));
  }

  @Test
  public void log_a_warning_when_hotspot_rule_description_is_missing_risk_tab_content() {
    RuleDefinitionDto rule = dbTester.rules().insert(RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setDescription("<h2>Ask Yourself Whether</h2>\n" +
        "bar\n" +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        "foo"));
    OrganizationDto organization = dbTester.organizations().insert();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .isEqualTo(format(
        "Description of Security Hotspot Rule %s can't be fully parsed: What is the risk?=missing, Are you vulnerable?=ok, How to fix it=ok",
        rule.getKey()));
  }

  @Test
  public void log_a_warning_when_hotspot_rule_description_is_missing_vulnerable_tab_content() {
    RuleDefinitionDto rule = dbTester.rules().insert(RuleTesting.newRule()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setDescription("bar\n" +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        "foo"));
    OrganizationDto organization = dbTester.organizations().insert();
    underTest.commitAndIndex(dbTester.getSession(), rule.getId(), organization);

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0))
      .isEqualTo(format(
        "Description of Security Hotspot Rule %s can't be fully parsed: What is the risk?=ok, Are you vulnerable?=missing, How to fix it=ok",
        rule.getKey()));
  }
}
