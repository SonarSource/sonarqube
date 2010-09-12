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

import com.thoughtworks.xstream.converters.ConversionException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.plugins.pmd.xml.Property;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.plugins.pmd.xml.PmdRuleset;
import org.sonar.test.TestUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

public class PmdRulesRepositoryTest {

  private PmdRulesRepository repository;

  @Before
  public void setup() {
    repository = new PmdRulesRepository(new Java());
  }

  @Test
  public void rulesAreDefinedWithTheDefaultSonarXmlFormat() {
    List<org.sonar.api.rules.Rule> rules = repository.getInitialReferential();
    assertTrue(rules.size() > 0);
    for (org.sonar.api.rules.Rule rule : rules) {
      assertNotNull(rule.getKey());
      assertNotNull(rule.getDescription());
      assertNotNull(rule.getConfigKey());
      assertNotNull(rule.getName());
    }
  }

  @Test
  public void shouldImportconfigurationWithUtf8Character() {
    RulesProfile rulesProfile = repository.loadProvidedProfile("profile", "test_xml_utf8.xml");
    assertThat(rulesProfile, notNullValue());

    String value = rulesProfile.getActiveRules().get(0).getActiveRuleParams().get(0).getValue();
    assertThat(value, is("\u00E9"));
  }

  @Test
  public void shouldBuildModuleWithProperties() {
    org.sonar.api.rules.Rule dbRule = new org.sonar.api.rules.Rule();
    dbRule.setConfigKey("rulesets/design.xml/CloseResource");
    dbRule.setPluginName(CoreProperties.PMD_PLUGIN);
    RuleParam ruleParam = new RuleParam(dbRule, "types", null, null);
    ActiveRule activeRule = new ActiveRule(null, dbRule, RulePriority.MAJOR);
    activeRule.setActiveRuleParams(Arrays.asList(new ActiveRuleParam(activeRule, ruleParam, "Connection,Statement,ResultSet")));

    PmdRuleset ruleset = repository.buildModuleTree(Arrays.asList(activeRule));

    assertThat(ruleset.getRules().size(), is(1));

    PmdRule rule = ruleset.getRules().get(0);
    assertThat(rule.getRef(), is("rulesets/design.xml/CloseResource"));
    assertThat(rule.getProperties().size(), is(1));

    assertThat(rule.getPriority(), is("3"));

    Property property = rule.getProperties().get(0);
    assertThat(property.getName(), is("types"));
    assertThat(property.getValue(), is("Connection,Statement,ResultSet"));
  }

  @Test
  public void shouldBuildManyModules() {

    org.sonar.api.rules.Rule rule1 = new org.sonar.api.rules.Rule();
    rule1.setPluginName(CoreProperties.PMD_PLUGIN);
    rule1.setConfigKey("rulesets/design.xml/CloseResource");
    ActiveRule activeRule1 = new ActiveRule(null, rule1, RulePriority.MAJOR);
    org.sonar.api.rules.Rule rule2 = new org.sonar.api.rules.Rule();
    rule2.setPluginName(CoreProperties.PMD_PLUGIN);
    rule2.setConfigKey("rulesets/braces.xml/IfElseStmtsMustUseBraces");
    ActiveRule activeRule2 = new ActiveRule(null, rule2, RulePriority.MAJOR);

    PmdRuleset ruleset = repository.buildModuleTree(Arrays.asList(activeRule1, activeRule2));

    assertThat(ruleset.getRules().size(), is(2));
    assertThat(ruleset.getRules().get(0).getRef(), is("rulesets/design.xml/CloseResource"));
    assertThat(ruleset.getRules().get(1).getRef(), is("rulesets/braces.xml/IfElseStmtsMustUseBraces"));
  }

  @Test
  public void shouldBuilModuleTreeFromXml() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/test_module_tree.xml");
    PmdRuleset ruleset = repository.buildModuleTreeFromXml(IOUtils.toString(input));

    assertThat(ruleset.getRules().size(), is(3));

    PmdRule rule1 = ruleset.getRules().get(0);
    assertThat(rule1.getRef(), is("rulesets/coupling.xml/CouplingBetweenObjects"));
    assertThat(rule1.getPriority(), is("2"));
    assertThat(rule1.getProperties().size(), is(1));

