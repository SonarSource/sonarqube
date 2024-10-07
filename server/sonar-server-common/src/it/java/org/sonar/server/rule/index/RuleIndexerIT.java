/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.server.es.EsTester;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.security.SecurityStandards.CWES_BY_SQ_CATEGORY;
import static org.sonar.server.security.SecurityStandards.SQ_CATEGORY_KEYS_ORDERING;

@RunWith(DataProviderRunner.class)
public class RuleIndexerIT {

  private static final String VALID_HOTSPOT_RULE_DESCRIPTION = "acme\n" +
    "<h2>Ask Yourself Whether</h2>\n" +
    "bar\n" +
    "<h2>Recommended Secure Coding Practices</h2>\n" +
    "foo";

  private static final UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();
  private static final RuleDescriptionSectionDto RULE_DESCRIPTION_SECTION_DTO = createDefaultRuleDescriptionSection(uuidFactory.create(), VALID_HOTSPOT_RULE_DESCRIPTION);
  private static final RuleDescriptionSectionDto RULE_DESCRIPTION_SECTION_DTO2 = RuleDescriptionSectionDto.builder()
    .uuid(uuidFactory.create())
    .key("section2")
    .content("rule descriptions section 2")
    .build();

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = dbTester.getDbClient();
  private final RuleIndexer underTest = new RuleIndexer(es.client(), dbClient);
  private final DbSession dbSession = dbTester.getSession();
  private final RuleDto rule = new RuleDto()
    .setUuid("rule-uuid")
    .setRuleKey("S001")
    .setRepositoryKey("xoo")
    .setConfigKey("S1")
    .setName("Null Pointer")
    .setDescriptionFormat(RuleDto.Format.HTML)
    .addRuleDescriptionSectionDto(RULE_DESCRIPTION_SECTION_DTO)
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
    assertThat(es.countDocuments(TYPE_RULE)).isZero();
  }

  @Test
  public void index() {
    dbClient.ruleDao().insert(dbSession, rule);
    underTest.commitAndIndex(dbSession, rule.getUuid());

    assertThat(es.countDocuments(TYPE_RULE)).isOne();
  }

  @Test
  public void removed_rule_is_not_removed_from_index() {
    // Create and Index rule
    dbClient.ruleDao().insert(dbSession, rule.setStatus(RuleStatus.READY));
    dbSession.commit();
    underTest.commitAndIndex(dbTester.getSession(), rule.getUuid());
    assertThat(es.countDocuments(TYPE_RULE)).isOne();

    // Remove rule
    dbTester.getDbClient().ruleDao().update(dbTester.getSession(), rule.setStatus(RuleStatus.READY).setUpdatedAt(2000000000000L));
    underTest.commitAndIndex(dbTester.getSession(), rule.getUuid());

    assertThat(es.countDocuments(TYPE_RULE)).isOne();
  }

  @Test
  public void index_long_rule_description() {
    RuleDescriptionSectionDto ruleDescriptionSectionDto = createDefaultRuleDescriptionSection(uuidFactory.create(), secure().nextAlphanumeric(100000));
    RuleDto rule = dbTester.rules().insert(newRule(ruleDescriptionSectionDto));

    underTest.commitAndIndex(dbTester.getSession(), rule.getUuid());

    assertThat(es.countDocuments(TYPE_RULE)).isOne();
  }

  @Test
  public void index_long_rule_with_several_sections() {
    RuleDto rule = dbTester.rules().insert(newRule(RULE_DESCRIPTION_SECTION_DTO, RULE_DESCRIPTION_SECTION_DTO2));

    underTest.commitAndIndex(dbTester.getSession(), rule.getUuid());

    List<RuleDoc> ruleDocs = es.getDocuments(TYPE_RULE, RuleDoc.class);
    assertThat(ruleDocs).hasSize(1);
    assertThat(ruleDocs.iterator().next().htmlDescription())
      .contains(RULE_DESCRIPTION_SECTION_DTO.getContent())
      .contains(RULE_DESCRIPTION_SECTION_DTO2.getContent())
      .hasSize(RULE_DESCRIPTION_SECTION_DTO.getContent().length() + " ".length() + RULE_DESCRIPTION_SECTION_DTO2.getContent().length());
  }

  @Test
  public void index_long_rule_with_section_in_markdown() {
    RuleDto rule = dbTester.rules().insert(newRule(RULE_DESCRIPTION_SECTION_DTO).setDescriptionFormat(RuleDto.Format.MARKDOWN));

    underTest.commitAndIndex(dbTester.getSession(), rule.getUuid());

    List<RuleDoc> ruleDocs = es.getDocuments(TYPE_RULE, RuleDoc.class);
    assertThat(ruleDocs).hasSize(1);
    assertThat(ruleDocs.iterator().next().htmlDescription())
      .isEqualTo("acme<br/>&lt;h2&gt;Ask Yourself Whether&lt;/h2&gt;<br/>bar<br/>"
        + "&lt;h2&gt;Recommended Secure Coding Practices&lt;/h2&gt;<br/>foo");
  }

  @Test
  @UseDataProvider("twoDifferentCategoriesButOTHERS")
  public void log_debug_if_hotspot_rule_maps_to_multiple_SQCategories(SQCategory sqCategory1, SQCategory sqCategory2) {
    logTester.setLevel(Level.DEBUG);
    Set<String> standards = Stream.of(sqCategory1, sqCategory2)
      .flatMap(t -> CWES_BY_SQ_CATEGORY.get(t).stream().map(e -> "cwe:" + e))
      .collect(toSet());
    SecurityStandards securityStandards = SecurityStandards.fromSecurityStandards(standards);
    RuleDto rule = dbTester.rules().insert(newRule(RULE_DESCRIPTION_SECTION_DTO)
      .setType(RuleType.SECURITY_HOTSPOT)
      .setSecurityStandards(standards));
    underTest.commitAndIndex(dbTester.getSession(), rule.getUuid());

    assertThat(logTester.logs(Level.DEBUG))
      .contains(format(
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
    return new Object[][]{
      {sqCategory1, sqCategory2}
    };
  }

}
