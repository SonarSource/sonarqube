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
package org.sonar.api.server.rule;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.Severity;
import org.sonar.api.rule.RuleStatus;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import static org.fest.assertions.Assertions.assertThat;

public class RuleDefinitionsFromXmlTest {

  @org.junit.Rule
  public final ExpectedException thrown = ExpectedException.none();

  private RuleDefinitions.Repository load(Reader reader) {
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    RuleDefinitions.NewRepository newRepository = context.newRepository("squid", "java");
    new RuleDefinitionsFromXml().loadRules(newRepository, reader);
    newRepository.done();
    return context.repository("squid");
  }

  @Test
  public void should_parse_xml() throws Exception {
    InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/server/rule/RuleDefinitionsFromXmlTest/rules.xml"), Charsets.UTF_8.name());
    RuleDefinitions.Repository repository = load(reader);
    assertThat(repository.rules()).hasSize(2);

    RuleDefinitions.Rule rule = repository.rule("complete");
    assertThat(rule.key()).isEqualTo("complete");
    assertThat(rule.name()).isEqualTo("Complete");
    assertThat(rule.htmlDescription()).isEqualTo("Description of Complete");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.template()).isTrue();
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.engineKey()).isEqualTo("Checker/TreeWalker/LocalVariableName");

    assertThat(rule.params()).hasSize(2);
    RuleDefinitions.Param ignore = rule.param("ignore");
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
  }

  @Test
  public void should_fail_if_missing_rule_key() {
    thrown.expect(IllegalStateException.class);
    load(new StringReader("<rules><rule><name>Foo</name></rule></rules>"));
  }

  @Test
  public void should_fail_if_missing_property_key() {
    thrown.expect(IllegalStateException.class);
    load(new StringReader("<rules><rule><key>foo</key><name>Foo</name><param></param></rule></rules>"));
  }

  @Test
  public void should_fail_on_invalid_rule_parameter_type() {
    thrown.expect(IllegalStateException.class);
    load(new StringReader("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>INVALID</type></param></rule></rules>"));
  }

  @Test
  public void test_utf8_encoding() throws UnsupportedEncodingException {
    InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/server/rule/RuleDefinitionsFromXmlTest/utf8.xml"), Charsets.UTF_8.name());
    RuleDefinitions.Repository repository = load(reader);

    assertThat(repository.rules()).hasSize(1);
    RuleDefinitions.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.naming.LocalVariableNameCheck");
    assertThat(rule.name()).isEqualTo("M & M");
    assertThat(rule.htmlDescription().charAt(0)).isEqualTo('\u00E9');
    assertThat(rule.htmlDescription().charAt(1)).isEqualTo('\u00E0');
    assertThat(rule.htmlDescription().charAt(2)).isEqualTo('\u0026');
  }

  @Test
  public void should_support_deprecated_format() throws UnsupportedEncodingException {
    // the deprecated format uses some attributes instead of nodes
    InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/server/rule/RuleDefinitionsFromXmlTest/deprecated.xml"), Charsets.UTF_8.name());
    RuleDefinitions.Repository repository = load(reader);

    assertThat(repository.rules()).hasSize(1);
    RuleDefinitions.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("org.sonar.it.checkstyle.MethodsCountCheck");
    assertThat(rule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(rule.htmlDescription()).isEqualTo("Count methods");
    assertThat(rule.param("minMethodsCount")).isNotNull();
  }
}
