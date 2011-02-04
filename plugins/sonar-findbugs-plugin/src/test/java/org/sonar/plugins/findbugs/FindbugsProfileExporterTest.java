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
package org.sonar.plugins.findbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.plugins.findbugs.xml.Bug;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;
import org.xml.sax.SAXException;

public class FindbugsProfileExporterTest extends FindbugsTests {

  private FindbugsProfileExporter exporter = new FindbugsProfileExporter();

  @Test
  public void shouldAddHeaderToExportedXml() throws IOException, SAXException {
    RulesProfile profile = RulesProfile.create();

    StringWriter xml = new StringWriter();
    exporter.exportProfile(profile, xml);
    assertXmlAreSimilar(xml.toString(), "test_header.xml");
  }

  @Test
  public void shouldExportConfiguration() throws IOException, SAXException {
    List<Rule> rules = buildRulesFixture();
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(rules);
    RulesProfile profile = RulesProfile.create();
    profile.setActiveRules(activeRulesExpected);

    StringWriter xml = new StringWriter();
    exporter.exportProfile(profile, xml);
    assertXmlAreSimilar(xml.toString(), "test_xml_complete.xml");
  }

  @Test
  public void shouldBuildOnlyOneModuleWhenNoActiveRules() {
    FindBugsFilter filter = FindbugsProfileExporter.buildFindbugsFilter(Collections.<ActiveRule> emptyList());
    assertThat(filter.getMatchs().size(), is(0));
  }

  @Test
  public void shouldBuildTwoModulesEvenIfSameTwoRulesActivated() {
    ActiveRule activeRule1 = anActiveRule(DLS_DEAD_LOCAL_STORE);
    ActiveRule activeRule2 = anActiveRule(SS_SHOULD_BE_STATIC);
    FindBugsFilter filter = FindbugsProfileExporter.buildFindbugsFilter(Arrays.asList(activeRule1, activeRule2));

    List<Match> matches = filter.getMatchs();
    assertThat(matches.size(), is(2));

    assertThat(matches.get(0).getBug().getPattern(), is("DLS_DEAD_LOCAL_STORE"));
    assertThat(matches.get(1).getBug().getPattern(), is("SS_SHOULD_BE_STATIC"));
  }

  @Test
  public void shouldBuildOnlyOneModuleWhenNoFindbugsActiveRules() {
    ActiveRule activeRule1 = anActiveRuleFromAnotherPlugin();
    ActiveRule activeRule2 = anActiveRuleFromAnotherPlugin();

    FindBugsFilter filter = FindbugsProfileExporter.buildFindbugsFilter(Arrays.asList(activeRule1, activeRule2));
    assertThat(filter.getMatchs().size(), is(0));
  }

  @Test
  public void shouldBuildModuleWithProperties() {
    ActiveRule activeRule = anActiveRule(DLS_DEAD_LOCAL_STORE);
    FindBugsFilter filter = FindbugsProfileExporter.buildFindbugsFilter(Arrays.asList(activeRule));

    assertThat(filter.getMatchs().size(), is(1));
    assertThat(filter.getMatchs().get(0).getBug().getPattern(), is("DLS_DEAD_LOCAL_STORE"));
  }

  @Test
  public void shouldBuilXmlFromModuleTree() throws IOException, SAXException {
    FindBugsFilter findBugsFilter = new FindBugsFilter();
    findBugsFilter.addMatch(new Match(new Bug("DLS_DEAD_LOCAL_STORE")));
    findBugsFilter.addMatch(new Match(new Bug("URF_UNREAD_FIELD")));

    String xml = findBugsFilter.toXml();

    assertXmlAreSimilar(xml, "test_module_tree.xml");
  }

  private static final String DLS_DEAD_LOCAL_STORE = "DLS_DEAD_LOCAL_STORE";
  private static final String SS_SHOULD_BE_STATIC = "SS_SHOULD_BE_STATIC";

  private static ActiveRule anActiveRule(String configKey) {
    Rule rule = Rule.create();
    rule.setConfigKey(configKey);
    rule.setRepositoryKey(FindbugsConstants.REPOSITORY_KEY);
    ActiveRule activeRule = RulesProfile.create().activateRule(rule, RulePriority.CRITICAL);
    return activeRule;
  }

  private static ActiveRule anActiveRuleFromAnotherPlugin() {
    Rule rule = Rule.create();
    rule.setPluginName("not-a-findbugs-plugin");
    ActiveRule activeRule = RulesProfile.create().activateRule(rule, RulePriority.CRITICAL);
    return activeRule;
  }
}
