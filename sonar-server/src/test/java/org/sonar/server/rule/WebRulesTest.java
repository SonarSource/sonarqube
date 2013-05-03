/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.server.platform.UserSession;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebRulesTest {

  RuleI18nManager i18n = mock(RuleI18nManager.class);
  WebRules facade = new WebRules(i18n);

  @Test
  public void should_get_localized_rule_name() {
    UserSession.set(new UserSession(123, "emmerik", Locale.FRENCH));
    when(i18n.getName("squid", "AvoidCycle", Locale.FRENCH)).thenReturn("Eviter les cycles");

    String name = facade.l10nRuleName(new Rule("squid", "AvoidCycle"));
    assertThat(name).isEqualTo("Eviter les cycles");
  }

  @Test
  public void should_get_raw_name_if_no_l10n_name() throws Exception {
    UserSession.set(new UserSession(123, "emmerik", Locale.FRENCH));
    when(i18n.getName("squid", "AvoidCycle", Locale.FRENCH)).thenReturn(null);

    Rule rule = new Rule("squid", "AvoidCycle");
    rule.setName("Avoid cycles");
    String name = facade.l10nRuleName(rule);
    assertThat(name).isEqualTo("Avoid cycles");
  }

  @Test
  public void should_get_localized_rule_description() {
    UserSession.set(new UserSession(123, "emmerik", Locale.FRENCH));
    when(i18n.getDescription("squid", "AvoidCycle", Locale.FRENCH)).thenReturn("Les cycles sont le mal");

    String desc = facade.l10nRuleDescription(new Rule("squid", "AvoidCycle"));
    assertThat(desc).isEqualTo("Les cycles sont le mal");
  }

  @Test
  public void should_get_raw_description_if_no_l10n_description() throws Exception {
    UserSession.set(new UserSession(123, "emmerik", Locale.FRENCH));
    when(i18n.getDescription("squid", "AvoidCycle", Locale.FRENCH)).thenReturn(null);

    Rule rule = new Rule("squid", "AvoidCycle");
    rule.setDescription("Cycles are evil");
    String desc = facade.l10nRuleDescription(rule);
    assertThat(desc).isEqualTo("Cycles are evil");
  }

  @Test
  public void just_for_fun_and_coverage() throws Exception {
    facade.start();
    facade.stop();
    // do not fail
  }
}
