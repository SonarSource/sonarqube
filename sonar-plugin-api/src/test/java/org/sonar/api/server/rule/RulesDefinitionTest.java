/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.server.rule;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;

import java.net.URL;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RulesDefinitionTest {

  RulesDefinition.Context context = new RulesDefinition.Context();

  @Rule
  public LogTester logTester = new LogTester();

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
      .addTags("two", "three", "four");

    newRepo.createRule("ABC").setName("ABC").setMarkdownDescription("ABC");
    newRepo.done();

    RulesDefinition.Repository repo = context.repository("findbugs");
    assertThat(repo.rules()).hasSize(2);

    RulesDefinition.Rule rule = repo.rule("NPE");
    assertThat(rule.key()).isEqualTo("NPE");
    assertThat(rule.name()).isEqualTo("Detect NPE");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.htmlDescription()).isEqualTo("Detect <code>java.lang.NullPointerException</code>");
    assertThat(rule.markdownDescription()).isNull();
    assertThat(rule.tags()).containsOnly("one", "two", "three", "four");
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
  public void define_rules_with_technical_debt() {
    RulesDefinition.NewRepository newRepo = context.createRepository("common-java", "java");
    RulesDefinition.NewRule newRule = newRepo.createRule("InsufficientBranchCoverage")
      .setName("Insufficient condition coverage")
      .setHtmlDescription("Insufficient condition coverage by unit tests")
      .setSeverity(Severity.MAJOR)
      .setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTS)
      .setEffortToFixDescription("Effort to test one uncovered branch");
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("1h", "10min"));
    newRepo.done();

    RulesDefinition.Repository repo = context.repository("common-java");
    assertThat(repo.rules()).hasSize(1);

    RulesDefinition.Rule rule = repo.rule("InsufficientBranchCoverage");
    assertThat(rule.debtSubCharacteristic()).isEqualTo("UNIT_TESTS");
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(rule.debtRemediationFunction().coefficient()).isEqualTo("1h");
    assertThat(rule.debtRemediationFunction().offset()).isEqualTo("10min");
    assertThat(rule.effortToFixDescription()).isEqualTo("Effort to test one uncovered branch");
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
    assertThat(rule.debtSubCharacteristic()).isNull();
    assertThat(rule.debtRemediationFunction()).isNull();
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
  public void sanitize_rule_name() {
    RulesDefinition.NewRepository newFindbugs = context.createRepository("findbugs", "java");
    newFindbugs.createRule("NPE").setName("   \n  NullPointer   \n   ").setHtmlDescription("NPE");
    newFindbugs.done();

    RulesDefinition.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.name()).isEqualTo("NullPointer");
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
  public void warning_if_duplicated_rule_keys() {
    RulesDefinition.NewRepository findbugs = context.createRepository("findbugs", "java");
    findbugs.createRule("NPE");
    findbugs.createRule("NPE");
    // do not fail as long as http://jira.sonarsource.com/browse/SONARJAVA-428 is not fixed
    // and as common-rules are packaged within plugins (common-rules were integrated to core in v5.2)
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
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
  public void fail_if_define_characteristic_without_function() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    newRepository.createRule("NPE").setName("NPE").setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setDebtSubCharacteristic("COMPILER");
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Both debt sub-characteristic and debt remediation function should be defined on rule '[repository=findbugs, key=NPE]'");
    }
  }

  @Test
  public void fail_if_define_function_without_characteristic() {
    RulesDefinition.NewRepository newRepository = context.createRepository("findbugs", "java");
    RulesDefinition.NewRule newRule = newRepository.createRule("NPE").setName("NPE").setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setDebtSubCharacteristic("");
    newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset("1h", "10min"));
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Both debt sub-characteristic and debt remediation function should be defined on rule '[repository=findbugs, key=NPE]'");
    }
  }

}
