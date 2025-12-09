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
package org.sonar.core.util.rule;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RuleSetChangedEventTest {

  @Test
  public void getDeactivatedAndActivatedRules() {
    String project = "sonarqube";
    RuleChange[] activatedRules = { new RuleChange()};
    String[] deactivatedRules = {"ruleKey"};
    RuleSetChangedEvent event = new RuleSetChangedEvent(project, activatedRules, deactivatedRules);

    assertThat(event.getActivatedRules()).isEqualTo(activatedRules);
    assertThat(event.getDeactivatedRules()).isEqualTo(deactivatedRules);
  }

  @Test
  public void getLanguage_givenBothArraysEmpty_throwException() {
    String project = "sonarqube";
    RuleChange[] activatedRules = {};
    String[] deactivatedRules = {};

    assertThatThrownBy(() -> new RuleSetChangedEvent(project, activatedRules, deactivatedRules))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private RuleChange createRuleChange(String language) {
    RuleChange ruleChange = new RuleChange();
    ruleChange.setLanguage(language);
    return ruleChange;
  }
}
