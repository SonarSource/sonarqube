/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.rule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;

import static org.assertj.core.api.Assertions.assertThat;

public class XooBuiltInQualityProfilesDefinitionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private XooBuiltInQualityProfilesDefinition underTest = new XooBuiltInQualityProfilesDefinition();

  @Test
  public void test_built_in_quality_profile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

    underTest.define(context);

    BuiltInQualityProfile profile = context.profile("xoo", "test BuiltInQualityProfilesDefinition");
    assertThat(profile.isDefault()).isFalse();
    assertThat(profile.name()).isEqualTo("test BuiltInQualityProfilesDefinition");
    assertThat(profile.language()).isEqualTo("xoo");
    assertThat(profile.rules()).hasSize(1);
    BuiltInQualityProfilesDefinition.BuiltInActiveRule activeRule = profile.rule(RuleKey.of("xoo", "HasTag"));
    assertThat(activeRule.overriddenSeverity()).isEqualTo("BLOCKER");
    assertThat(activeRule.overriddenParams()).hasSize(1);
    assertThat(activeRule.overriddenParam("tag").overriddenValue()).isEqualTo("TODO");
  }
}
