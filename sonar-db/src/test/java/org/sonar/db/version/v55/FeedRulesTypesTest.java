/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v55;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedRulesTypesTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedRulesTypesTest.class, "schema.sql");

  static final Joiner TAGS_JOINER = Joiner.on(",");

  System2 system2 = mock(System2.class);

  static final long BEFORE = 1000L;
  static final long NOW = 2000L;

  MigrationStep underTest;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
    underTest = new FeedRulesTypes(db.database(), system2);
  }

  @Test
  public void set_type_depending_on_system_tags() throws Exception {
    insertRuleWithSystemTags("only_performance", "performance");
    insertRuleWithSystemTags("no_tag");
    insertRuleWithSystemTags("only_bug", "bug");
    insertRuleWithSystemTags("security", "security", "test");
    insertRuleWithSystemTags("bug_and_security", "security", "bug");
    insertRuleWithType("type_already_exists", RuleType.BUG);

    underTest.execute();

    assertRule("only_performance", RuleType.CODE_SMELL, "performance", NOW);
    assertRule("no_tag", RuleType.CODE_SMELL, "", NOW);
    assertRule("only_bug", RuleType.BUG, "", NOW);
    assertRule("security", RuleType.VULNERABILITY, "test", NOW);
    assertRule("bug_and_security", RuleType.BUG, "", NOW);
    assertRule("type_already_exists", RuleType.BUG, null, BEFORE);
  }

  @Test
  public void remove_forbidden_user_tags() throws Exception {
    insertRuleWithUserTags("only_performance", "performance");
    insertRuleWithUserTags("no_tag");
    insertRuleWithUserTags("only_bug", "bug");
    insertRuleWithUserTags("security", "security", "test");
    insertRuleWithUserTags("bug_and_security", "security", "bug");

    underTest.execute();

    assertRuleWithUserTags("only_performance", "performance", NOW);
    assertRuleWithUserTags("no_tag", "", NOW);
    assertRuleWithUserTags("only_bug", "", NOW);
    assertRuleWithUserTags("security", "test", NOW);
    assertRuleWithUserTags("bug_and_security", "", NOW);
  }

  private void assertRule(String ruleKey, RuleType expectedType, @Nullable String systemTags, long updatedAt) {
    Map<String, Object> rule = db.selectFirst("select rule_type as \"ruleType\", system_tags as \"systemTags\", updated_at as \"updatedAt\" from rules where plugin_rule_key='"
      + ruleKey + "'");
    assertThat(((Number) rule.get("ruleType")).intValue()).isEqualTo(expectedType.getDbConstant());
    assertThat(rule.get("systemTags")).isEqualTo(systemTags);
    assertThat(rule.get("updatedAt")).isEqualTo(updatedAt);
  }

  private void assertRuleWithUserTags(String ruleKey, @Nullable String tags, long updatedAt) {
    Map<String, Object> rule = db.selectFirst("select tags as \"tags\", updated_at as \"updatedAt\" from rules where plugin_rule_key='" + ruleKey + "'");
    assertThat(rule.get("tags")).isEqualTo(tags);
    assertThat(rule.get("updatedAt")).isEqualTo(updatedAt);
  }

  private void insertRuleWithSystemTags(String ruleKey, String... systemTags) {
    db.executeInsert("rules", ImmutableMap.of(
      "plugin_rule_key", ruleKey,
      "system_tags", TAGS_JOINER.join(systemTags),
      "created_at", Long.toString(BEFORE),
      "updated_at", Long.toString(BEFORE)
      ));
  }

  private void insertRuleWithUserTags(String ruleKey, String... userTags) {
    db.executeInsert("rules", ImmutableMap.of(
      "plugin_rule_key", ruleKey,
      "tags", TAGS_JOINER.join(userTags),
      "created_at", Long.toString(BEFORE),
      "updated_at", Long.toString(BEFORE)
      ));
  }

  private void insertRuleWithType(String ruleKey, RuleType type) {
    db.executeInsert("rules", ImmutableMap.of(
      "plugin_rule_key", ruleKey,
      "rule_type", Integer.toString(type.getDbConstant()),
      "created_at", Long.toString(BEFORE),
      "updated_at", Long.toString(BEFORE)
      ));
  }

}
