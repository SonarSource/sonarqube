/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.sonar.api.PropertyType;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Cardinality;
import org.sonar.check.Status;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class XMLRuleParserTest {

  @Test
  public void should_parse_xml() throws Exception {
    File file = new File(getClass().getResource("/org/sonar/api/rules/XMLRuleParserTest/rules.xml").toURI());
    List<Rule> rules = new XMLRuleParser().parse(file);
    assertThat(rules.size(), is(2));

    Rule rule = rules.get(0);
    assertThat(rule.getName(), is("Local Variable Name"));
    assertThat(rule.getDescription(), is("Checks that local, non-final variable names conform to a format specified by the format property."));
    assertThat(rule.getSeverity(), Is.is(RulePriority.BLOCKER));
    assertThat(rule.getCardinality(), Is.is(Cardinality.MULTIPLE));
    assertThat(rule.getConfigKey(), is("Checker/TreeWalker/LocalVariableName"));
    assertThat(rule.getStatus(), nullValue());

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
  public void should_fail_if_missing_rule_key() {
    new XMLRuleParser().parse(new StringReader("<rules><rule><name>Foo</name></rule></rules>"));
  }

  @Test
  public void should_rule_name_should_be_optional() {
    List<Rule> rules = new XMLRuleParser().parse(new StringReader("<rules><rule><key>foo</key></rule></rules>"));
    assertThat(rules.get(0).getName(), nullValue());
  }

  @Test(expected = SonarException.class)
  public void should_fail_if_missing_property_key() {
    new XMLRuleParser().parse(new StringReader("<rules><rule><key>foo</key><name>Foo</name><param></param></rule></rules>"));
  }

  @Test
  public void should_read_rule_parameter_type() {
    assertThat(typeOf("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>STRING</type></param></rule></rules>")).isEqualTo(PropertyType.STRING.name());
    assertThat(typeOf("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>INTEGER</type></param></rule></rules>")).isEqualTo(PropertyType.INTEGER.name());
    assertThat(typeOf("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>s</type></param></rule></rules>")).isEqualTo(PropertyType.STRING.name());
    assertThat(typeOf("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>s{}</type></param></rule></rules>")).isEqualTo("s{}");
    assertThat(typeOf("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>i{}</type></param></rule></rules>")).isEqualTo("i{}");
    assertThat(typeOf("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>s[foo,bar]</type></param></rule></rules>")).isEqualTo("s[foo,bar]");
  }

  static String typeOf(String xml) {
    return getOnlyElement(new XMLRuleParser().parse(new StringReader(xml))).getParam("key").getType();
  }

  @Test(expected = SonarException.class)
  public void should_fail_on_invalid_rule_parameter_type() {
    new XMLRuleParser().parse(new StringReader("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>INVALID</type></param></rule></rules>"));
  }

  @Test
  public void test_utf8_encoding() {
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
  public void should_support_deprecated_format() {
    // the deprecated format uses some attributes instead of nodes
    List<Rule> rules = new XMLRuleParser().parse(getClass().getResourceAsStream("/org/sonar/api/rules/XMLRuleParserTest/deprecated.xml"));
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getSeverity(), Is.is(RulePriority.CRITICAL));
    assertThat(rule.getKey(), is("org.sonar.it.checkstyle.MethodsCountCheck"));
    assertThat(rule.getParam("minMethodsCount"), not(nullValue()));
  }

  @Test
  public void should_read_rule_status() {
    List<Rule> rules = new XMLRuleParser().parse(new StringReader(
        "<rules>"+
            "<rule><key>foo</key><status>READY</status></rule>"+
            "<rule><key>foo</key><status>BETA</status></rule>"+
            "<rule><key>foo</key><status>DEPRECATED</status></rule>"+
            "</rules>"));
    assertThat(rules.get(0).getStatus(), is(Status.READY.name()));
    assertThat(rules.get(1).getStatus(), is(Status.BETA.name()));
    assertThat(rules.get(2).getStatus(), is(Status.DEPRECATED.name()));
  }
}
