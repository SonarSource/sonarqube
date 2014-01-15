/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.rule;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleDefinitionsTest {

  RuleDefinitions.Context context = new RuleDefinitions.Context();

  @Test
  public void define_repositories() throws Exception {
    assertThat(context.repositories()).isEmpty();

    context.newRepository("findbugs", "java").setName("Findbugs").done();
    context.newRepository("checkstyle", "java").done();

    assertThat(context.repositories()).hasSize(2);
    RuleDefinitions.Repository findbugs = context.repository("findbugs");
    assertThat(findbugs).isNotNull();
    assertThat(findbugs.key()).isEqualTo("findbugs");
    assertThat(findbugs.language()).isEqualTo("java");
    assertThat(findbugs.name()).isEqualTo("Findbugs");
    assertThat(findbugs.rules()).isEmpty();
    RuleDefinitions.Repository checkstyle = context.repository("checkstyle");
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
    RuleDefinitions.NewRepository newFindbugs = context.newRepository("findbugs", "java");
    newFindbugs.newRule("NPE")
      .setName("Detect NPE")
      .setHtmlDescription("Detect <code>NPE</code>")
      .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
      .setDefaultSeverity(Severity.BLOCKER)
      .setMetadata("/something")
      .setStatus(RuleDefinitions.Status.BETA)
      .setTags("one", "two")
      .addTags("two", "three", "four");
    newFindbugs.newRule("ABC").setName("ABC").setHtmlDescription("ABC");
    newFindbugs.done();

    RuleDefinitions.Repository findbugs = context.repository("findbugs");
    assertThat(findbugs.rules()).hasSize(2);

    RuleDefinitions.Rule npeRule = findbugs.rule("NPE");
    assertThat(npeRule.key()).isEqualTo("NPE");
    assertThat(npeRule.name()).isEqualTo("Detect NPE");
    assertThat(npeRule.defaultSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(npeRule.htmlDescription()).isEqualTo("Detect <code>java.lang.NullPointerException</code>");
    assertThat(npeRule.tags()).containsOnly("one", "two", "three", "four");
    assertThat(npeRule.params()).isEmpty();
    assertThat(npeRule.metadata()).isEqualTo("/something");
    assertThat(npeRule.template()).isFalse();
    assertThat(npeRule.status()).isEqualTo(RuleDefinitions.Status.BETA);
    assertThat(npeRule.toString()).isEqualTo("[repository=findbugs, key=NPE]");
    assertThat(npeRule.repository()).isSameAs(findbugs);

    // test equals() and hashCode()
    RuleDefinitions.Rule otherRule = findbugs.rule("ABC");
    assertThat(npeRule).isEqualTo(npeRule).isNotEqualTo(otherRule).isNotEqualTo("NPE").isNotEqualTo(null);
    assertThat(npeRule.hashCode()).isEqualTo(npeRule.hashCode());
  }

  @Test
  public void define_rule_with_default_fields() {
    RuleDefinitions.NewRepository newFindbugs = context.newRepository("findbugs", "java");
    newFindbugs.newRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newFindbugs.done();

    RuleDefinitions.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.key()).isEqualTo("NPE");
    assertThat(rule.defaultSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(rule.params()).isEmpty();
    assertThat(rule.metadata()).isNull();
    assertThat(rule.status()).isEqualTo(RuleDefinitions.Status.READY);
    assertThat(rule.tags()).isEmpty();
  }

  @Test
  public void define_rule_parameters() {
    RuleDefinitions.NewRepository newFindbugs = context.newRepository("findbugs", "java");
    RuleDefinitions.NewRule newNpe = newFindbugs.newRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newNpe.newParam("level").setDefaultValue("LOW").setName("Level").setDescription("The level").setType(RuleParamType.INTEGER);
    newNpe.newParam("effort");
    newFindbugs.done();

    RuleDefinitions.Rule rule = context.repository("findbugs").rule("NPE");
    assertThat(rule.params()).hasSize(2);

    RuleDefinitions.Param level = rule.param("level");
    assertThat(level.key()).isEqualTo("level");
    assertThat(level.name()).isEqualTo("Level");
    assertThat(level.description()).isEqualTo("The level");
    assertThat(level.defaultValue()).isEqualTo("LOW");
    assertThat(level.type()).isEqualTo(RuleParamType.INTEGER);

    RuleDefinitions.Param effort = rule.param("effort");
    assertThat(effort.key()).isEqualTo("effort").isEqualTo(effort.name());
    assertThat(effort.description()).isNull();
    assertThat(effort.defaultValue()).isNull();
    assertThat(effort.type()).isEqualTo(RuleParamType.STRING);

    // test equals() and hashCode()
    assertThat(level).isEqualTo(level).isNotEqualTo(effort).isNotEqualTo("level").isNotEqualTo(null);
    assertThat(level.hashCode()).isEqualTo(level.hashCode());
  }

  @Test
  public void extend_repository() {
    assertThat(context.extendedRepositories()).isEmpty();

    // for example fb-contrib
    RuleDefinitions.NewExtendedRepository newFindbugs = context.extendRepository("findbugs", "java");
    newFindbugs.newRule("NPE").setName("NPE").setHtmlDescription("NPE");
    newFindbugs.done();

    assertThat(context.repositories()).isEmpty();
    assertThat(context.extendedRepositories()).hasSize(1);
    assertThat(context.extendedRepositories("other")).isEmpty();
    assertThat(context.extendedRepositories("findbugs")).hasSize(1);

    RuleDefinitions.ExtendedRepository findbugs = context.extendedRepositories("findbugs").get(0);
    assertThat(findbugs.language()).isEqualTo("java");
    assertThat(findbugs.rule("NPE")).isNotNull();
  }

  @Test
  public void fail_if_duplicated_repo_keys() {
    context.newRepository("findbugs", "java").done();
    try {
      context.newRepository("findbugs", "whatever_the_language").done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The rule repository 'findbugs' is defined several times");
    }
  }

  @Test
  public void warning_if_duplicated_rule_keys() {
    RuleDefinitions.NewRepository findbugs = context.newRepository("findbugs", "java");
    findbugs.newRule("NPE");
    findbugs.newRule("NPE");
    // do not fail as long as http://jira.codehaus.org/browse/SONARJAVA-428 is not fixed
  }

  @Test
  public void fail_if_duplicated_rule_param_keys() {
    RuleDefinitions.NewRule rule = context.newRepository("findbugs", "java").newRule("NPE");
    rule.newParam("level");
    try {
      rule.newParam("level");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The parameter 'level' is declared several times on the rule [repository=findbugs, key=NPE]");
    }
  }

  @Test
  public void fail_if_blank_rule_name() {
    RuleDefinitions.NewRepository newRepository = context.newRepository("findbugs", "java");
    newRepository.newRule("NPE").setName(null).setHtmlDescription("NPE");
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Name of rule [repository=findbugs, key=NPE] is empty");
    }
  }

  @Test
  public void fail_if_bad_rule_tag() {
    try {
      // whitespaces are not allowed in tags
      context.newRepository("findbugs", "java").newRule("NPE").setTags("coding style");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Whitespaces are not allowed in rule tags: 'coding style'");
    }
  }

  @Test
  public void fail_if_blank_rule_html_description() {
    RuleDefinitions.NewRepository newRepository = context.newRepository("findbugs", "java");
    newRepository.newRule("NPE").setName("NPE").setHtmlDescription(null);
    try {
      newRepository.done();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("HTML description of rule [repository=findbugs, key=NPE] is empty");
    }
  }

  @Test
  public void fail_if_bad_rule_severity() {
    try {
      context.newRepository("findbugs", "java").newRule("NPE").setDefaultSeverity("VERY HIGH");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Default severity of rule [repository=findbugs, key=NPE] is not correct: VERY HIGH");
    }
  }
}
