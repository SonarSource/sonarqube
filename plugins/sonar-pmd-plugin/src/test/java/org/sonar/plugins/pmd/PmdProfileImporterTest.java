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
package org.sonar.plugins.pmd;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.pmd.xml.PmdRuleset;
import org.sonar.test.TestUtils;

public class PmdProfileImporterTest {

  private PmdProfileImporter importer;
  private ValidationMessages messages;

  @Before
  public void before() {
    messages = ValidationMessages.create();
    RuleFinder finder = createRuleFinder();
    importer = new PmdProfileImporter(finder);
  }

  @Test
  public void testBuildPmdRuleset() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    PmdRuleset pmdRuleset = importer.parsePmdRuleset(reader, messages);
    assertThat(pmdRuleset.getPmdRules().size(), is(3));
  }

  @Test
  public void testImportingSimpleProfile() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules().size(), is(3));
    assertNotNull(profile.getActiveRuleByConfigKey("pmd", "rulesets/coupling.xml/ExcessiveImports"));
    assertNotNull(profile.getActiveRuleByConfigKey("pmd", "rulesets/design.xml/UseNotifyAllInsteadOfNotify"));
    assertThat(messages.hasErrors(), is(false));
  }

  @Test
  public void testImportingProfileWithXPathRule() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/export_xpath_rules.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules().size(), is(0));
    assertThat(messages.hasWarnings(), is(true));
  }

  @Test
  public void testImportingParameters() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/coupling.xml/ExcessiveImports");
    assertThat(activeRule.getActiveRuleParams().size(), is(1));
    assertThat(activeRule.getParameter("max"), is("30"));
  }

  @Test
  public void testImportingDefaultPriority() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/coupling.xml/ExcessiveImports");
    assertThat(activeRule.getPriority(), is(RulePriority.BLOCKER)); // reuse the rule default priority
  }

  @Test
  public void testImportingPriority() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/design.xml/UseNotifyAllInsteadOfNotify");
    assertThat(activeRule.getPriority(), is(RulePriority.MINOR));

    activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/coupling.xml/CouplingBetweenObjects");
    assertThat(activeRule.getPriority(), is(RulePriority.CRITICAL));
  }

  @Test
  public void testImportingPmdConfigurationWithUnknownNodes() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/complex-with-unknown-nodes.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules().size(), is(3));
  }

  @Test
  public void testUnsupportedProperty() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule check = profile.getActiveRuleByConfigKey("pmd", "rulesets/coupling.xml/CouplingBetweenObjects");
    assertThat(check.getParameter("threshold"), nullValue());
    assertThat(messages.getWarnings().size(), is(1));
  }

  @Test
  public void testUnvalidXML() {
    Reader reader = new StringReader("not xml");
    importer.importProfile(reader, messages);
    assertThat(messages.getErrors().size(), is(1));
  }

  @Test
  public void testImportingUnknownRules() {
    Reader reader = new StringReader(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    importer = new PmdProfileImporter(mock(RuleFinder.class));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules().size(), is(0));
    assertThat(messages.getWarnings().size(), is(3));
  }

  private RuleFinder createRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find((RuleQuery) anyObject())).thenAnswer(new Answer<Rule>() {

      public Rule answer(InvocationOnMock iom) throws Throwable {
        RuleQuery query = (RuleQuery) iom.getArguments()[0];
        Rule rule = Rule.create(query.getRepositoryKey(), query.getConfigKey(), "Rule name - " + query.getConfigKey())
            .setConfigKey(query.getConfigKey()).setPriority(RulePriority.BLOCKER);
        if (rule.getConfigKey().equals("rulesets/coupling.xml/ExcessiveImports")) {
          rule.createParameter("max");
        }
        return rule;
      }
    });
    return ruleFinder;
  }
}
