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
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class XMLProfileImporterTest {

  @Test
  public void importProfile() throws UnsupportedEncodingException {
    Reader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/profiles/XMLProfileImporterTest/importProfile.xml"), CharEncoding.UTF_8);
    try {
      ValidationMessages validation = ValidationMessages.create();
      ProfilePrototype profile = XMLProfileImporter.create().importProfile(reader, validation);

      assertThat(validation.hasErrors(), is(false));
      assertNotNull(profile);
      assertThat(profile.getRule("checkstyle", "IllegalRegexp").getPriority(), is(RulePriority.CRITICAL));
      
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  @Test
  public void importProfileWithRuleParameters() throws UnsupportedEncodingException {
    Reader reader = new InputStreamReader(getClass().getResourceAsStream("/org/sonar/api/profiles/XMLProfileImporterTest/importProfileWithRuleParameters.xml"), CharEncoding.UTF_8);
    try {
      ValidationMessages validation = ValidationMessages.create();
      ProfilePrototype profile = XMLProfileImporter.create().importProfile(reader, validation);

      assertThat(validation.hasErrors(), is(false));
      ProfilePrototype.RulePrototype rule = profile.getRule("checkstyle", "IllegalRegexp");
      assertThat(rule.getParameter("format"), is("foo"));
      assertThat(rule.getParameter("message"), is("with special characters < > &"));

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
