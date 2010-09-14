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

import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.xml.Profile;
import org.sonar.api.rules.xml.Property;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StandardProfileXmlParserTest {

  @Test
  public void checkAllFields() {
    StandardProfileXmlParser parser = new StandardProfileXmlParser();
    String xml = "<profile name='Sonar way' language='java'><rule key=\"2006\" priority=\"warning\" /><rule key=\"2007\" priority=\"error\"><property name=\"toto\" value=\"titi\" /></rule></profile>";
    Profile profile = parser.parse(xml);

    assertEquals(2, profile.getRules().size());
    assertEquals("Sonar way", profile.getName());

    org.sonar.api.rules.xml.Rule rule1 = profile.getRules().get(0);
    assertEquals("2006", rule1.getKey());
    assertEquals("warning", rule1.getPriority());
    assertNull(rule1.getProperties());

    org.sonar.api.rules.xml.Rule rule2 = profile.getRules().get(1);
    assertEquals("2007", rule2.getKey());
    assertEquals("error", rule2.getPriority());
    assertEquals(rule2.getProperties().size(), 1);

    Property property = rule2.getProperties().get(0);
    assertEquals("toto", property.getName());
    assertEquals("titi", property.getValue());
  }

  @Test(expected = SonarException.class)
  public void shouldProfileNameBeNotNull() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/rules/test_profile_name_null.xml");
    StandardProfileXmlParser standardProfileXmlParser = new StandardProfileXmlParser();
    standardProfileXmlParser.importConfiguration(IOUtils.toString(input));
  }

  @Test
  public void shouldBuildProfileFromXml() throws IOException {
    StandardProfileXmlParser standardProfileXmlParser = new StandardProfileXmlParser();
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/rules/test_profile.xml");
    Profile profile = standardProfileXmlParser.buildProfileFromXml(IOUtils.toString(input));

    assertThat("Sonar way", is(profile.getName()));
    assertThat(profile.getRules().size(), is(3));

    org.sonar.api.rules.xml.Rule rule1 = profile.getRules().get(0);
    assertThat(rule1.getKey(), is("2006"));
    assertThat(rule1.getPriority(), is("warning"));
    assertNull(rule1.getProperties());

    org.sonar.api.rules.xml.Rule rule2 = profile.getRules().get(1);
    assertThat(rule2.getKey(), is("2007"));
    assertThat(rule2.getPriority(), is("error"));
    assertThat(rule2.getProperties().size(), is(1));

    org.sonar.api.rules.xml.Rule rule3 = profile.getRules().get(2);
    assertThat(rule3.getKey(), is("2008"));
    assertThat(rule3.getPriority(), is("critical"));
    assertNull(rule3.getProperties());

    Property rule2Property = rule2.getProperties().get(0);
    assertThat(rule2Property.getName(), is("toto"));
    assertThat(rule2Property.getValue(), is("titi"));
  }

  @Test
  public void shouldImportConfiguration() throws IOException {
    final List<Rule> inputRules = buildRulesFixture();
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(inputRules);

    StandardProfileXmlParser standardProfileXmlParser = new StandardProfileXmlParser(inputRules);

    InputStream input = getClass().getResourceAsStream("/org/sonar/api/rules/test_profile.xml");
    RulesProfile profile = standardProfileXmlParser.importConfiguration(IOUtils.toString(input));
    List<ActiveRule> results = profile.getActiveRules();

    assertThat("Sonar way", CoreMatchers.is(profile.getName()));
    assertThat(results.size(), is(activeRulesExpected.size()));
    assertActiveRulesAreEquals(results, activeRulesExpected);
  }

  private List<Rule> buildRulesFixture() {
    List<Rule> rules = new ArrayList<Rule>();

    Rule rule1 = new Rule("One rule", "2006",
        "2006", null, "MYPLUGIN", null);

    Rule rule2 = new Rule("Another rule", "2007",
        "2007", null, "MYPLUGIN", null);
    RuleParam ruleParam2 = new RuleParam(rule2, "toto", null, "s");
    rule2.setParams(Arrays.asList(ruleParam2));

    Rule rule3 = new Rule("Third rule", "2008",
        "2008", null, "MYPLUGIN", null);

    rules.add(rule1);
    rules.add(rule2);
    rules.add(rule3);

    return rules;
  }


  private List<ActiveRule> buildActiveRulesFixture(List<Rule> rules) {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();

    ActiveRule activeRule1 = new ActiveRule(null, rules.get(0), RulePriority.INFO);
    activeRules.add(activeRule1);

    ActiveRule activeRule2 = new ActiveRule(null, rules.get(1), RulePriority.MAJOR);
    activeRule2.setActiveRuleParams(Arrays.asList(new ActiveRuleParam(activeRule2, rules.get(1).getParams().get(0), "titi")));
    activeRules.add(activeRule2);

    ActiveRule activeRule3 = new ActiveRule(null, rules.get(2), RulePriority.CRITICAL);
    activeRules.add(activeRule3);

    return activeRules;
  }

  private void assertActiveRulesAreEquals(List<ActiveRule> activeRules1, List<ActiveRule> activeRules2) {
    for (int i = 0; i < activeRules1.size(); i++) {
      ActiveRule activeRule1 = activeRules1.get(i);
      ActiveRule activeRule2 = activeRules2.get(i);
      assertTrue(activeRule1.getRule().equals(activeRule2.getRule()) && activeRule1.getPriority().equals(activeRule2.getPriority()));

      Assert.assertEquals(activeRule1.getActiveRuleParams().size(), (activeRule2.getActiveRuleParams().size()));
      for (int j = 0; j < activeRule1.getActiveRuleParams().size(); j++) {
        ActiveRuleParam activeRuleParam1 = activeRule1.getActiveRuleParams().get(j);
        ActiveRuleParam activeRuleParam2 = activeRule2.getActiveRuleParams().get(j);
        assertTrue(activeRuleParam1.getRuleParam().equals(activeRuleParam2.getRuleParam())
            && activeRuleParam1.getValue().equals(activeRuleParam2.getValue()));
      }
    }
  }

}