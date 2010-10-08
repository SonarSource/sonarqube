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
package org.sonar.server.rules;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DeprecatedProfilesTest {
  @Test
  public void shouldCreateProfile() {
    DeprecatedProfiles.DefaultProfileDefinition def = DeprecatedProfiles.DefaultProfileDefinition.create("sonar way", "java");
    assertThat(def.createProfile(ValidationMessages.create()).getName(), is("sonar way"));
    assertThat(def.createProfile(ValidationMessages.create()).getLanguage(), is("java"));
  }

  @Test
  public void testActivateRule() {
    DeprecatedProfiles.DefaultProfileDefinition def = DeprecatedProfiles.DefaultProfileDefinition.create("sonar way", "java");
    def.activateRule(Rule.create("checkstyle", "IllegalRegexp", "Illegal Regexp"), RulePriority.BLOCKER);
    def.activateRule(Rule.create("pmd", "NullPointer", "Null Pointer"), RulePriority.INFO);

    assertThat(def.getRules().size(), is(2));
    assertThat(def.getRulesByRepositoryKey("checkstyle").size(), is(1));
    assertThat(def.getRulesByRepositoryKey("checkstyle").get(0).getPriority(), is(RulePriority.BLOCKER));
  }

  @Test
  public void priorityIsOptional() {
    DeprecatedProfiles.DefaultProfileDefinition def = DeprecatedProfiles.DefaultProfileDefinition.create("sonar way", "java");
    def.activateRule(Rule.create("checkstyle", "IllegalRegexp", "Illegal regexp").setPriority(RulePriority.BLOCKER), null);
    assertThat(def.getRules().get(0).getPriority(), is(RulePriority.BLOCKER));
  }
}
