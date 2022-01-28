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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableSet;
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
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRuleFinderTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession session = dbTester.getSession();

  private final RuleDto rule1 = new RuleDto()
    .setName("Check Header")
    .setConfigKey("Checker/Treewalker/HeaderCheck")
    .setRuleKey("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck")
    .setRepositoryKey("checkstyle")
    .setSeverity(4)
    .setScope(Scope.MAIN)
    .setStatus(RuleStatus.READY);

  private final RuleDto rule2 = new RuleDto()
    .setName("Disabled checked")
    .setConfigKey("Checker/Treewalker/DisabledCheck")
    .setRuleKey("DisabledCheck")
    .setRepositoryKey("checkstyle")
    .setSeverity(4)
    .setScope(Scope.MAIN)
    .setStatus(RuleStatus.REMOVED);

  private final RuleDto rule3 = new RuleDto()
    .setName("Check Annotation")
    .setConfigKey("Checker/Treewalker/AnnotationUseStyleCheck")
    .setRuleKey("com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck")
    .setRepositoryKey("checkstyle")
    .setSeverity(4)
    .setScope(Scope.MAIN)
    .setStatus(RuleStatus.READY);

  private final RuleDto rule4 = new RuleDto()
    .setName("Call Super First")
    .setConfigKey("rulesets/android.xml/CallSuperFirst")
    .setRuleKey("CallSuperFirst")
    .setRepositoryKey("pmd")
    .setSeverity(2)
    .setScope(Scope.MAIN)
    .setStatus(RuleStatus.READY);

  private final DefaultRuleFinder underTest = new DefaultRuleFinder(dbClient);

  @Before
  public void setup() {
    dbTester.rules().insertRule(rule1);
    dbTester.rules().insertRule(rule2);
    dbTester.rules().insertRule(rule3);
    dbTester.rules().insertRule(rule4);
    session.commit();
  }

  @Test
  public void should_success_finder_wrap() {
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
    assertThat(underTest.findAll(RuleQuery.create())).extracting(Rule::ruleKey).containsOnly(rule1.getKey(), rule3.getKey(), rule4.getKey());

    // find_all
    assertThat(underTest.findAll()).extracting(RuleDefinitionDto::getRuleKey).containsOnly(rule1.getKey().rule(), rule3.getKey().rule(), rule4.getKey().rule());

    // do_not_find_disabled_rules
    assertThat(underTest.findByKey("checkstyle", "DisabledCheck")).isNull();

    // do_not_find_unknown_rules
    assertThat(underTest.findAll(RuleQuery.create().withRepositoryKey("unknown_repository"))).isEmpty();

    assertThat(underTest.findDtoByKey(RuleKey.of("pmd", "CallSuperFirst")).get().getUuid()).isEqualTo(rule4.getUuid());
    assertThat(underTest.findDtoByUuid(rule4.getUuid())).isPresent();
  }

  @Test
  public void should_fail_find() {
    assertThat(underTest.findDtoByKey(RuleKey.of("pmd", "unknown"))).isEmpty();
    assertThat(underTest.findDtoByUuid("unknown")).isEmpty();
  }

  @Test
  public void find_all_not_include_removed_rule() {
    // rule 3 is REMOVED
    assertThat(underTest.findAll(RuleQuery.create())).extracting(Rule::ruleKey).containsOnly(rule1.getKey(), rule3.getKey(), rule4.getKey());
    assertThat(underTest.findAll()).extracting(RuleDefinitionDto::getRuleKey).containsOnly(rule1.getKey().rule(), rule3.getKey().rule(), rule4.getKey().rule());
  }

  @Test
  public void findByKey_populates_system_tags_but_not_tags() {
    RuleDefinitionDto ruleDefinition = dbTester.rules()
      .insert(t -> t.setSystemTags(ImmutableSet.of(randomAlphanumeric(5), randomAlphanumeric(6))));
    dbTester.rules().insertRule();

    Rule rule = underTest.findByKey(ruleDefinition.getKey());
    assertThat(rule.getSystemTags())
      .containsOnlyElementsOf(ruleDefinition.getSystemTags());
    assertThat(rule.getTags()).isEmpty();

    rule = underTest.findByKey(ruleDefinition.getRepositoryKey(), ruleDefinition.getRuleKey());
    assertThat(rule.getSystemTags())
      .containsOnlyElementsOf(ruleDefinition.getSystemTags());
    assertThat(rule.getTags()).isEmpty();
  }
}
