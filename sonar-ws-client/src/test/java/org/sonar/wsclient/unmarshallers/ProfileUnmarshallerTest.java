/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Profile;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProfileUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void shouldNotHaveProfile() {
    Profile profile = new ProfileUnmarshaller().toModel("[]");
    assertThat(profile, nullValue());
  }

  @Test
  public void shouldGetProfile() {
    Profile profile = new ProfileUnmarshaller().toModel(loadFile("/profiles/profile.json"));
    assertThat(profile.getLanguage(), is("java"));
    assertThat(profile.getName(), is("Sonar way"));
    assertThat(profile.getParentName(), nullValue());
    assertThat(profile.isDefaultProfile(), is(true));

    assertThat(profile.getRules().size(), is(116));
    Profile.Rule rule1 = profile.getRules().get(0);
    assertThat(rule1.getKey(), is("com.puppycrawl.tools.checkstyle.checks.coding.InnerAssignmentCheck"));
    assertThat(rule1.getRepository(), is("checkstyle"));
    assertThat(rule1.getInheritance(), nullValue());
    assertThat(rule1.getSeverity(), is("MAJOR"));
    assertThat(rule1.getParameters().size(), is(0));
    assertThat(rule1.getParameter("foo"), nullValue());

    Profile.Rule rule2 = profile.getRule("checkstyle", "com.puppycrawl.tools.checkstyle.checks.naming.LocalFinalVariableNameCheck");
    assertThat(rule2.getParameters().size(), is(1));
    assertThat(rule2.getParameter("format"), is("^[a-z][a-zA-Z0-9]*$"));
  }
}
