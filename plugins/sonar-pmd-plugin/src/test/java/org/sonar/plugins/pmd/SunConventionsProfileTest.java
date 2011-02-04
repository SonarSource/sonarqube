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

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SunConventionsProfileTest {
  @Test
  public void shouldCreateProfile() {
    SunConventionsProfile sunConvention = new SunConventionsProfile(createPmdProfileImporter());
    ValidationMessages validation = ValidationMessages.create();
    RulesProfile profile = sunConvention.createProfile(validation);
    assertThat(profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY).size(), greaterThan(1));
    assertThat(validation.hasErrors(), is(false));
  }

  private PmdProfileImporter createPmdProfileImporter() {

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find((RuleQuery) anyObject())).thenAnswer(new Answer<Rule>() {

      public Rule answer(InvocationOnMock iom) throws Throwable {
        RuleQuery query = (RuleQuery) iom.getArguments()[0];
        Rule rule = Rule.create(query.getRepositoryKey(), query.getConfigKey(), "Rule name - " + query.getConfigKey())
            .setConfigKey(query.getConfigKey()).setSeverity(RulePriority.BLOCKER);
        return rule;
      }
    });
    PmdProfileImporter importer = new PmdProfileImporter(ruleFinder);
    return importer;
  }

}