    Property module1Property = rule1.getProperties().get(0);
    assertThat(module1Property.getName(), is("threshold"));
    assertThat(module1Property.getValue(), is("20"));

    PmdRule rule2 = ruleset.getRules().get(1);
    assertThat(rule2.getRef(), is("rulesets/coupling.xml/ExcessiveImports"));
    assertThat(rule2.getPriority(), is("3"));
    assertThat(rule2.getProperties().size(), is(1));

    Property module2Property = rule2.getProperties().get(0);
    assertThat(module2Property.getName(), is("max"));
    assertThat(module2Property.getValue(), is("30"));

    PmdRule rule3 = ruleset.getRules().get(2);
    assertThat(rule3.getRef(), is("rulesets/design.xml/UseNotifyAllInsteadOfNotify"));
    assertThat(rule3.getPriority(), is("4"));
    assertNull(rule3.getProperties());
  }

  @Test
  public void shouldBuilModuleTreeFromXmlInUtf8() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/test_xml_utf8.xml");
    PmdRuleset ruleset = repository.buildModuleTreeFromXml(IOUtils.toString(input, CharEncoding.UTF_8));

    PmdRule rule1 = ruleset.getRules().get(0);
    assertThat(rule1.getRef(), is("rulesets/coupling.xml/CouplingBetweenObjects"));
    assertThat(rule1.getProperties().get(0).getValue(), is("\u00E9"));
  }

  @Test
  public void shouldBuilXmlFromModuleTree() throws IOException, SAXException {
    PmdRuleset ruleset = buildModuleTreeFixture();
    String xml = repository.buildXmlFromModuleTree(ruleset);
    assertXmlAreSimilar(xml, "test_module_tree.xml");
  }


  @Test
  public void shouldImportConfiguration() throws IOException {
    final List<org.sonar.api.rules.Rule> inputRules = buildRulesFixture();
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(inputRules);

    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/test_module_tree.xml");
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), inputRules);

    assertThat(results.size(), is(activeRulesExpected.size()));
    assertActiveRulesAreEquals(results, activeRulesExpected);
  }

  @Test
  public void shouldImportPmdLevelsAsSonarLevels() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/PmdRulesRepositoryTest/shouldImportPmdLevelsAsSonarLevels.xml");
    final List<org.sonar.api.rules.Rule> rules = buildRulesFixture();
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), rules);

    assertThat(results.size(), is(3));
    assertThat(results.get(0).getPriority(), is(RulePriority.MAJOR));
    assertThat(results.get(1).getPriority(), is(RulePriority.MINOR));
    assertThat(results.get(2).getPriority(), is(RulePriority.INFO));
  }

  @Test
  public void shouldImportWithDefaultRuleLevelWhenNoExplicitPriority() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/PmdRulesRepositoryTest/shouldImportWithDefaultRuleLevelWhenNoExplicitPriority.xml");
    final List<org.sonar.api.rules.Rule> rules = buildRulesFixture();
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), rules);

    assertThat(results.size(), is(1));
    assertThat(results.get(0).getPriority(), is(RulePriority.MAJOR));
  }

  @Test
  public void shouldImportConfigurationContainingDataToExcludeWithoutException() throws IOException {
    shouldImportConfiguration("test_xml_with_data_to_exclude.xml");
  }

  // See http://jira.codehaus.org/browse/XSTR-448 for details
  @Test(expected = ConversionException.class)
  public void shouldFailToImportConfigurationContainingClassParamBecauseOfXStreamLimitation() throws IOException {
    shouldImportConfiguration("test_xml_with_class_param.xml");
  }

  @Test
  public void shouldBuildActiveRulesFromModuleTree() {
    final List<org.sonar.api.rules.Rule> inputRules = buildRulesFixture();
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(inputRules);

    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    PmdRuleset ruleset = buildModuleTreeFixture();
    repository.buildActiveRulesFromModuleTree(ruleset, activeRules, inputRules);

    assertThat(activeRulesExpected.size(), is(activeRules.size()));
    assertActiveRulesAreEquals(activeRulesExpected, activeRules);
  }

  @Test
  public void shouldProvideProfiles() {
    List<RulesProfile> profiles = repository.getProvidedProfiles();
    assertThat(profiles.size(), is(3));

    RulesProfile profile1 = profiles.get(0);
    assertThat(profile1.getName(), is(RulesProfile.SONAR_WAY_NAME));
    assertTrue(profile1.getActiveRules().size() + "", profile1.getActiveRules().size() > 30);

    RulesProfile profile2 = profiles.get(1);
    assertThat(profile2.getName(), is(RulesProfile.SONAR_WAY_FINDBUGS_NAME));
    assertTrue(profile2.getActiveRules().size() + "", profile2.getActiveRules().size() > 30);

    RulesProfile profile3 = profiles.get(2);
    assertThat(profile3.getName(), is(RulesProfile.SUN_CONVENTIONS_NAME));
    assertTrue(profile3.getActiveRules().size() + "", profile3.getActiveRules().size() > 1);
  }


  @Test
  public void shouldAddHeaderToXml() throws IOException, SAXException {
    String xml = repository.addHeaderToXml("<ruleset/>");
    assertXmlAreSimilar(xml, "test_header.xml");
  }

  @Test
  public void shouldBuildOnlyOneModuleWhenNoPmdActiveRules() {
    org.sonar.api.rules.Rule rule1 = new org.sonar.api.rules.Rule();
    rule1.setPluginName("not-a-pmd-plugin");
    ActiveRule activeRule1 = new ActiveRule(null, rule1, RulePriority.CRITICAL);
    org.sonar.api.rules.Rule rule2 = new org.sonar.api.rules.Rule();
    rule2.setPluginName("not-a-pmd-plugin");
    ActiveRule activeRule2 = new ActiveRule(null, rule1, RulePriority.CRITICAL);

    PmdRuleset tree = repository.buildModuleTree(Arrays.asList(activeRule1, activeRule2));
    assertThat(tree.getRules().size(), is(0));
  }

  @Test
  public void shouldBuildOnlyOneModuleWhenNoActiveRules() {
    PmdRuleset tree = repository.buildModuleTree(Collections.<ActiveRule>emptyList());
    assertThat(tree.getRules().size(), is(0));
  }

  @Test
  public void shouldBuildTwoModulesEvenIfSameTwoRulesActivated() {
    org.sonar.api.rules.Rule dbRule1 = new org.sonar.api.rules.Rule();
    dbRule1.setPluginName(CoreProperties.PMD_PLUGIN);
    dbRule1.setConfigKey("rulesets/coupling.xml/CouplingBetweenObjects");
    ActiveRule activeRule1 = new ActiveRule(null, dbRule1, RulePriority.CRITICAL);
    org.sonar.api.rules.Rule dbRule2 = new org.sonar.api.rules.Rule();
    dbRule2.setPluginName(CoreProperties.PMD_PLUGIN);
    dbRule2.setConfigKey("rulesets/coupling.xml/CouplingBetweenObjects");
    ActiveRule activeRule2 = new ActiveRule(null, dbRule2, RulePriority.CRITICAL);

    PmdRuleset tree = repository.buildModuleTree(Arrays.asList(activeRule1, activeRule2));
    assertThat(tree.getRules().size(), is(2));

    PmdRule rule1 = tree.getRules().get(0);
    assertThat(rule1.getRef(), is("rulesets/coupling.xml/CouplingBetweenObjects"));

    PmdRule rule2 = tree.getRules().get(1);
    assertThat(rule2.getRef(), is("rulesets/coupling.xml/CouplingBetweenObjects"));
  }

  // ------------------------------------------------------------------------
  // -- Private methods
  // ------------------------------------------------------------------------

  private void assertXmlAreSimilar(String xml, String xmlFileToFind) throws IOException, SAXException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/" + xmlFileToFind);
    String xmlToFind = IOUtils.toString(input);
    TestUtils.assertSimilarXml(xmlToFind, xml);
  }

  private PmdRuleset buildModuleTreeFixture() {
    PmdRuleset ruleset = new PmdRuleset();
    ruleset.setDescription("Sonar PMD rules");

    PmdRule rule1 = new PmdRule("rulesets/coupling.xml/CouplingBetweenObjects", "2");
    rule1.addProperty(new Property("threshold", "20"));
    ruleset.addRule(rule1);

    PmdRule rule2 = new PmdRule("rulesets/coupling.xml/ExcessiveImports", "3");
    rule2.addProperty(new Property("max", "30"));
    ruleset.addRule(rule2);

    PmdRule rule3 = new PmdRule("rulesets/design.xml/UseNotifyAllInsteadOfNotify", "4");
    ruleset.addRule(rule3);

    return ruleset;
  }

  private List<org.sonar.api.rules.Rule> buildRulesFixture() {
    final org.sonar.api.rules.Rule rule1 = new org.sonar.api.rules.Rule("Coupling Between Objects", "CouplingBetweenObjects",
        "rulesets/coupling.xml/CouplingBetweenObjects", null, CoreProperties.PMD_PLUGIN, null);
    RuleParam ruleParam1 = new RuleParam(rule1, "threshold", null, "i");
    rule1.setParams(Arrays.asList(ruleParam1));

    final org.sonar.api.rules.Rule rule2 = new org.sonar.api.rules.Rule("Excessive Imports", "ExcessiveImports",
        "rulesets/coupling.xml/ExcessiveImports", null, CoreProperties.PMD_PLUGIN, null);
    RuleParam ruleParam2 = new RuleParam(rule2, "max", null, "i");
    rule2.setParams(Arrays.asList(ruleParam2));

    final org.sonar.api.rules.Rule rule3 = new org.sonar.api.rules.Rule("Use Notify All Instead Of Notify", "UseNotifyAllInsteadOfNotify",
        "rulesets/design.xml/UseNotifyAllInsteadOfNotify", null, CoreProperties.PMD_PLUGIN, null);

    final org.sonar.api.rules.Rule rule4 = new org.sonar.api.rules.Rule("Class names should always begin with an upper case character.", "ClassNamingConventions",
        "rulesets/naming.xml/ClassNamingConventions", null, CoreProperties.PMD_PLUGIN, null);

    return Arrays.asList(rule1, rule2, rule3, rule4);
  }

  private List<ActiveRule> buildActiveRulesFixture(List<org.sonar.api.rules.Rule> rules) {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();

    ActiveRule activeRule1 = new ActiveRule(null, rules.get(0), RulePriority.CRITICAL);
    activeRule1.setActiveRuleParams(Arrays.asList(new ActiveRuleParam(activeRule1, rules.get(0).getParams().get(0), "20")));
    activeRules.add(activeRule1);

    ActiveRule activeRule2 = new ActiveRule(null, rules.get(1), RulePriority.MAJOR);
    activeRule2.setActiveRuleParams(Arrays.asList(new ActiveRuleParam(activeRule2, rules.get(1).getParams().get(0), "30")));
    activeRules.add(activeRule2);

    ActiveRule activeRule3 = new ActiveRule(null, rules.get(2), RulePriority.MINOR);
    activeRules.add(activeRule3);

    return activeRules;

  }

  private void assertActiveRulesAreEquals(List<ActiveRule> activeRules1, List<ActiveRule> activeRules2) {
    for (int i = 0; i < activeRules1.size(); i++) {
      ActiveRule activeRule1 = activeRules1.get(i);
      ActiveRule activeRule2 = activeRules2.get(i);
      assertTrue(activeRule1.getRule().equals(activeRule2.getRule()));
      assertTrue(activeRule1.getPriority().equals(activeRule2.getPriority()));
      assertEquals(activeRule1.getActiveRuleParams().size(), (activeRule2.getActiveRuleParams().size()));

      for (int j = 0; j < activeRule1.getActiveRuleParams().size(); j++) {
        ActiveRuleParam activeRuleParam1 = activeRule1.getActiveRuleParams().get(j);
        ActiveRuleParam activeRuleParam2 = activeRule2.getActiveRuleParams().get(j);
        assertTrue(activeRuleParam1.getRuleParam().equals(activeRuleParam2.getRuleParam())
            && activeRuleParam1.getValue().equals(activeRuleParam2.getValue()));
      }
    }
  }

  public void shouldImportConfiguration(String configurationFile) throws IOException {
    final List<org.sonar.api.rules.Rule> inputRules = buildRulesFixture();
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(inputRules);

    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/" + configurationFile);
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), inputRules);

    assertThat(results.size(), is(activeRulesExpected.size()));
    assertActiveRulesAreEquals(results, activeRulesExpected);
  }

  @Test
  public void shouldUseTheProfileNameWhenBuildingTheRulesSet() {
    PmdRuleset tree = repository.buildModuleTree(Collections.<ActiveRule>emptyList(), "aprofile");
    assertThat(tree.getDescription(), is("aprofile"));
  }

}
