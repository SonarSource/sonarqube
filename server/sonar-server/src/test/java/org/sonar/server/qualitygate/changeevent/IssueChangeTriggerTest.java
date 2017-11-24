/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualitygate.changeevent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rules.RuleType;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class IssueChangeTriggerTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void IssueChange_constructor_throws_IAE_if_both_args_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("At least one of ruleType and transitionKey must be non null");

    new IssueChangeTrigger.IssueChange(null, null);
  }

  @Test
  public void verify_IssueChange_getters() {
    IssueChangeTrigger.IssueChange transitionKeyOnly = new IssueChangeTriggerImpl.IssueChange("foo");
    assertThat(transitionKeyOnly.getTransitionKey()).contains("foo");
    assertThat(transitionKeyOnly.getRuleType()).isEmpty();
    IssueChangeTrigger.IssueChange ruleTypeOnly = new IssueChangeTriggerImpl.IssueChange(RuleType.BUG);
    assertThat(ruleTypeOnly.getTransitionKey()).isEmpty();
    assertThat(ruleTypeOnly.getRuleType()).contains(RuleType.BUG);
    IssueChangeTrigger.IssueChange transitionKeyAndRuleType = new IssueChangeTriggerImpl.IssueChange(RuleType.VULNERABILITY, "bar");
    assertThat(transitionKeyAndRuleType.getTransitionKey()).contains("bar");
    assertThat(transitionKeyAndRuleType.getRuleType()).contains(RuleType.VULNERABILITY);
  }

  @Test
  public void verify_IssueChange_equality() {
    IssueChangeTrigger.IssueChange underTest = new IssueChangeTrigger.IssueChange(RuleType.BUG);

    assertThat(underTest).isEqualTo(new IssueChangeTrigger.IssueChange(RuleType.BUG));
    assertThat(underTest).isEqualTo(new IssueChangeTrigger.IssueChange(RuleType.BUG, null));

    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.BUG, randomAlphanumeric(10)));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.CODE_SMELL));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.CODE_SMELL, randomAlphanumeric(10)));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.VULNERABILITY));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.VULNERABILITY, randomAlphanumeric(10)));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(randomAlphanumeric(10)));

    String transitionKey = randomAlphanumeric(10);
    underTest = new IssueChangeTrigger.IssueChange(transitionKey);

    assertThat(underTest).isEqualTo(new IssueChangeTrigger.IssueChange(transitionKey));
    assertThat(underTest).isEqualTo(new IssueChangeTrigger.IssueChange(null, transitionKey));

    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.BUG));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.BUG, transitionKey));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.BUG, randomAlphanumeric(10)));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.CODE_SMELL));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.CODE_SMELL, randomAlphanumeric(10)));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.CODE_SMELL, transitionKey));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.VULNERABILITY));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.VULNERABILITY, randomAlphanumeric(10)));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(RuleType.VULNERABILITY, transitionKey));
    assertThat(underTest).isNotEqualTo(new IssueChangeTrigger.IssueChange(randomAlphanumeric(9)));
  }
}
