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
package org.sonar.plugins.findbugs;

import org.apache.commons.io.IOUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RulesManager;
import org.sonar.test.TestUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class FindbugsTests {

  protected void assertXmlAreSimilar(String actualContent, String expectedFileName) throws IOException, SAXException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/" + expectedFileName);
    String expectedContent = IOUtils.toString(input);
    TestUtils.assertSimilarXml(expectedContent, actualContent);
  }

  protected List<Rule> buildRulesFixture() {
    List<Rule> rules = new ArrayList<Rule>();

    Rule rule1 = new Rule("DLS: Dead store to local variable", "DLS_DEAD_LOCAL_STORE",
        "DLS_DEAD_LOCAL_STORE", null, CoreProperties.FINDBUGS_PLUGIN, null);

    Rule rule2 = new Rule("UrF: Unread field", "URF_UNREAD_FIELD",
        "URF_UNREAD_FIELD", null, CoreProperties.FINDBUGS_PLUGIN, null);

    rules.add(rule1);
    rules.add(rule2);

    return rules;
  }

  protected List<ActiveRule> buildActiveRulesFixture(List<Rule> rules) {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    ActiveRule activeRule1 = new ActiveRule(null, rules.get(0), RulePriority.CRITICAL);
    activeRules.add(activeRule1);
    ActiveRule activeRule2 = new ActiveRule(null, rules.get(1), RulePriority.MAJOR);
    activeRules.add(activeRule2);
    return activeRules;
  }


  protected RulesManager createRulesManager() {
    RulesManager rulesManager = mock(RulesManager.class);

    when(rulesManager.getPluginRule(eq(CoreProperties.FINDBUGS_PLUGIN), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        Rule rule = new Rule();
        rule.setPluginName((String) args[0]);
        rule.setKey((String) args[1]);
        return rule;
      }
    });
    return rulesManager;
  }

  protected RulesProfile createRulesProfileWithActiveRules() {
    RulesProfile rulesProfile = mock(RulesProfile.class);
    when(rulesProfile.getActiveRule(eq(CoreProperties.FINDBUGS_PLUGIN), anyString())).thenAnswer(new Answer<ActiveRule>() {
      public ActiveRule answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        ActiveRule activeRule = mock(ActiveRule.class);
        when(activeRule.getPluginName()).thenReturn((String) args[0]);
        when(activeRule.getRuleKey()).thenReturn((String) args[1]);
        when(activeRule.getPriority()).thenReturn(RulePriority.CRITICAL);
        return activeRule;
      }
    });
    when(rulesProfile.getActiveRulesByPlugin(CoreProperties.FINDBUGS_PLUGIN)).thenReturn(Arrays.asList(new ActiveRule()));
    return rulesProfile;
  }

  protected RulesProfile createRulesProfileWithoutActiveRules() {
    RulesProfile rulesProfile = new RulesProfile();
    List<ActiveRule> list = new ArrayList<ActiveRule>();
    rulesProfile.setActiveRules(list);
    return rulesProfile;
  }
}
