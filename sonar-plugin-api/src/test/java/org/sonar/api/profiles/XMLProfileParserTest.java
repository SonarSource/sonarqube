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
package org.sonar.api.profiles;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XMLProfileParserTest {

  @Test
  public void importProfile() throws UnsupportedEncodingException {
    ValidationMessages validation = ValidationMessages.create();
    RuleFinder ruleFinder = newRuleFinder();
    RulesProfile profile = new XMLProfileParser(ruleFinder).parseResource(getClass().getClassLoader(), "org/sonar/api/profiles/XMLProfileParserTest/importProfile.xml", validation);

    assertThat(profile.getLanguage(), is("java"));
    assertThat(profile.getName(), is("sonar way"));
    assertThat(validation.hasErrors(), is(false));
    assertNotNull(profile);
    assertThat(profile.getActiveRule("checkstyle", "IllegalRegexp").getPriority(), is(RulePriority.CRITICAL));
  }

  @Test
  public void nameAndLanguageShouldBeMandatory() throws UnsupportedEncodingException {
    ValidationMessages validation = ValidationMessages.create();
    RuleFinder ruleFinder = newRuleFinder();
    RulesProfile profile = new XMLProfileParser(ruleFinder).parseResource(getClass().getClassLoader(), "org/sonar/api/profiles/XMLProfileParserTest/nameAndLanguageShouldBeMandatory.xml", validation);

    assertThat(validation.getErrors().size(), is(2));
    assertThat(validation.getErrors().get(0), containsString(""));

  }

  @Test
  public void importProfileWithRuleParameters() throws UnsupportedEncodingException {
    ValidationMessages validation = ValidationMessages.create();
    RuleFinder ruleFinder = newRuleFinder();
    RulesProfile profile = new XMLProfileParser(ruleFinder).parseResource(getClass().getClassLoader(), "org/sonar/api/profiles/XMLProfileParserTest/importProfileWithRuleParameters.xml", validation);

    assertThat(validation.hasErrors(), is(false));
    assertThat(validation.hasWarnings(), is(false));
    ActiveRule rule = profile.getActiveRule("checkstyle", "IllegalRegexp");
    assertThat(rule.getParameter("format"), is("foo"));
    assertThat(rule.getParameter("message"), is("with special characters < > &"));
  }

  @Test
  public void importProfileWithUnknownRuleParameter() throws UnsupportedEncodingException {
    ValidationMessages validation = ValidationMessages.create();
    RuleFinder ruleFinder = newRuleFinder();
    RulesProfile profile = new XMLProfileParser(ruleFinder).parseResource(getClass().getClassLoader(), "org/sonar/api/profiles/XMLProfileParserTest/importProfileWithUnknownRuleParameter.xml", validation);

    assertThat(validation.getWarnings().size(), is(1));
    ActiveRule rule = profile.getActiveRule("checkstyle", "IllegalRegexp");
    assertThat(rule.getParameter("unknown"), nullValue());
  }

  private RuleFinder newRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock iom) throws Throwable {
        Rule rule = Rule.create((String) iom.getArguments()[0], (String) iom.getArguments()[1], (String) iom.getArguments()[1]);
        rule.createParameter("format");
        rule.createParameter("message");
        return rule;
      }
    });
    return ruleFinder;
  }
}