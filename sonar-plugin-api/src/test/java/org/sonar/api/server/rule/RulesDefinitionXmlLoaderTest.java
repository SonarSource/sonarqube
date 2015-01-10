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

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesDefinitionXmlLoaderTest {

  @org.junit.Rule
  public final ExpectedException thrown = ExpectedException.none();

  private RulesDefinition.Repository load(InputStream input, String encoding) {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository newRepository = context.createRepository("squid", "java");
    new RulesDefinitionXmlLoader().load(newRepository, input, encoding);
    newRepository.done();
    return context.repository("squid");
  }

  @Test
  public void parse_xml() throws Exception {
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/server/rule/RulesDefinitionXmlLoaderTest/rules.xml");
    RulesDefinition.Repository repository = load(input, Charsets.UTF_8.name());
    assertThat(repository.rules()).hasSize(2);

    RulesDefinition.Rule rule = repository.rule("complete");
    assertThat(rule.key()).isEqualTo("complete");
    assertThat(rule.name()).isEqualTo("Complete");
    assertThat(rule.htmlDescription()).isEqualTo("Description of Complete");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.template()).isTrue();
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.internalKey()).isEqualTo("Checker/TreeWalker/LocalVariableName");
    assertThat(rule.tags()).containsOnly("style", "security");

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
  }

  @Test
  public void fail_if_missing_rule_key() {
    thrown.expect(IllegalStateException.class);
    load(IOUtils.toInputStream("<rules><rule><name>Foo</name></rule></rules>"), Charsets.UTF_8.name());
  }

  @Test
  public void fail_if_missing_property_key() {
    thrown.expect(IllegalStateException.class);
    load(IOUtils.toInputStream("<rules><rule><key>foo</key><name>Foo</name><param></param></rule></rules>"), Charsets.UTF_8.name());
  }

  @Test
  public void fail_on_invalid_rule_parameter_type() {
    thrown.expect(IllegalStateException.class);
    load(IOUtils.toInputStream("<rules><rule><key>foo</key><name>Foo</name><param><key>key</key><type>INVALID</type></param></rule></rules>"), Charsets.UTF_8.name());
  }

  @Test
  public void fail_if_invalid_xml() throws UnsupportedEncodingException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("XML is not valid");

    InputStream input = getClass().getResourceAsStream("/org/sonar/api/server/rule/RulesDefinitionXmlLoaderTest/invalid.xml");
    load(input, Charsets.UTF_8.name());
  }

  @Test
  public void test_utf8_encoding() throws UnsupportedEncodingException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/server/rule/RulesDefinitionXmlLoaderTest/utf8.xml");
    RulesDefinition.Repository repository = load(input, Charsets.UTF_8.name());

    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.naming.LocalVariableNameCheck");
    assertThat(rule.name()).isEqualTo("M & M");
    assertThat(rule.htmlDescription().charAt(0)).isEqualTo('\u00E9');
    assertThat(rule.htmlDescription().charAt(1)).isEqualTo('\u00E0');
    assertThat(rule.htmlDescription().charAt(2)).isEqualTo('\u0026');
  }

  @Test
  public void support_deprecated_format() throws UnsupportedEncodingException {
    // the deprecated format uses some attributes instead of nodes
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/server/rule/RulesDefinitionXmlLoaderTest/deprecated.xml");
    RulesDefinition.Repository repository = load(input, Charsets.UTF_8.name());

    assertThat(repository.rules()).hasSize(1);
    RulesDefinition.Rule rule = repository.rules().get(0);
    assertThat(rule.key()).isEqualTo("org.sonar.it.checkstyle.MethodsCountCheck");
    assertThat(rule.internalKey()).isEqualTo("Checker/TreeWalker/org.sonar.it.checkstyle.MethodsCountCheck");
    assertThat(rule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(rule.htmlDescription()).isEqualTo("Count methods");
    assertThat(rule.param("minMethodsCount")).isNotNull();
  }
}
