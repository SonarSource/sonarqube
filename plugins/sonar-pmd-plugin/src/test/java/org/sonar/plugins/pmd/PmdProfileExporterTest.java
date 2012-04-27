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
package org.sonar.plugins.pmd;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.pmd.xml.PmdProperty;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.test.TestUtils;

import java.io.StringReader;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.MoreConditions.equalsIgnoreEOL;

public class PmdProfileExporterTest {
  PmdProfileExporter exporter = new PmdProfileExporter();

  @Test
  public void should_export_pmd_profile() {
    String importedXml = TestUtils.getResourceContent("/org/sonar/plugins/pmd/export_simple.xml");

    String exportedXml = exporter.exportProfile(PmdConstants.REPOSITORY_KEY, importProfile(importedXml));

    assertThat(exportedXml).satisfies(equalsIgnoreEOL(importedXml));
  }

  @Test
  public void should_export_empty_configuration_as_xml() {
    String exportedXml = exporter.exportProfile(PmdConstants.REPOSITORY_KEY, RulesProfile.create());

    assertThat(exportedXml).satisfies(equalsIgnoreEOL("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ruleset />"));
  }

  @Test
  public void should_export_xPath_rule() {
    Rule rule = Rule.create(PmdConstants.REPOSITORY_KEY, "MyOwnRule", "This is my own xpath rule.")
        .setConfigKey(PmdConstants.XPATH_CLASS)
        .setRepositoryKey(PmdConstants.REPOSITORY_KEY);
    rule.createParameter(PmdConstants.XPATH_EXPRESSION_PARAM);
    rule.createParameter(PmdConstants.XPATH_MESSAGE_PARAM);

    RulesProfile profile = RulesProfile.create();
    ActiveRule xpath = profile.activateRule(rule, null);
    xpath.setParameter(PmdConstants.XPATH_EXPRESSION_PARAM, "//FieldDeclaration");
    xpath.setParameter(PmdConstants.XPATH_MESSAGE_PARAM, "This is bad");

    String exportedXml = exporter.exportProfile(PmdConstants.REPOSITORY_KEY, profile);

    assertThat(exportedXml).satisfies(equalsIgnoreEOL(TestUtils.getResourceContent("/org/sonar/plugins/pmd/export_xpath_rules.xml")));
  }

  @Test(expected = SonarException.class)
  public void should_fail_if_message_not_provided_for_xPath_rule() {
    PmdRule rule = new PmdRule(PmdConstants.XPATH_CLASS);

    rule.addProperty(new PmdProperty(PmdConstants.XPATH_EXPRESSION_PARAM, "xpathExpression"));
    rule.setName("MyOwnRule");

    exporter.processXPathRule("xpathKey", rule);
  }

  @Test
  public void should_process_xPath_rule() {
    PmdRule rule = new PmdRule(PmdConstants.XPATH_CLASS);
    rule.setName("MyOwnRule");
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_EXPRESSION_PARAM, "xpathExpression"));
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_MESSAGE_PARAM, "message"));

    exporter.processXPathRule("xpathKey", rule);

    assertThat(rule.getMessage()).isEqualTo("message");
    assertThat(rule.getRef()).isNull();
    assertThat(rule.getClazz()).isEqualTo(PmdConstants.XPATH_CLASS);
    assertThat(rule.getProperty(PmdConstants.XPATH_MESSAGE_PARAM)).isNull();
    assertThat(rule.getName()).isEqualTo("xpathKey");
    assertThat(rule.getProperty(PmdConstants.XPATH_EXPRESSION_PARAM).getValue()).isEqualTo("xpathExpression");
  }

  @Test(expected = SonarException.class)
  public void should_fail_if_xPath_not_provided() {
    PmdRule rule = new PmdRule(PmdConstants.XPATH_CLASS);
    rule.setName("MyOwnRule");
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_MESSAGE_PARAM, "This is bad"));

    exporter.processXPathRule("xpathKey", rule);
  }

  static RulesProfile importProfile(String configuration) {
    PmdRuleRepository pmdRuleRepository = new PmdRuleRepository(mock(ServerFileSystem.class), new XMLRuleParser());
    RuleFinder ruleFinder = createRuleFinder(pmdRuleRepository.createRules());
    PmdProfileImporter importer = new PmdProfileImporter(ruleFinder);

    return importer.importProfile(new StringReader(configuration), ValidationMessages.create());
  }

  static RuleFinder createRuleFinder(final List<Rule> rules) {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find(any(RuleQuery.class))).then(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) {
        RuleQuery query = (RuleQuery) invocation.getArguments()[0];
        for (Rule rule : rules) {
          if (query.getConfigKey().equals(rule.getConfigKey())) {
            return rule.setRepositoryKey(PmdConstants.REPOSITORY_KEY);
          }
        }
        return null;
      }
    });
    return ruleFinder;
  }
}
