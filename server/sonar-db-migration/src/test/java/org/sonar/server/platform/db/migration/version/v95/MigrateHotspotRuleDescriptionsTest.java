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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kotlin.Pair;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;
import static org.sonar.server.platform.db.migration.version.v95.DbVersion95.DEFAULT_DESCRIPTION_KEY;

public class MigrateHotspotRuleDescriptionsTest {
  private static final String LEGACY_HOTSPOT_RULE_HTML_DESC = "<p>Formatted SQL queries can be difficult to maintain"
    + "<h2>Ask Yourself Whether</h2>\n"
    + "balbalblabla\n"
    + "<h2>Recommended Secure Coding Practices</h2>\n"
    + "Consider using ORM frameworks"
    + "<h2>Sensitive Code Example</h2>\n"
    + "mysql\n"
    + "<h2>Compliant Solution</h2>\n"
    + "compliant solution desc\n"
    + "<h2>Exceptions</h2>\n"
    + "<p>This rule current implementation does not follow variables.</p>\n"
    + "<h2>See</h2>\n"
    + "<a href=\"https://owasp.org/Top10/A03_2021-Injection/\">OWASP Top 10 2021 Category A3</a> - Injection </li>\n";

  private static final String ISSUE_RULE = "rule_non_hotspot";
  private static final String LEGACY_HOTSPOT_RULE = "rule_legacy_hotspot";
  private static final String LEGACY_HOTSPOT_CUSTOM_RULE = "rule_legacy_hotspot_custom";
  private static final String ADVANCED_RULE = "rule_advanced_hotspot";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MigrateHotspotRuleDescriptionsTest.class, "schema.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private final DataChange fixHotspotRuleDescriptions = new MigrateHotspotRuleDescriptions(db.database(), uuidFactory);

  @Before
  public void setUp() {
    insertRule(ISSUE_RULE, RuleType.CODE_SMELL);
    insertRule(LEGACY_HOTSPOT_RULE, RuleType.SECURITY_HOTSPOT);
    insertRule(LEGACY_HOTSPOT_CUSTOM_RULE, RuleType.SECURITY_HOTSPOT, new Pair<>("template_uuid", LEGACY_HOTSPOT_RULE));
    insertRule(ADVANCED_RULE, RuleType.SECURITY_HOTSPOT);
  }

  private void insertRule(String uuid, RuleType ruleType, Pair<String, Object> ... additionalKeyValues) {
    Map<String, Object> ruleParams = new HashMap<>();
    ruleParams.put("uuid", uuid);
    ruleParams.put("plugin_rule_key", "plugin_key_" + uuid);
    ruleParams.put("plugin_name", "plugin_name");
    ruleParams.put("scope", "ALL");
    ruleParams.put("is_template", false);
    ruleParams.put("is_external", true);
    ruleParams.put("is_ad_hoc", false);
    ruleParams.put("rule_type", ruleType.getDbConstant());
    Arrays.stream(additionalKeyValues).forEach(pair -> ruleParams.put(pair.getFirst(), pair.getSecond()));

    db.executeInsert("rules", ruleParams);
  }

  @Test
  public void insertRuleDescriptions_doesNotFailIfRulesDescSectionsTableIsEmpty() {
    assertThatCode(fixHotspotRuleDescriptions::execute)
      .doesNotThrowAnyException();
  }

  @Test
  public void fixHotspotRuleDescriptions_whenHtmlDescriptionIsComplete_createAllSectionsForLegacyHotspot() throws SQLException {
    insertSectionForLegacyHotspotRule(LEGACY_HOTSPOT_RULE_HTML_DESC);

    fixHotspotRuleDescriptions.execute();

    List<Map<String, Object>> ruleDescriptionSections = findRuleDescriptionSections(LEGACY_HOTSPOT_RULE);

    assertThat(ruleDescriptionSections).hasSize(3)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .contains(tuple("root_cause", "<p>Formatted SQL queries can be difficult to maintain<h2>Exceptions</h2>\n"
        + "<p>This rule current implementation does not follow variables.</p>\n"))
      .contains(tuple("assess_the_problem", "<h2>Ask Yourself Whether</h2>\n"
        + "balbalblabla\n"
        + "<h2>Sensitive Code Example</h2>\n"
        + "mysql\n"))
      .contains(tuple("how_to_fix", "<h2>Recommended Secure Coding Practices</h2>\n"
        + "Consider using ORM frameworks<h2>Compliant Solution</h2>\n"
        + "compliant solution desc\n"
        + "<h2>See</h2>\n"
        + "<a href=\"https://owasp.org/Top10/A03_2021-Injection/\">OWASP Top 10 2021 Category A3</a> - Injection </li>\n"));
  }

