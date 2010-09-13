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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XMLProfileImporterTest {

  @Test
  public void importProfile() throws UnsupportedEncodingException {
    Reader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/profiles/XMLProfileImporterTest/importProfile.xml"), CharEncoding.UTF_8);
    try {
      ValidationMessages validation = ValidationMessages.create();
      RuleFinder ruleFinder = newRuleFinder();
      RulesProfile profile = XMLProfileImporter.create(ruleFinder).importProfile(reader, validation);

      assertThat(profile.getLanguage(), is("java"));
      assertThat(profile.getName(), is("sonar way"));
      assertThat(validation.hasErrors(), is(false));
      assertNotNull(profile);
      assertThat(profile.getActiveRule("checkstyle", "IllegalRegexp").getPriority(), is(RulePriority.CRITICAL));
      
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  @Test
  public void importProfileWithRuleParameters() throws UnsupportedEncodingException {
    Reader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/profiles/XMLProfileImporterTest/importProfileWithRuleParameters.xml"), CharEncoding.UTF_8);
    try {
      ValidationMessages validation = ValidationMessages.create();
      RuleFinder ruleFinder = newRuleFinder();
      RulesProfile profile = XMLProfileImporter.create(ruleFinder).importProfile(reader, validation);

      assertThat(validation.hasErrors(), is(false));
      assertThat(validation.hasWarnings(), is(false));
      ActiveRule rule = profile.getActiveRule("checkstyle", "IllegalRegexp");
      assertThat(rule.getParameter("format"), is("foo"));
      assertThat(rule.getParameter("message"), is("with special characters < > &"));

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  @Test
  public void importProfileWithUnknownRuleParameter() throws UnsupportedEncodingException {
    Reader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/profiles/XMLProfileImporterTest/importProfileWithUnknownRuleParameter.xml"), CharEncoding.UTF_8);
    try {
      ValidationMessages validation = ValidationMessages.create();
      RuleFinder ruleFinder = newRuleFinder();
      RulesProfile profile = XMLProfileImporter.create(ruleFinder).importProfile(reader, validation);

      assertThat(validation.getWarnings().size(), is(1));
      ActiveRule rule = profile.getActiveRule("checkstyle", "IllegalRegexp");
      assertThat(rule.getParameter("unknown"), nullValue());

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private RuleFinder newRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>(){
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