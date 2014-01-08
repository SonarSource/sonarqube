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
    assertThat(context.getRepositories()).isEmpty();

    RuleDefinitions.NewRepository findbugs = context.newRepository("findbugs", "java")
        .setName("Findbugs");
    RuleDefinitions.NewRepository checkstyle = context.newRepository("checkstyle", "java");

    assertThat(findbugs).isNotNull();
    assertThat(findbugs.key()).isEqualTo("findbugs");
    assertThat(findbugs.language()).isEqualTo("java");
    assertThat(findbugs.name()).isEqualTo("Findbugs");
    assertThat(findbugs.getRules()).isEmpty();

    assertThat(context.getRepositories()).hasSize(2);
    assertThat(context.getRepository("findbugs")).isSameAs(findbugs);
    assertThat(context.getRepository("unknown")).isNull();

    // test equals() and hashCode()
    assertThat(findbugs).isEqualTo(findbugs).isNotEqualTo(checkstyle).isNotEqualTo("findbugs");
    assertThat(findbugs.hashCode()).isEqualTo(findbugs.hashCode());
  }

  @Test
  public void default_repository_name_is_key() {
    RuleDefinitions.NewRepository findbugs = context.newRepository("findbugs", "java");
    assertThat(findbugs.name()).isEqualTo(findbugs.key()).isEqualTo("findbugs");
  }

  @Test
  public void define_rules() {
    RuleDefinitions.NewRepository findbugs = context.newRepository("findbugs", "java");
    findbugs.newRule("NPE")
        .setName("Detect NPE")
        .setHtmlDescription("Detect <code>java.lang.NullPointerException</code>")
        .setSeverity(Severity.BLOCKER)
        .setMetadata("/something")
        .setTags("valuable", "bug");
    findbugs.newRule("ABC");

    assertThat(findbugs.getRules()).hasSize(2);

    RuleDefinitions.NewRule npeRule = findbugs.getRule("NPE");
    assertThat(npeRule.key()).isEqualTo("NPE");
    assertThat(npeRule.name()).isEqualTo("Detect NPE");
    assertThat(npeRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(npeRule.htmlDescription()).isEqualTo("Detect <code>java.lang.NullPointerException</code>");
    assertThat(npeRule.tags()).containsOnly("valuable", "bug");
    assertThat(npeRule.getParams()).isEmpty();
    assertThat(npeRule.metadata()).isEqualTo("/something");

    // test equals() and hashCode()
    RuleDefinitions.NewRule otherRule = findbugs.getRule("ABC");
    assertThat(npeRule).isEqualTo(npeRule).isNotEqualTo(otherRule).isNotEqualTo("NPE");
    assertThat(npeRule.hashCode()).isEqualTo(npeRule.hashCode());
  }

  @Test
  public void define_rule_with_default_fields() {
    context.newRepository("findbugs", "java").newRule("NPE");

    RuleDefinitions.NewRule rule = context.getRepository("findbugs").getRule("NPE");
    assertThat(rule.key()).isEqualTo("NPE");
    assertThat(rule.name()).isEqualTo("NPE");
    assertThat(rule.severity()).isEqualTo(Severity.MAJOR);
    assertThat(rule.htmlDescription()).isNull();
    assertThat(rule.getParams()).isEmpty();
    assertThat(rule.metadata()).isNull();
    assertThat(rule.tags()).isEmpty();
  }

  @Test
  public void define_rule_parameters() {
    context.newRepository("findbugs", "java")
        .newRule("NPE")
        .newParam("level").setDefaultValue("LOW").setName("Level").setDescription("The level")
        .rule()
        .newParam("effort");

    RuleDefinitions.NewRule rule = context.getRepository("findbugs").getRule("NPE");
    assertThat(rule.getParams()).hasSize(2);

    RuleDefinitions.NewParam level = rule.getParam("level");
    assertThat(level.key()).isEqualTo("level");
    assertThat(level.name()).isEqualTo("Level");
    assertThat(level.description()).isEqualTo("The level");
    assertThat(level.defaultValue()).isEqualTo("LOW");

    RuleDefinitions.NewParam effort = rule.getParam("effort");
    assertThat(effort.key()).isEqualTo("effort").isEqualTo(effort.name());
    assertThat(effort.description()).isNull();
    assertThat(effort.defaultValue()).isNull();

    // test equals() and hashCode()
    assertThat(level).isEqualTo(level).isNotEqualTo(effort).isNotEqualTo("level");
    assertThat(level.hashCode()).isEqualTo(level.hashCode());
  }

  @Test
  public void extend_repository() {
    assertThat(context.getExtendedRepositories()).isEmpty();

    // for example fb-contrib
    context.extendRepository("findbugs").newRule("NPE");

    assertThat(context.getRepositories()).isEmpty();
    assertThat(context.getExtendedRepositories()).hasSize(1);
    assertThat(context.getExtendedRepositories("other")).isEmpty();
    assertThat(context.getExtendedRepositories("findbugs")).hasSize(1);

    RuleDefinitions.ExtendedRepository findbugs = context.getExtendedRepositories("findbugs").get(0);
    assertThat(findbugs.getRule("NPE")).isNotNull();
  }

  @Test
  public void fail_if_duplicated_repo_keys() {
    context.newRepository("findbugs", "java");
    try {
      context.newRepository("findbugs", "whatever_the_language");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The rule repository 'findbugs' is defined several times");
    }
  }

  @Test
  public void fail_if_duplicated_rule_keys() {
    RuleDefinitions.NewRepository findbugs = context.newRepository("findbugs", "java");
    findbugs.newRule("NPE");
    try {
      findbugs.newRule("NPE");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The rule 'NPE' of repository 'findbugs' is declared several times");
    }
  }

  @Test
  public void fail_if_duplicated_rule_param_keys() {
    RuleDefinitions.NewRule rule = context.newRepository("findbugs", "java").newRule("NPE");
    rule.newParam("level");
    try {
      rule.newParam("level");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The parameter 'NPE' is declared several times on the rule [repository=findbugs, key=NPE]");
    }
  }

  @Test
  public void fail_if_blank_rule_name() {
    try {
      context.newRepository("findbugs", "java").newRule("NPE").setName(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Name of rule [repository=findbugs, key=NPE] is blank");
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
    try {
      context.newRepository("findbugs", "java").newRule("NPE").setHtmlDescription(null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("HTML description of rule [repository=findbugs, key=NPE] is blank");
    }
  }

  @Test
  public void fail_if_bad_rule_severity() {
    try {
      context.newRepository("findbugs", "java").newRule("NPE").setSeverity("VERY HIGH");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Severity of rule [repository=findbugs, key=NPE] is not correct: VERY HIGH");
    }
  }
}
