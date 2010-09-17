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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RulesManager;
import org.sonar.test.TestUtils;
import org.xml.sax.SAXException;

public abstract class FindbugsTests {

  protected void assertXmlAreSimilar(String actualContent, String expectedFileName) throws IOException, SAXException {
    String expectedContent = TestUtils.getResourceContent("/org/sonar/plugins/findbugs/" + expectedFileName);
    TestUtils.assertSimilarXml(expectedContent, actualContent);
  }

  protected List<Rule> buildRulesFixture() {
    List<Rule> rules = new ArrayList<Rule>();

    Rule rule1 = new Rule("DLS: Dead store to local variable", "DLS_DEAD_LOCAL_STORE", "DLS_DEAD_LOCAL_STORE", null,
        CoreProperties.FINDBUGS_PLUGIN, null);

    Rule rule2 = new Rule("UrF: Unread field", "URF_UNREAD_FIELD", "URF_UNREAD_FIELD", null, CoreProperties.FINDBUGS_PLUGIN, null);

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
    RulesProfile profile = RulesProfile.create();
    profile.setName(RulesProfile.SONAR_WAY_FINDBUGS_NAME);
    profile.setLanguage(Java.KEY);
    for (Rule rule : new FindbugsRuleRepository().createRules()) {
      rule.setRepositoryKey(FindbugsConstants.REPOSITORY_KEY);
      profile.activateRule(rule, null);
    }
    return profile;
  }
}
