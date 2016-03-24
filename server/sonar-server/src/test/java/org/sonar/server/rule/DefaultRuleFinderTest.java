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
package org.sonar.server.rule;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRuleFinderTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();
  DbSession session = dbTester.getSession();

  RuleDto rule1 = new RuleDto()
    .setName("Check Header")
    .setConfigKey("Checker/Treewalker/HeaderCheck")
    .setRuleKey("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck")
    .setRepositoryKey("checkstyle")
    .setSeverity(4)
    .setStatus(RuleStatus.READY);

  RuleDto rule2 = new RuleDto()
    .setName("Disabled checked")
    .setConfigKey("Checker/Treewalker/DisabledCheck")
    .setRuleKey("DisabledCheck")
    .setRepositoryKey("checkstyle")
    .setSeverity(4)
    .setStatus(RuleStatus.REMOVED);

  RuleDto rule3 = new RuleDto()
    .setName("Check Annotation")
    .setConfigKey("Checker/Treewalker/AnnotationUseStyleCheck")
    .setRuleKey("com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck")
    .setRepositoryKey("checkstyle")
    .setSeverity(4)
    .setStatus(RuleStatus.READY);

  RuleDto rule4 = new RuleDto()
    .setName("Call Super First")
    .setConfigKey("rulesets/android.xml/CallSuperFirst")
    .setRuleKey("CallSuperFirst")
    .setRepositoryKey("pmd")
    .setSeverity(2)
    .setStatus(RuleStatus.READY);

  DefaultRuleFinder underTest = new DefaultRuleFinder(dbClient);

  @Before
  public void setup() {
    dbClient.ruleDao().insert(session, rule1);
    dbClient.ruleDao().insert(session, rule2);
    dbClient.ruleDao().insert(session, rule3);
    dbClient.ruleDao().insert(session, rule4);
    session.commit();
  }

  @Test
  public void should_success_finder_wrap() {
    // has Id
    assertThat(underTest.findById(rule1.getId()).getId()).isEqualTo(rule1.getId());

    // should_find_by_id
    assertThat(underTest.findById(rule3.getId()).getConfigKey()).isEqualTo("Checker/Treewalker/AnnotationUseStyleCheck");

    // should_not_find_disabled_rule_by_id
    assertThat(underTest.findById(rule2.getId())).isNull();

    // should_find_by_ids
    assertThat(underTest.findByIds(newArrayList(rule2.getId(), rule3.getId()))).hasSize(2);

    // should_find_by_key
    Rule rule = underTest.findByKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck");
    assertThat(rule).isNotNull();
    assertThat(rule.getKey()).isEqualTo(("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck"));
    assertThat(rule.isEnabled()).isTrue();

    // find_should_return_null_if_no_results
    assertThat(underTest.findByKey("checkstyle", "unknown")).isNull();
    assertThat(underTest.find(RuleQuery.create().withRepositoryKey("checkstyle").withConfigKey("unknown"))).isNull();

    // find_repository_rules
    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey("checkstyle"))).hasSize(2);

    // find_all_enabled
    assertThat(underTest.findAll(RuleQuery.create())).extracting("id").containsOnly(rule1.getId(), rule3.getId(), rule4.getId());
    assertThat(underTest.findAll(RuleQuery.create())).hasSize(3);

    // do_not_find_disabled_rules
    assertThat(underTest.findByKey("checkstyle", "DisabledCheck")).isNull();

    // do_not_find_unknown_rules
    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey("unknown_repository"))).isEmpty();
  }

  @Test
  public void find_ids_including_removed_rule() {
    // find rule with id 2 is REMOVED
    assertThat(underTest.findByIds(newArrayList(rule2.getId()))).hasSize(1);
  }

  @Test
  public void find_keys_including_removed_rule() {
    assertThat(underTest.findByKeys(newArrayList(RuleKey.of("checkstyle", "DisabledCheck")))).hasSize(1);

    // find rule with id 2 is REMOVED
    assertThat(underTest.findByKeys(newArrayList(RuleKey.of("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck")))).hasSize(1);

    assertThat(underTest.findByKeys(Collections.<RuleKey>emptyList())).isEmpty();
  }

  @Test
  public void find_id_return_null_on_removed_rule() {
    // find rule with id 2 is REMOVED
    assertThat(underTest.findById(rule2.getId())).isNull();
  }

  @Test
  public void find_all_not_include_removed_rule() {
    // find rule with id 2 is REMOVED
    assertThat(underTest.findAll(RuleQuery.create())).extracting("id").containsOnly(rule1.getId(), rule3.getId(), rule4.getId());
  }

}
