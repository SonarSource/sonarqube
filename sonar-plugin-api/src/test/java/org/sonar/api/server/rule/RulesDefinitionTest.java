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
package org.sonar.api.server.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.impl.server.RulesDefinitionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class RulesDefinitionTest {

  RulesDefinition.Context context = new RulesDefinitionContext();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void define_repositories() {
    assertThat(context.repositories()).isEmpty();

    context.createRepository("findbugs", "java").setName("Findbugs").done();
    context.createRepository("checkstyle", "java").done();

    assertThat(context.repositories()).hasSize(2);
    RulesDefinition.Repository findbugs = context.repository("findbugs");
    assertThat(findbugs).isNotNull();
    assertThat(findbugs.key()).isEqualTo("findbugs");
    assertThat(findbugs.language()).isEqualTo("java");
    assertThat(findbugs.name()).isEqualTo("Findbugs");
    assertThat(findbugs.rules()).isEmpty();
    RulesDefinition.Repository checkstyle = context.repository("checkstyle");
    assertThat(checkstyle).isNotNull();
    assertThat(checkstyle.key()).isEqualTo("checkstyle");
    assertThat(checkstyle.language()).isEqualTo("java");

    // default name is key
    assertThat(checkstyle.name()).isEqualTo("checkstyle");
    assertThat(checkstyle.rules()).isEmpty();
    assertThat(context.repository("unknown")).isNull();

    // test equals() and hashCode()
    assertThat(findbugs).isEqualTo(findbugs).isNotEqualTo(checkstyle).isNotEqualTo("findbugs").isNotEqualTo(null);
    assertThat(findbugs.hashCode()).isEqualTo(findbugs.hashCode());
  }

  @Test
  public void define_rules() {
    RulesDefinition.NewRepository newRepo = context.createRepository("findbugs", "java");
    newRepo.createRule("NPE")
      .setName("Detect NPE")
      .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setSeverity(Severity.BLOCKER)
      .setInternalKey("/something")
      .setStatus(RuleStatus.BETA)
      .setTags("one", "two")
      .setScope(RuleScope.ALL)
      .addOwaspTop10(RulesDefinition.OwaspTop10.A1, RulesDefinition.OwaspTop10.A3)
      .addCwe(1, 2, 123)
      .addTags("two", "three", "four");

    newRepo.createRule("ABC").setName("ABC").setMarkdownDescription("ABC");
    newRepo.done();

    RulesDefinition.Repository repo = context.repository("findbugs");
    assertThat(repo.rules()).hasSize(2);
    assertThat(repo.isExternal()).isFalse();

    RulesDefinition.Rule rule = repo.rule("NPE");
    assertThat(rule.scope()).isEqualTo(RuleScope.ALL);
    assertThat(rule.key()).isEqualTo("NPE");
    assertThat(rule.name()).isEqualTo("Detect NPE");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.htmlDescription()).isEqualTo("Detect <code>java.lang.NullPointerException</code>");
    assertThat(rule.markdownDescription()).isNull();
    assertThat(rule.tags()).containsOnly("one", "two", "three", "four");
    assertThat(rule.securityStandards()).containsOnly("cwe:1", "cwe:123", "cwe:2", "owaspTop10:a1", "owaspTop10:a3");
    assertThat(rule.params()).isEmpty();
    assertThat(rule.internalKey()).isEqualTo("/something");
    assertThat(rule.template()).isFalse();
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.toString()).isEqualTo("[repository=findbugs, key=NPE]");
    assertThat(rule.repository()).isSameAs(repo);

    RulesDefinition.Rule otherRule = repo.rule("ABC");
    assertThat(otherRule.htmlDescription()).isNull();
    assertThat(otherRule.markdownDescription()).isEqualTo("ABC");

    // test equals() and hashCode()
    assertThat(rule).isEqualTo(rule).isNotEqualTo(otherRule).isNotEqualTo("NPE").isNotEqualTo(null);
    assertThat(rule.hashCode()).isEqualTo(rule.hashCode());
  }

  @Test
  public void define_rules_with_remediation_function() {
    RulesDefinition.NewRepository newRepo = context.createRepository("common-java", "java");
    RulesDefinition.NewRule newRule = newRepo.createRule("InsufficientBranchCoverage")
      .setName("Insufficient condition coverage")
      .setHtmlDescription("Insufficient condition coverage by unit tests")
      .setSeverity(Severity.MAJOR)
      .setGapDescription("Effort to test one uncovered branch");
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("1h", "10min"));
    newRepo.done();

    RulesDefinition.Repository repo = context.repository("common-java");
    assertThat(repo.rules()).hasSize(1);

    RulesDefinition.Rule rule = repo.rule("InsufficientBranchCoverage");
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(rule.debtRemediationFunction().gapMultiplier()).isEqualTo("1h");
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("10min");
    assertThat(rule.gapDescription()).isEqualTo("Effort to test one uncovered branch");
  }

  @Test
  public void define_rule_with_default_fields() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java");
    newFindbugs.createRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newFindbugs.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.key()).isEqualTo("NPE");
    assertThat(rule.severity()).isEqualTo(Severity.MAJOR);
    assertThat(rule.params()).isEmpty();
    assertThat(rule.internalKey()).isNull();
    assertThat(rule.status()).isEqualTo(RuleStatus.defaultStatus());
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.securityStandards()).isEmpty();
    assertThat(rule.debtRemediationFunction()).isNull();
  }

  @Test
  public void define_external_rules() {
    RulesDefinition.NewRepository newRepo = context.createExternalRepository("eslint", "js");
    newRepo.createRule("NPE")
      .setName("Detect NPE")
      .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setSeverity(Severity.BLOCKER)
      .setInternalKey("/something")
      .setStatus(RuleStatus.BETA)
      .setTags("one", "two")
      .setScope(RuleScope.ALL)
      .addOwaspTop10(RulesDefinition.OwaspTop10.A1, RulesDefinition.OwaspTop10.A3)
      .addCwe(1, 2, 123)
      .addTags("two", "three", "four");

    newRepo.createRule("ABC").setName("ABC").setMarkdownDescription("ABC");
    newRepo.done();

    assertThat(context.repository("eslint")).isNull();
    RulesDefinition.Repository repo = context.repository("external_eslint");
    assertThat(repo.rules()).hasSize(2);
    assertThat(repo.isExternal()).isTrue();

    RulesDefinition.Rule rule = repo.rule("NPE");
    assertThat(rule.scope()).isEqualTo(RuleScope.ALL);
    assertThat(rule.key()).isEqualTo("NPE");
    assertThat(rule.name()).isEqualTo("Detect NPE");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.htmlDescription()).isEqualTo("Detect <code>java.lang.NullPointerException</code>");
    assertThat(rule.markdownDescription()).isNull();
    assertThat(rule.tags()).containsOnly("one", "two", "three", "four");
    assertThat(rule.securityStandards()).containsOnly("cwe:1", "cwe:123", "cwe:2", "owaspTop10:a1", "owaspTop10:a3");
    assertThat(rule.params()).isEmpty();
    assertThat(rule.internalKey()).isEqualTo("/something");
    assertThat(rule.template()).isFalse();
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.toString()).isEqualTo("[repository=external_eslint, key=NPE]");
    assertThat(rule.repository()).isSameAs(repo);

    RulesDefinition.Rule otherRule = repo.rule("ABC");
    assertThat(otherRule.htmlDescription()).isNull();
    assertThat(otherRule.markdownDescription()).isEqualTo("ABC");

    // test equals() and hashCode()
    assertThat(rule).isEqualTo(rule).isNotEqualTo(otherRule).isNotEqualTo("NPE").isNotEqualTo(null);
    assertThat(rule.hashCode()).isEqualTo(rule.hashCode());
  }

  @Test
  public void define_rule_parameters() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java");
    RulesDefinition.NewRule newNpe = newFindbugs.createRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newNpe.createParam("level").setDefaultValue("LOW").setName("Level").setDescription("The level").setType(RuleParamType.INTEGER);
    newNpe.createParam("effort");
    newFindbugs.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.params()).hasSize(2);

    RulesDefinition.Param level = rule.param("level");
    assertThat(level.key()).isEqualTo("level");
    assertThat(level.name()).isEqualTo("Level");
    assertThat(level.description()).isEqualTo("The level");
    assertThat(level.defaultValue()).isEqualTo("LOW");
    assertThat(level.type()).isEqualTo(RuleParamType.INTEGER);

    RulesDefinition.Param effort = rule.param("effort");
    assertThat(effort.key()).isEqualTo("effort").isEqualTo(effort.name());
    assertThat(effort.description()).isNull();
    assertThat(effort.defaultValue()).isNull();
    assertThat(effort.type()).isEqualTo(RuleParamType.STRING);

    // test equals() and hashCode()
    assertThat(level).isEqualTo(level).isNotEqualTo(effort).isNotEqualTo("level").isNotEqualTo(null);
    assertThat(level.hashCode()).isEqualTo(level.hashCode());
  }

  @Test
  public void define_rule_parameter_with_empty_default_value() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java");
    RulesDefinition.NewRule newNpe = newFindbugs.createRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newNpe.createParam("level").setDefaultValue("").setName("Level").setDescription("The level").setType(RuleParamType.INTEGER);
    newFindbugs.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.params()).hasSize(1);

    RulesDefinition.Param level = rule.param("level");
    assertThat(level.key()).isEqualTo("level");
    assertThat(level.name()).isEqualTo("Level");
    assertThat(level.description()).isEqualTo("The level");
    // Empty value is converted in null value
    assertThat(level.defaultValue()).isNull();
    assertThat(level.type()).isEqualTo(RuleParamType.INTEGER);
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void addDeprecatedRuleKey_fails_with_IAE_if_repository_is_null_or_empty(String nullOrEmpty) {
    RulesDefinition.NewRepository newRepository = context.createRepository("foo", "bar");
    RulesDefinition.NewRule newRule = newRepository.createRule("doh");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Repository must be set");

    newRule.addDeprecatedRuleKey(nullOrEmpty, "oldKey");
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void addDeprecatedRuleKey_fails_with_IAE_if_key_is_null_or_empty(String nullOrEmpty) {
    RulesDefinition.NewRepository newRepository = context.createRepository("foo", "bar");
    RulesDefinition.NewRule newRule = newRepository.createRule("doh");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Rule must be set");

    newRule.addDeprecatedRuleKey("oldRepo", nullOrEmpty);
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""}
    };
  }

  @Test
  public void getDeprecatedKeys_returns_empty_if_addDeprecatedKeys_never_called() {
    String repositoryKey = "foo";
    String ruleKey = "doh";
    RulesDefinition.NewRepository newRepository = context.createRepository(repositoryKey, "bar");
    newRepository.createRule(ruleKey)
      .setName("doh rule")
      .setHtmlDescription("doh description");
    newRepository.done();
    RulesDefinition.Repository repository = context.repository(repositoryKey);
    RulesDefinition.Rule rule = repository.rule(ruleKey);

    assertThat(rule.deprecatedRuleKeys()).isEmpty();
  }

  @Test
  public void getDeprecatedKeys_returns_keys_in_order_of_addDeprecatedKeys_calls() {
    Set<RuleKey> ruleKeys = ImmutableSet.of(RuleKey.of("foo", "AAA"),
      RuleKey.of("bar", "CCCC"), RuleKey.of("doh", "CCCC"), RuleKey.of("foo", "BBBBBBBBBB"));
    List<RuleKey> sortedRuleKeys = ruleKeys.stream().sorted(Ordering.natural().onResultOf(RuleKey::toString)).collect(Collectors.toList());

    // ensure we don't have the same order
    Assume.assumeTrue(!ImmutableList.copyOf(ruleKeys).equals(sortedRuleKeys));

    String repositoryKey = "foo";
    String ruleKey = "doh";
    RulesDefinition.NewRepository newRepository = context.createRepository(repositoryKey, "bar");
    RulesDefinition.NewRule newRule = newRepository.createRule(ruleKey)
      .setName("doh rule")
      .setHtmlDescription("doh description");
    sortedRuleKeys.forEach(r -> newRule.addDeprecatedRuleKey(r.repository(), r.rule()));
    newRepository.done();
    RulesDefinition.Repository repository = context.repository(repositoryKey);
    RulesDefinition.Rule rule = repository.rule(ruleKey);

    assertThat(ImmutableList.copyOf(rule.deprecatedRuleKeys()))
      .isEqualTo(sortedRuleKeys);
  }

  @Test
  public void getDeprecatedKeys_does_not_return_the_same_key_more_than_once() {
    RuleKey duplicatedRuleKey = RuleKey.of("foo", "AAA");
    RuleKey ruleKey2 = RuleKey.of("bar", "CCCC");
    RuleKey ruleKey3 = RuleKey.of("foo", "BBBBBBBBBB");
    List<RuleKey> ruleKeys = ImmutableList.of(duplicatedRuleKey, ruleKey2, duplicatedRuleKey, duplicatedRuleKey, ruleKey3);

    String repositoryKey = "foo";
    String ruleKey = "doh";
    RulesDefinition.NewRepository newRepository = context.createRepository(repositoryKey, "bar");
    RulesDefinition.NewRule newRule = newRepository.createRule(ruleKey)
      .setName("doh rule")
      .setHtmlDescription("doh description");
    ruleKeys.forEach(r -> newRule.addDeprecatedRuleKey(r.repository(), r.rule()));
    newRepository.done();
    RulesDefinition.Repository repository = context.repository(repositoryKey);
    RulesDefinition.Rule rule = repository.rule(ruleKey);

    assertThat(rule.deprecatedRuleKeys())
      .containsExactly(ruleKey2, duplicatedRuleKey, ruleKey3);
  }

  @Test
  public void sanitize_rule_name() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java");
    newFindbugs.createRule("NPE").setName("   \n  NullPointer   \n   ").setHtmlDescription("NPE");
    newFindbugs.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.name()).isEqualTo("NullPointer");
  }

  @Test
  public void default_scope_should_be_main() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java");
    newFindbugs.createRule("key").setName("name").setHtmlDescription("NPE");
    newFindbugs.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("key");
    assertThat(rule.scope()).isEqualTo(RuleScope.MAIN);
  }

  @Test
  public void add_rules_to_existing_repository() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java").setName("Findbugs");
    newFindbugs.createRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newFindbugs.done();

    RulesDefinition.NewRepository newFbContrib = context.createRepository("findbugs", "java");
    newFbContrib.createRule("VULNERABILITY").setName("Vulnerability").setMarkdownDescription("Detect vulnerability");
    newFbContrib.done();

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository findbugs = context.repository("findbugs");
    assertThat(findbugs.key()).isEqualTo("findbugs");
    assertThat(findbugs.language()).isEqualTo("java");
    assertThat(findbugs.name()).isEqualTo("Findbugs");
    assertThat(findbugs.rules()).extracting("key").containsOnly("NPE", "VULNERABILITY");
  }

  /**
   * This is temporarily accepted only for the support of the common-rules that are still declared
   * by plugins. It could be removed in 7.0
   *
   * @since 5.2
   */
  @Test
  public void allow_to_replace_an_existing_common_rule() {
    RulesDefinition.NewRepository newCommonJava1 = context.createRepository("common-java", "java").setName("Common Java");
    newCommonJava1.createRule("coverage").setName("Lack of coverage").setHtmlDescription("Coverage must be high");
    newCommonJava1.done();

    RulesDefinition.NewRepository newCommonJava2 = context.createRepository("common-java", "java");
    newCommonJava2.createRule("coverage").setName("Lack of coverage (V2)").setMarkdownDescription("Coverage must be high (V2)");
    newCommonJava2.done();

    RulesDefinition.Repository commonJava = context.repository("common-java");
    assertThat(commonJava.rules()).hasSize(1);
    RulesDefinition.Rule rule = commonJava.rule("coverage");
    assertThat(rule.name()).isEqualTo("Lack of coverage (V2)");

    // replacement but not merge -> keep only the v2 (which has markdown but not html description)
    assertThat(rule.markdownDescription()).isEqualTo("Coverage must be high (V2)");
    assertThat(rule.htmlDescription()).isNull();

    // do not log warning
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void cant_set_blank_repository_name() {
    context.createRepository("findbugs", "java").setName(null).done();

    assertThat(context.repository("findbugs").name()).isEqualTo("findbugs");
  }

  @Test
  public void fail_if_duplicated_rule_keys_in_the_same_repository() {
    expectedException.expect(IllegalArgumentException.class);

    RulesDefinition.NewRepository findbugs = context.createRepository("findbugs", "java");
    findbugs.createRule("NPE");
    findbugs.createRule("NPE");
  }

  @Test
  public void fail_if_duplicated_rule_param_keys() {
    RulesDefinition.NewRule rule = context.createRepository("findbugs", "java").createRule("NPE");
    rule.createParam("level");
    try {
      rule.createParam("level");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The parameter 'level' is declared several times on the rule [repository=findbugs, key=NPE]");
    }
  }

  @Test
  public void fail_if_blank_rule_name() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName(null).setHtmlDescription("NPE");
    try {
      newRepository.done();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Name of rule [repository=findbugs, key=NPE] is empty");
    }
  }

  @Test
  public void fail_if_bad_rule_tag() {
    try {
      // whitespaces are not allowed in tags
      context.createRepository("findbugs", "java").createRule("NPE").setTags("coding style");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Tag 'coding style' is invalid. Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'");
    }
  }

  @Test
  public void load_rule_html_description_from_file() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setHtmlDescription(getClass().getResource("/org/sonar/api/server/rule/RulesDefinitionTest/sample.html"));
    newRepository.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.htmlDescription()).isEqualTo("description of rule loaded from file");
  }

  @Test
  public void load_rule_markdown_description_from_file() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setMarkdownDescription(getClass().getResource("/org/sonar/api/server/rule/RulesDefinitionTest/sample.md"));
    newRepository.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.markdownDescription()).isEqualTo("description of rule loaded from file");
  }

  @Test
  public void fail_to_load_html_rule_description_from_file() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setHtmlDescription((URL) null);
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("One of HTML description or Markdown description must be defined for rule [repository=findbugs, key=NPE]");
    }
  }

  @Test
  public void fail_to_load_markdown_rule_description_from_file() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setMarkdownDescription((URL) null);
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("One of HTML description or Markdown description must be defined for rule [repository=findbugs, key=NPE]");
    }
  }

  @Test
  public void fail_if_no_rule_description() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE");
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("One of HTML description or Markdown description must be defined for rule [repository=findbugs, key=NPE]");
    }
  }

  @Test
  public void fail_if_rule_already_has_html_description() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    try {
      newRepository.createRule("NPE").setName("NPE").setHtmlDescription("polop").setMarkdownDescription("palap");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Rule '[repository=findbugs, key=NPE]' already has an HTML description");
    }
  }

  @Test
  public void fail_if_rule_already_has_markdown_description() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    try {
      newRepository.createRule("NPE").setName("NPE").setMarkdownDescription("palap").setHtmlDescription("polop");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Rule '[repository=findbugs, key=NPE]' already has a Markdown description");
    }
  }

  @Test
  public void fail_if_bad_rule_severity() {
    try {
      context.createRepository("findbugs", "java").createRule("NPE").setSeverity("VERY HIGH");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Severity of rule [repository=findbugs, key=NPE] is not correct: VERY HIGH");
    }
  }

  @Test
  public void fail_if_removed_status() {
    try {
      context.createRepository("findbugs", "java").createRule("NPE").setStatus(RuleStatus.REMOVED);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Status 'REMOVED' is not accepted on rule '[repository=findbugs, key=NPE]'");
    }
  }

  @Test
  public void sqale_characteristic_is_deprecated_and_is_ignored() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setHtmlDescription("desc")
      .setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.API_ABUSE);
    newRepository.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.debtSubCharacteristic()).isNull();
  }

  @Test
  public void type_is_defined() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setHtmlDescription("desc")
      .setType(RuleType.VULNERABILITY).setTags("bug", "misra");
    newRepository.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    // type VULNERABILITY is kept even if the tag "bug" is present
    assertThat(rule.type()).isEqualTo(RuleType.VULNERABILITY);
    // tag "bug" is reserved and removed.
    assertThat(rule.tags()).containsOnly("misra");
  }

  @Test
  public void guess_type_from_tags_if_type_is_missing() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setHtmlDescription("desc").setTags("bug", "misra");
    newRepository.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.type()).isEqualTo(RuleType.BUG);
    // tag "bug" is reserved and removed
    assertThat(rule.tags()).containsOnly("misra");
  }
}
