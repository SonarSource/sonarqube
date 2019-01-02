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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.Collections;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRulesHolderImplTest {

  private static final String PLUGIN_KEY = "java";

  private static final long SOME_DATE = 1_000L;

  static final RuleKey RULE_KEY = RuleKey.of("squid", "S001");
  private static final String QP_KEY = "qp1";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ActiveRulesHolderImpl underTest = new ActiveRulesHolderImpl();

  @Test
  public void get_inactive_rule() {
    underTest.set(Collections.emptyList());
    Optional<ActiveRule> activeRule = underTest.get(RULE_KEY);
    assertThat(activeRule.isPresent()).isFalse();
  }

  @Test
  public void get_active_rule() {
    underTest.set(asList(new ActiveRule(RULE_KEY, Severity.BLOCKER, Collections.emptyMap(), SOME_DATE, PLUGIN_KEY, QP_KEY)));

    Optional<ActiveRule> activeRule = underTest.get(RULE_KEY);
    assertThat(activeRule.isPresent()).isTrue();
    assertThat(activeRule.get().getRuleKey()).isEqualTo(RULE_KEY);
    assertThat(activeRule.get().getSeverity()).isEqualTo(Severity.BLOCKER);
  }

  @Test
  public void can_not_set_twice() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Active rules have already been initialized");

    underTest.set(asList(new ActiveRule(RULE_KEY, Severity.BLOCKER, Collections.emptyMap(), SOME_DATE, PLUGIN_KEY, QP_KEY)));
    underTest.set(Collections.emptyList());

  }

  @Test
  public void can_not_get_if_not_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Active rules have not been initialized yet");

    underTest.get(RULE_KEY);
  }

  @Test
  public void can_not_set_duplicated_rules() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Active rule must not be declared multiple times: squid:S001");

    underTest.set(asList(
      new ActiveRule(RULE_KEY, Severity.BLOCKER, Collections.emptyMap(), SOME_DATE, PLUGIN_KEY, QP_KEY),
      new ActiveRule(RULE_KEY, Severity.MAJOR, Collections.emptyMap(), SOME_DATE, PLUGIN_KEY, QP_KEY)));
  }
}
