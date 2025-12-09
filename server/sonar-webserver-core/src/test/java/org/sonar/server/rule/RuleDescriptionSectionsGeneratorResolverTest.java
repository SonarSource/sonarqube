/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.rule;

import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleDescriptionSectionsGeneratorResolverTest {

  private static final String RULE_KEY = "RULE_KEY";

  @Mock
  private RuleDescriptionSectionsGenerator generator1;
  @Mock
  private RuleDescriptionSectionsGenerator generator2;
  @Mock
  private RulesDefinition.Rule rule;

  private RuleDescriptionSectionsGeneratorResolver resolver;

  @Before
  public void setUp() {
    resolver = new RuleDescriptionSectionsGeneratorResolver(Set.of(generator1, generator2));
    when(rule.key()).thenReturn(RULE_KEY);
  }

  @Test
  public void getRuleDescriptionSectionsGenerator_returnsTheCorrectGenerator() {
    when(generator2.isGeneratorForRule(rule)).thenReturn(true);
    assertThat(resolver.getRuleDescriptionSectionsGenerator(rule)).isEqualTo(generator2);
  }

  @Test
  public void getRuleDescriptionSectionsGenerator_whenNoGeneratorFound_throwsWithCorrectMessage() {
    assertThatIllegalStateException()
      .isThrownBy(() ->  resolver.getRuleDescriptionSectionsGenerator(rule))
      .withMessage("No rule description section generator found for rule with key RULE_KEY");
  }

  @Test
  public void getRuleDescriptionSectionsGenerator_whenMoreThanOneGeneratorFound_throwsWithCorrectMessage() {
    when(generator1.isGeneratorForRule(rule)).thenReturn(true);
    when(generator2.isGeneratorForRule(rule)).thenReturn(true);
    assertThatIllegalStateException()
      .isThrownBy(() ->  resolver.getRuleDescriptionSectionsGenerator(rule))
      .withMessage("More than one rule description section generator found for rule with key RULE_KEY");
  }

}
