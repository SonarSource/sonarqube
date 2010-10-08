/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rules;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.utils.SonarException;

import java.io.StringReader;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class XMLRuleParserTest {

  @Test
  public void parseXml() {
    List<Rule> rules = new XMLRuleParser().parse(getClass().getResourceAsStream("/org/sonar/api/rules/XMLRuleParserTest/rules.xml"));
    assertThat(rules.size(), is(2));

    Rule rule = rules.get(0);
    assertThat(rule.getName(), is("Local Variable Name"));
    assertThat(rule.getDescription(), is("Checks that local, non-final variable names conform to a format specified by the format property."));
    assertThat(rule.getPriority(), Is.is(RulePriority.BLOCKER));
    assertThat(rule.getCardinality(), Is.is(Rule.Cardinality.MULTIPLE));
    assertThat(rule.getConfigKey(), is("Checker/TreeWalker/LocalVariableName"));

    assertThat(rule.getParams().size(), is(2));
    RuleParam prop = rule.getParam("ignore");
    assertThat(prop.getKey(), is("ignore"));
    assertThat(prop.getDescription(), is("Ignore ?"));
    assertThat(prop.getDefaultValue(), is("false"));

    Rule minimalRule = rules.get(1);
    assertThat(minimalRule.getKey(), is("com.puppycrawl.tools.checkstyle.checks.coding.MagicNumberCheck"));
    assertThat(minimalRule.getParams().size(), is(0));

  }

  @Test(expected = SonarException.class)
  public void failIfMissingRuleKey() {
    new XMLRuleParser().parse(new StringReader("<rules><rule><name>Foo</name></rule></rules>"));
  }

  @Test(expected = SonarException.class)
  public void failIfMissingPropertyKey() {
    new XMLRuleParser().parse(new StringReader("<rules><rule><key>foo</key><name>Foo</name><param></param></rule></rules>"));
  }

  @Test
  public void utf8Encoding() {
    List<Rule> rules = new XMLRuleParser().parse(getClass().getResourceAsStream("/org/sonar/api/rules/XMLRuleParserTest/utf8.xml"));
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is("com.puppycrawl.tools.checkstyle.checks.naming.LocalVariableNameCheck"));
    assertThat(rule.getName(), is("M & M"));
    assertThat(rule.getDescription().charAt(0), is('\u00E9'));
    assertThat(rule.getDescription().charAt(1), is('\u00E0'));
    assertThat(rule.getDescription().charAt(2), is('\u0026'));
  }

  @Test
  public void supportDeprecatedFormat() {
    // the deprecated format uses some attributes instead of nodes
    List<Rule> rules = new XMLRuleParser().parse(getClass().getResourceAsStream("/org/sonar/api/rules/XMLRuleParserTest/deprecated.xml"));
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getPriority(), Is.is(RulePriority.CRITICAL));
    assertThat(rule.getKey(), is("org.sonar.it.checkstyle.MethodsCountCheck"));
    assertThat(rule.getParam("minMethodsCount"), not(nullValue()));
  }
}
