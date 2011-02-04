/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.pmd.xml.PmdProperty;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.test.TestUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class PmdProfileExporterTest {

  private PmdProfileExporter exporter = new PmdProfileExporter();

  @Test
  public void testExportProfile() throws IOException, SAXException {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    PmdRuleRepository repository = new PmdRuleRepository(fileSystem, new XMLRuleParser());
    List<Rule> rules = repository.createRules();

    RuleFinder ruleFinder = new FakeRuleFinder(rules);
    PmdProfileImporter importer = new PmdProfileImporter(ruleFinder);
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    RulesProfile rulesProfile = importer.importProfile(reader, ValidationMessages.create());

    StringWriter xmlOutput = new StringWriter();
    exporter.exportProfile(rulesProfile, xmlOutput);
    assertEquals(TestUtils.getResourceContent("/org/sonar/plugins/pmd/export_simple.xml"), StringUtils.remove(xmlOutput.toString(), '\r'));
  }

  @Test
  public void testExportXPathRule() {
    StringWriter xmlOutput = new StringWriter();
    RulesProfile profile = RulesProfile.create();
    Rule xpathTemplate = Rule.create(PmdConstants.REPOSITORY_KEY, "MyOwnRule", "This is my own xpath rule.")
        .setConfigKey(PmdConstants.XPATH_CLASS).setPluginName(PmdConstants.REPOSITORY_KEY);
    xpathTemplate.createParameter(PmdConstants.XPATH_EXPRESSION_PARAM);
    xpathTemplate.createParameter(PmdConstants.XPATH_MESSAGE_PARAM);
    ActiveRule xpath = profile.activateRule(xpathTemplate, null);
    xpath.setParameter(PmdConstants.XPATH_EXPRESSION_PARAM, "//FieldDeclaration");
    xpath.setParameter(PmdConstants.XPATH_MESSAGE_PARAM, "This is bad");
    exporter.exportProfile(profile, xmlOutput);
    assertEquals(TestUtils.getResourceContent("/org/sonar/plugins/pmd/export_xpath_rules.xml"),
        StringUtils.remove(xmlOutput.toString(), '\r'));
  }

  @Test(expected = SonarException.class)
  public void shouldFailIfMessageNotProvidedForXPathRule() {
    String xpathExpression = "xpathExpression";

    PmdRule rule = new PmdRule(PmdConstants.XPATH_CLASS);
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_EXPRESSION_PARAM, xpathExpression));
    rule.setName("MyOwnRule");

    exporter.processXPathRule("xpathKey", rule);
  }

  @Test(expected = SonarException.class)
  public void shouldFailIfXPathNotProvidedForXPathRule() {
    String message = "This is bad";

    PmdRule rule = new PmdRule(PmdConstants.XPATH_CLASS);
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_MESSAGE_PARAM, message));
    rule.setName("MyOwnRule");

    exporter.processXPathRule("xpathKey", rule);
  }

  @Test
  public void testProcessingXPathRule() {
    String message = "This is bad";
    String xpathExpression = "xpathExpression";

    PmdRule rule = new PmdRule(PmdConstants.XPATH_CLASS);
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_EXPRESSION_PARAM, xpathExpression));
    rule.addProperty(new PmdProperty(PmdConstants.XPATH_MESSAGE_PARAM, message));
    rule.setName("MyOwnRule");

    exporter.processXPathRule("xpathKey", rule);

    assertThat(rule.getMessage(), is(message));
    assertThat(rule.getRef(), is(nullValue()));
    assertThat(rule.getClazz(), is(PmdConstants.XPATH_CLASS));
    assertThat(rule.getProperty(PmdConstants.XPATH_MESSAGE_PARAM), is(nullValue()));
    assertThat(rule.getName(), is("xpathKey"));
    assertThat(rule.getProperty(PmdConstants.XPATH_EXPRESSION_PARAM).getValue(), is(xpathExpression));
  }

  private static class FakeRuleFinder implements RuleFinder {

    private List<Rule> rules;

    public FakeRuleFinder(List<Rule> rules) {
      this.rules = rules;
    }

    public Rule findById(int ruleId) {
      throw new UnsupportedOperationException();
    }

    public Rule findByKey(String repositoryKey, String key) {
      throw new UnsupportedOperationException();
    }

    public Collection<Rule> findAll(RuleQuery query) {
      throw new UnsupportedOperationException();
    }

    public Rule find(RuleQuery query) {
      for (Rule rule : rules) {
        if (query.getConfigKey().equals(rule.getConfigKey())) {
          rule.setPluginName(PmdConstants.REPOSITORY_KEY);
          return rule;
        }
      }
      return null;
    }
  }
}
