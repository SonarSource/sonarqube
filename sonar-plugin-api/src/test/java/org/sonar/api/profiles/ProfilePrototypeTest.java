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
import org.sonar.api.rules.RulePriority;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ProfilePrototypeTest {

  @Test
  public void addRuleByKey() {
    ProfilePrototype profile = ProfilePrototype.create();
    profile.activateRule("checkstyle", "JavadocCheck", RulePriority.MINOR);
    profile.activateRule("checkstyle", "EqualsHashCodeCheck", RulePriority.BLOCKER);
    profile.activateRule("findbugs", "DetectNullPointer", RulePriority.BLOCKER);

    assertThat(profile.getRules().size(), is(3));
    assertThat(profile.getRulesByRepositoryKey("checkstyle").size(), is(2));
    assertThat(profile.getRulesByRepositoryKey("pmd").size(), is(0));
    assertThat(profile.getRule("findbugs", "DetectNullPointer"), not(nullValue()));
    assertThat(profile.getRule("findbugs", "DetectNullPointer").getPriority(), is(RulePriority.BLOCKER));
  }

  @Test
  public void addRuleByConfigKey() {
    ProfilePrototype profile = ProfilePrototype.create();
    profile.activateRule(ProfilePrototype.RulePrototype.createByConfigKey("checkstyle", "Checker/TreeWalker/EqualsHashCode"));

    assertThat(profile.getRules().size(), is(1));
    assertThat(profile.getRule("checkstyle", "Checker/TreeWalker/EqualsHashCode"), nullValue());
    assertThat(profile.getRuleByConfigKey("checkstyle", "Checker/TreeWalker/EqualsHashCode"), not(nullValue()));
  }
}