  @Test
  public void fixHotspotRuleDescriptions_whenCustomRule_doNotCreateSections() throws SQLException {
    insertSectionForLegacyCustomHotspotRule(LEGACY_HOTSPOT_RULE_HTML_DESC);

    fixHotspotRuleDescriptions.execute();

    List<Map<String, Object>> ruleDescriptionSections = findRuleDescriptionSections(LEGACY_HOTSPOT_CUSTOM_RULE);

    assertThat(ruleDescriptionSections)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .containsOnly(tuple(DEFAULT_DESCRIPTION_KEY, LEGACY_HOTSPOT_RULE_HTML_DESC));
  }

  @Test
  public void fixHotspotRuleDescriptions_whenMixtureOfRules_createAllSectionsForLegacyHotspotAndDoNotModifyOthers() throws SQLException {
    insertSectionForLegacyIssueRule(LEGACY_HOTSPOT_RULE_HTML_DESC);
    insertSectionForLegacyHotspotRule(LEGACY_HOTSPOT_RULE_HTML_DESC);
    insertSectionForAdvancedRule("test", LEGACY_HOTSPOT_RULE_HTML_DESC);

    fixHotspotRuleDescriptions.execute();

    List<Map<String, Object>> ruleDescriptionSectionsLegacyHotspotRule = findRuleDescriptionSections(LEGACY_HOTSPOT_RULE);
    List<Map<String, Object>> ruleDescriptionSectionsIssueRule = findRuleDescriptionSections(ISSUE_RULE);
    List<Map<String, Object>> ruleDescriptionSectionsAdvancedRule = findRuleDescriptionSections(ADVANCED_RULE);

    assertThat(ruleDescriptionSectionsIssueRule)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .containsOnly(tuple(DEFAULT_DESCRIPTION_KEY, LEGACY_HOTSPOT_RULE_HTML_DESC));

    assertThat(ruleDescriptionSectionsAdvancedRule)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .contains(tuple("test", LEGACY_HOTSPOT_RULE_HTML_DESC));

    assertThat(ruleDescriptionSectionsLegacyHotspotRule).hasSize(3)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .contains(tuple("root_cause", "<p>Formatted SQL queries can be difficult to maintain<h2>Exceptions</h2>\n"
        + "<p>This rule current implementation does not follow variables.</p>\n"))
      .contains(tuple("assess_the_problem", "<h2>Ask Yourself Whether</h2>\n"
        + "balbalblabla\n"
        + "<h2>Sensitive Code Example</h2>\n"
        + "mysql\n"))
      .contains(tuple("how_to_fix", "<h2>Recommended Secure Coding Practices</h2>\n"
        + "Consider using ORM frameworks<h2>Compliant Solution</h2>\n"
        + "compliant solution desc\n"
        + "<h2>See</h2>\n"
        + "<a href=\"https://owasp.org/Top10/A03_2021-Injection/\">OWASP Top 10 2021 Category A3</a> - Injection </li>\n"));
  }

  @Test
  public void fixHotspotRuleDescriptions_whenHtmlDescriptionContainsNoHeaders_createOnlyRootCause() throws SQLException {
    String noSection = "No sections";
    insertSectionForLegacyHotspotRule(noSection);

    fixHotspotRuleDescriptions.execute();

    List<Map<String, Object>> ruleDescriptionSections = findRuleDescriptionSections(LEGACY_HOTSPOT_RULE);

    assertThat(ruleDescriptionSections)
      .hasSize(1)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .contains(tuple("root_cause", noSection));
  }

  @Test
  public void fixHotspotRuleDescriptions_whenLegacyIssueRule_doNotChangeSections() throws SQLException {
    insertSectionForLegacyIssueRule(LEGACY_HOTSPOT_RULE_HTML_DESC);

    fixHotspotRuleDescriptions.execute();

    List<Map<String, Object>> ruleDescriptionSections = findRuleDescriptionSections(ISSUE_RULE);

    assertThat(ruleDescriptionSections)
      .hasSize(1)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .contains(tuple("default", LEGACY_HOTSPOT_RULE_HTML_DESC));
  }

