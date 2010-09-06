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
package org.sonar.plugins.checkstyle;

import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.ProfilePrototype;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class SunConventionsProfileTest {
  @Test
  public void create() {
    ProfileDefinition sunConventionsProfile = new SunConventionsProfile();
    ProfilePrototype prototype = sunConventionsProfile.createPrototype();
    assertThat(prototype.getRulesByRepositoryKey(CheckstyleConstants.REPOSITORY_KEY).size(), greaterThan(1));
    assertThat(
        prototype.getRule(CheckstyleConstants.REPOSITORY_KEY, "com.puppycrawl.tools.checkstyle.checks.NewlineAtEndOfFileCheck").getParameter("lineSeparator"),
        is("system"));
  }
}
