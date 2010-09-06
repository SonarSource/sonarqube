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
package org.sonar.plugins.findbugs.xml;

import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.plugins.findbugs.FindbugsRulePriorityMapper;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FindBugsFilterTest extends FindBugsXmlTests {

  @Test
  public void shouldBuilXmlFromModuleTree() throws IOException, SAXException {
    FindBugsFilter root = buildModuleTreeFixture();

    String xml = root.toXml();

    assertXmlAreSimilar(xml, "test_module_tree.xml");
  }

  @Test
  public void shouldBuilModuleTreeFromXml() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/test_module_tree.xml");

    FindBugsFilter module = FindBugsFilter.fromXml(IOUtils.toString(input));

    List<Match> matches = module.getMatchs();
    assertThat(matches.size(), is(2));
    assertChild(matches.get(0), "DLS_DEAD_LOCAL_STORE");
    assertChild(matches.get(1), "URF_UNREAD_FIELD");
  }

  private static FindBugsFilter buildModuleTreeFixture() {
    FindBugsFilter findBugsFilter = new FindBugsFilter();
    findBugsFilter.addMatch(new Match(new Bug("DLS_DEAD_LOCAL_STORE")));
    findBugsFilter.addMatch(new Match(new Bug("URF_UNREAD_FIELD")));
    return findBugsFilter;
  }

  private static final String DLS_DEAD_LOCAL_STORE = "DLS_DEAD_LOCAL_STORE";
  private static final String SS_SHOULD_BE_STATIC = "SS_SHOULD_BE_STATIC";

  @Test
  public void shouldBuildModuleWithProperties() {
    ActiveRule activeRule = anActiveRule(DLS_DEAD_LOCAL_STORE);
    FindBugsFilter filter = FindBugsFilter.fromActiveRules(Arrays.asList(activeRule));

    assertThat(filter.getMatchs().size(), is(1));
    assertChild(filter.getMatchs().get(0), DLS_DEAD_LOCAL_STORE);
  }

  @Test
  public void shouldBuildOnlyOneModuleWhenNoActiveRules() {
    FindBugsFilter filter = FindBugsFilter.fromActiveRules(Collections.<ActiveRule>emptyList());
    assertThat(filter.getMatchs().size(), is(0));
  }

  @Test
  public void shouldBuildTwoModulesEvenIfSameTwoRulesActivated() {
    ActiveRule activeRule1 = anActiveRule(DLS_DEAD_LOCAL_STORE);
    ActiveRule activeRule2 = anActiveRule(SS_SHOULD_BE_STATIC);
    FindBugsFilter filter = FindBugsFilter.fromActiveRules(Arrays.asList(activeRule1, activeRule2));

    List<Match> matches = filter.getMatchs();
    assertThat(matches.size(), is(2));

    assertChild(matches.get(0), DLS_DEAD_LOCAL_STORE);
    assertChild(matches.get(1), SS_SHOULD_BE_STATIC);
  }

  @Test
  public void shouldBuildOnlyOneModuleWhenNoFindbugsActiveRules() {
    ActiveRule activeRule1 = anActiveRuleFromAnotherPlugin();
    ActiveRule activeRule2 = anActiveRuleFromAnotherPlugin();

    FindBugsFilter filter = FindBugsFilter.fromActiveRules(Arrays.asList(activeRule1, activeRule2));
    assertThat(filter.getMatchs().size(), is(0));
  }

  private static ActiveRule anActiveRule(String configKey) {
    Rule rule = new Rule();
    rule.setConfigKey(configKey);
    rule.setPluginName(CoreProperties.FINDBUGS_PLUGIN);
    ActiveRule activeRule = new ActiveRule(null, rule, RulePriority.CRITICAL);
    return activeRule;
  }

  private static ActiveRule anActiveRuleFromAnotherPlugin() {
    Rule rule1 = new Rule();
    rule1.setPluginName("not-a-findbugs-plugin");
    ActiveRule activeRule1 = new ActiveRule(null, rule1, RulePriority.CRITICAL);
    return activeRule1;
  }

}