  @Test
  public void fixHotspotRuleDescriptions_whenAdvancedRule_doNotChangeSections() throws SQLException {
    insertSectionForAdvancedRule("root_cause", LEGACY_HOTSPOT_RULE_HTML_DESC);
    insertSectionForAdvancedRule("assess_the_problem", LEGACY_HOTSPOT_RULE_HTML_DESC);
    insertSectionForAdvancedRule("how_to_fix", LEGACY_HOTSPOT_RULE_HTML_DESC);
    insertSectionForAdvancedRule("other", "blablabla");

    fixHotspotRuleDescriptions.execute();

    List<Map<String, Object>> ruleDescriptionSections = findRuleDescriptionSections(ADVANCED_RULE);

    assertThat(ruleDescriptionSections)
      .hasSize(4)
      .extracting(r -> r.get("KEE"), r -> r.get("CONTENT"))
      .contains(tuple("root_cause", LEGACY_HOTSPOT_RULE_HTML_DESC))
      .contains(tuple("assess_the_problem", LEGACY_HOTSPOT_RULE_HTML_DESC))
      .contains(tuple("how_to_fix", LEGACY_HOTSPOT_RULE_HTML_DESC))
      .contains(tuple("other", "blablabla"));
  }

  @Test
  public void insertRuleDescriptions_whenReentrant_doesNotFail() throws SQLException {
    insertSectionForLegacyHotspotRule(LEGACY_HOTSPOT_RULE_HTML_DESC);

    fixHotspotRuleDescriptions.execute();
    fixHotspotRuleDescriptions.execute();
    fixHotspotRuleDescriptions.execute();

    assertThat(findRuleDescriptionSections(LEGACY_HOTSPOT_RULE)).hasSize(3);
  }

  @Test
  public void insertRuleDescriptions_skipRules_thatAlreadyHaveAdvancedDesc() throws SQLException {
    insertSectionForLegacyHotspotRule(LEGACY_HOTSPOT_RULE_HTML_DESC);
    insertRuleDescriptionSection(LEGACY_HOTSPOT_RULE, "root_cause", "content to check reentrant");

    fixHotspotRuleDescriptions.execute();

    assertThat(findRuleDescriptionSections(LEGACY_HOTSPOT_RULE)).hasSize(2);
  }

  private List<Map<String, Object>> findAllRuleDescriptionSections() {
    return db.select("select uuid, kee, rule_uuid, content from "
      + RULE_DESCRIPTION_SECTIONS_TABLE + "'");
  }

  private List<Map<String, Object>> findRuleDescriptionSections(String ruleUuid) {
    return db.select("select uuid, kee, rule_uuid, content from "
      + RULE_DESCRIPTION_SECTIONS_TABLE + " where rule_uuid = '" + ruleUuid + "'");
  }

  private void insertSectionForLegacyHotspotRule(String content) {
    insertRuleDescriptionSection(LEGACY_HOTSPOT_RULE, DEFAULT_DESCRIPTION_KEY, content);
  }

  private void insertSectionForLegacyCustomHotspotRule(String content) {
    insertRuleDescriptionSection(LEGACY_HOTSPOT_CUSTOM_RULE, DEFAULT_DESCRIPTION_KEY, content);
  }

  private void insertSectionForLegacyIssueRule(String content) {
    insertRuleDescriptionSection(ISSUE_RULE, DEFAULT_DESCRIPTION_KEY, content);
  }

  private void insertSectionForAdvancedRule(String key, String content) {
    insertRuleDescriptionSection(ADVANCED_RULE, key, content);
  }

  private void insertRuleDescriptionSection(String ruleUuid, String key, String content) {
    Map<String, Object> ruleParams = new HashMap<>();
    ruleParams.put("uuid", RandomStringUtils.randomAlphanumeric(20));
    ruleParams.put("rule_uuid", ruleUuid);
    ruleParams.put("kee", key);
    ruleParams.put("content", content);

    db.executeInsert(RULE_DESCRIPTION_SECTIONS_TABLE, ruleParams);
  }
}
