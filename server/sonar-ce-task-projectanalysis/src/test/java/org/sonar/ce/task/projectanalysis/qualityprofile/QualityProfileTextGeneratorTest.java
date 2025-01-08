/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class QualityProfileTextGeneratorTest {

  private final Map<ActiveRuleChange.Type, Long> changeToNumberOfRules;
  private final String expectedText;

  public QualityProfileTextGeneratorTest(Map<ActiveRuleChange.Type, Long> changeToNumberOfRules, String expectedText) {
    this.changeToNumberOfRules = changeToNumberOfRules;
    this.expectedText = expectedText;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {
        Map.ofEntries(
          Map.entry(ActiveRuleChange.Type.ACTIVATED, 12L),
          Map.entry(ActiveRuleChange.Type.DEACTIVATED, 8L),
          Map.entry(ActiveRuleChange.Type.UPDATED, 5L)
        ),
        "12 new rules, 8 deactivated rules and 5 modified rules"
      },
      {
        Map.ofEntries(
          Map.entry(ActiveRuleChange.Type.ACTIVATED, 1L),
          Map.entry(ActiveRuleChange.Type.DEACTIVATED, 0L),
          Map.entry(ActiveRuleChange.Type.UPDATED, 0L)
        ),
        "1 new rule"
      },
      {
        Map.ofEntries(
          Map.entry(ActiveRuleChange.Type.DEACTIVATED, 5L)
        ),
        "5 deactivated rules"
      },
      {
        Map.ofEntries(
          Map.entry(ActiveRuleChange.Type.UPDATED, 7L)
        ),
        "7 modified rules"
      },
      {
        Map.ofEntries(
          Map.entry(ActiveRuleChange.Type.ACTIVATED, 1L),
          Map.entry(ActiveRuleChange.Type.DEACTIVATED, 1L),
          Map.entry(ActiveRuleChange.Type.UPDATED, 1L)
        ),
        "1 new rule, 1 deactivated rule and 1 modified rule"
      },
      {
        Map.ofEntries(
          Map.entry(ActiveRuleChange.Type.ACTIVATED, 1L),
          Map.entry(ActiveRuleChange.Type.UPDATED, 3L)
        ),
        "1 new rule and 3 modified rules"
      }
    });
  }

  @Test
  public void givenRulesChanges_whenBuild_thenTextContainsAll() {
    // given when
    String updateMessage = QualityProfileTextGenerator.generateRuleChangeText(changeToNumberOfRules);

    // then
    assertThat(updateMessage).isEqualTo(expectedText);
  }

}
