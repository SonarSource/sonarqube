/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.squid;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarWayProfileTest {
  @Test
  public void should_create_sonar_way_profile() {
    ValidationMessages validation = ValidationMessages.create();

    SonarWayProfile definition = new SonarWayProfile(new AnnotationProfileParser(ruleFinder()));
    RulesProfile profile = definition.createProfile(validation);

    assertThat(profile.getActiveRulesByRepository(SquidConstants.REPOSITORY_KEY)).hasSize(2);
    assertThat(profile.getName()).isEqualTo(RulesProfile.SONAR_WAY_NAME);
    assertThat(validation.hasErrors()).isFalse();
  }

  @Test
  public void should_create_sonar_way_with_findbugs_profile() {
    ValidationMessages validation = ValidationMessages.create();

    SonarWayWithFindbugsProfile definition = new SonarWayWithFindbugsProfile(new SonarWayProfile(new AnnotationProfileParser(ruleFinder())));
    RulesProfile profile = definition.createProfile(validation);

    assertThat(profile.getActiveRulesByRepository(SquidConstants.REPOSITORY_KEY)).hasSize(2);
    assertThat(profile.getName()).isEqualTo(RulesProfile.SONAR_WAY_FINDBUGS_NAME);
    assertThat(validation.hasErrors()).isFalse();
  }

  static RuleFinder ruleFinder() {
    return when(mock(RuleFinder.class).findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) {
        Object[] arguments = invocation.getArguments();
        return Rule.create((String) arguments[0], (String) arguments[1], (String) arguments[1]);
      }
    }).getMock();
  }
}
