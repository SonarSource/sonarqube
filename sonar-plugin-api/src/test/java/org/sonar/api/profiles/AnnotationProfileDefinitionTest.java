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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Check;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnnotationProfileDefinitionTest {

  @Test
  public void importProfile() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>(){
      public Rule answer(InvocationOnMock iom) throws Throwable {
        return Rule.create((String)iom.getArguments()[0], (String)iom.getArguments()[1], (String)iom.getArguments()[1]);
      }
    });

    ProfileDefinition definition = new FakeDefinition(ruleFinder);
    ValidationMessages validation = ValidationMessages.create();
    RulesProfile profile = definition.createProfile(validation);
    assertThat(profile.getActiveRule("squid", "fake").getPriority(), is(RulePriority.BLOCKER));
    assertThat(validation.hasErrors(), is(false));
  }
}

@BelongsToProfile(title = "not used !", priority = Priority.BLOCKER)
@Check(key = "fake", isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
class FakeRule {

}


class FakeDefinition extends AnnotationProfileDefinition {

  public FakeDefinition(RuleFinder ruleFinder) {
    super("squid", "sonar way", "java", Arrays.<Class>asList(FakeRule.class), ruleFinder);
  }
}