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

import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.impl.server.RulesDefinitionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.ExceptionCauseMatcher.hasType;

public class RulesDefinitionXmlLoaderTest {

  @org.junit.Rule
  public final ExpectedException expectedException = ExpectedException.none();

  RulesDefinitionXmlLoader underTest = new RulesDefinitionXmlLoader();

  @Test
  public void parse_xml() {
    InputStream input = getClass().getResourceAsStream("RulesDefinitionXmlLoaderTest/rules.xml");
    RulesDefinition.Repository repository = load(input, StandardCharsets.UTF_8.name());
    assertThat(repository.rules()).hasSize(2);

    RulesDefinition.Rule rule = repository.rule("complete");
    assertThat(rule.key()).isEqualTo("complete");
    assertThat(rule.name()).isEqualTo("Complete");
    assertThat(rule.htmlDescription()).isEqualTo("Description of Complete");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.template()).isTrue();
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.internalKey()).isEqualTo("Checker/TreeWalker/LocalVariableName");
    assertThat(rule.type()).isEqualTo(RuleType.BUG);
    assertThat(rule.tags()).containsOnly("misra", "spring");

    assertThat(rule.params()).hasSize(2);
    RulesDefinition.Param ignore = rule.param("ignore");
    assertThat(ignore.key()).isEqualTo("ignore");
    assertThat(ignore.description()).isEqualTo("Ignore ?");
    assertThat(ignore.defaultValue()).isEqualTo("false");

    rule = repository.rule("minimal");
    assertThat(rule.key()).isEqualTo("minimal");
    assertThat(rule.name()).isEqualTo("Minimal");
    assertThat(rule.htmlDescription()).isEqualTo("Description of Minimal");
    assertThat(rule.params()).isEmpty();
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.severity()).isEqualTo(Severity.MAJOR);
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
  }

  @Test
  public void fail_if_missing_rule_key() {
    expectedException.expect(IllegalStateException.class);
    load(IOUtils.toInputStream("<rules><rule><name>Foo</name></rule></rules>"), StandardCharsets.UTF_8.name());
  }

  @Test
  public void fail_if_missing_property_key() {
    expectedException.expect(IllegalStateException.class);
    load(IOUtils.toInputStream("<rules><rule><key>foo</key><name>Foo</name><param></param></rule></rules>"), StandardCharsets.UTF_8.name());
  }

  @Test
  public void fail_on_invalid_rule_parameter_type() {
    expectedException.expect(IllegalStateException.class);
    load(IOUtils.toInputStream("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>INVALID</type></param></rule></rules>"), StandardCharsets.UTF_8.name());
  }

  @Test
  public void fail_if_invalid_xml() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("XML is not valid");

    InputStream input = getClass().getResourceAsStream("RulesDefinitionXmlLoaderTest/invalid.xml");
    load(input, StandardCharsets.UTF_8.name());
  }

  @Test
  public void test_utf8_encoding() throws UnsupportedEncodingException {
    InputStream input = getClass().getResourceAsStream("RulesDefinitionXmlLoaderTest/utf8.xml");
    RulesDefinition.Repository repository = load(input, StandardCharsets.UTF_8.name());

    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.naming.LocalVariableNameCheck");
    assertThat(rule.name()).isEqualTo("M & M");
    assertThat(rule.htmlDescription().charAt(0)).isEqualTo('\u00E9');
    assertThat(rule.htmlDescription().charAt(1)).isEqualTo('\u00E0');
    assertThat(rule.htmlDescription().charAt(2)).isEqualTo('\u0026');
  }

  @Test
  public void support_deprecated_format() {
    // the deprecated format uses some attributes instead of nodes
    InputStream input = getClass().getResourceAsStream("RulesDefinitionXmlLoaderTest/deprecated.xml");
    RulesDefinition.Repository repository = load(input, StandardCharsets.UTF_8.name());

    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("org.sonar.it.checkstyle.MethodsCountCheck");
    assertThat(rule.internalKey()).isEqualTo("Checker/TreeWalker/org.sonar.it.checkstyle.MethodsCountCheck");
    assertThat(rule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(rule.htmlDescription()).isEqualTo("Count methods");
    assertThat(rule.param("minMethodsCount")).isNotNull();
  }

  @Test
  public void test_linear_remediation_function() throws Exception {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +

      "    <gapDescription>lines</gapDescription>" +
      "    <remediationFunction>LINEAR</remediationFunction>" +
      "    <remediationFunctionGapMultiplier>2d 3h</remediationFunctionGapMultiplier>" +
      "  </rule>" +
      "</rules>";
    RulesDefinition.Rule rule = load(xml).rule("1");
    assertThat(rule.gapDescription()).isEqualTo("lines");
    DebtRemediationFunction function = rule.debtRemediationFunction();
    assertThat(function).isNotNull();
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(function.gapMultiplier()).isEqualTo("2d3h");
    assertThat(function.baseEffort()).isNull();
  }

  @Test
  public void test_linear_with_offset_remediation_function() {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +

      "    <effortToFixDescription>lines</effortToFixDescription>" +
      "    <remediationFunction>LINEAR_OFFSET</remediationFunction>" +
      "    <remediationFunctionGapMultiplier>2d 3h</remediationFunctionGapMultiplier>" +
      "    <remediationFunctionBaseEffort>5min</remediationFunctionBaseEffort>" +
      "  </rule>" +
      "</rules>";
    RulesDefinition.Rule rule = load(xml).rule("1");
    assertThat(rule.gapDescription()).isEqualTo("lines");
    DebtRemediationFunction function = rule.debtRemediationFunction();
    assertThat(function).isNotNull();
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(function.gapMultiplier()).isEqualTo("2d3h");
    assertThat(function.baseEffort()).isEqualTo("5min");
  }

  @Test
  public void test_constant_remediation_function() {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +
      "    <remediationFunction>CONSTANT_ISSUE</remediationFunction>" +
      "    <remediationFunctionBaseEffort>5min</remediationFunctionBaseEffort>" +
      "  </rule>" +
      "</rules>";
    RulesDefinition.Rule rule = load(xml).rule("1");
    DebtRemediationFunction function = rule.debtRemediationFunction();
    assertThat(function).isNotNull();
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(function.gapMultiplier()).isNull();
    assertThat(function.baseEffort()).isEqualTo("5min");
  }

  @Test
  public void fail_if_invalid_remediation_function() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to load the rule with key [squid:1]");
    expectedException.expectCause(hasType(IllegalArgumentException.class)
      .andMessage("No enum constant org.sonar.api.server.debt.DebtRemediationFunction.Type.UNKNOWN"));

    load("" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +
      "    <remediationFunction>UNKNOWN</remediationFunction>" +
      "  </rule>" +
      "</rules>");
  }

  @Test
  public void ignore_deprecated_sqale_characteristic() {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +
      "    <effortToFixDescription>lines</effortToFixDescription>" +
      "    <debtSubCharacteristic>BUG</debtSubCharacteristic>" +
      "    <remediationFunction>LINEAR_OFFSET</remediationFunction>" +
      "    <remediationFunctionGapMultiplier>2d 3h</remediationFunctionGapMultiplier>" +
      "    <remediationFunctionBaseEffort>5min</remediationFunctionBaseEffort>" +
      "  </rule>" +
      "</rules>";
    RulesDefinition.Rule rule = load(xml).rule("1");
    assertThat(rule.debtSubCharacteristic()).isNull();
  }

  @Test
  public void markdown_description() {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +
      "    <descriptionFormat>MARKDOWN</descriptionFormat>" +
      "  </rule>" +
      "</rules>";
    RulesDefinition.Rule rule = load(xml).rule("1");
    assertThat(rule.markdownDescription()).isEqualTo("Desc");
    assertThat(rule.htmlDescription()).isNull();
  }

  @Test
  public void fail_if_unsupported_description_format() {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +
      "    <descriptionFormat>UNKNOWN</descriptionFormat>" +
      "  </rule>" +
      "</rules>";

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to load the rule with key [squid:1]");
    expectedException.expectCause(hasType(IllegalArgumentException.class)
      .andMessage("No enum constant org.sonar.api.server.rule.RulesDefinitionXmlLoader.DescriptionFormat.UNKNOWN"));

    load(xml).rule("1");
  }

  @Test
  public void test_deprecated_remediation_function() {
    String xml = "" +
      "<rules>" +
      "  <rule>" +
      "    <key>1</key>" +
      "    <name>One</name>" +
      "    <description>Desc</description>" +
      "    <effortToFixDescription>lines</effortToFixDescription>" +
      "    <debtRemediationFunction>LINEAR_OFFSET</debtRemediationFunction>" +
      "    <debtRemediationFunctionCoefficient>2d 3h</debtRemediationFunctionCoefficient>" +
      "    <debtRemediationFunctionOffset>5min</debtRemediationFunctionOffset>" +
      "  </rule>" +
      "</rules>";
    RulesDefinition.Rule rule = load(xml).rule("1");
    assertThat(rule.gapDescription()).isEqualTo("lines");
    DebtRemediationFunction function = rule.debtRemediationFunction();
    assertThat(function).isNotNull();
    assertThat(function.type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(function.gapMultiplier()).isEqualTo("2d3h");
    assertThat(function.baseEffort()).isEqualTo("5min");
  }

  private RulesDefinition.Repository load(InputStream input, String encoding) {
    RulesDefinition.Context context = new RulesDefinitionContext();
    RulesDefinition.NewRepository newRepository = context.createRepository("squid", "java");
    underTest.load(newRepository, input, encoding);
    newRepository.done();
    return context.repository("squid");
  }

  private RulesDefinition.Repository load(String xml) {
    RulesDefinition.Context context = new RulesDefinitionContext();
    RulesDefinition.NewRepository newRepository = context.createRepository("squid", "java");
    underTest.load(newRepository, new StringReader(xml));
    newRepository.done();
    return context.repository("squid");
  }
}
