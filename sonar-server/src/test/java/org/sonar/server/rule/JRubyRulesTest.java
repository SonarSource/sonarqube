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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.ui.JRubyI18n;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JRubyRulesTest {

  private JRubyRules JRubyRules;
  private JRubyI18n jRubyI18n;

  @Before
  public void before() {
    jRubyI18n = mock(JRubyI18n.class);
    JRubyRules = new JRubyRules(jRubyI18n);
  }

  @Test
  public void should_get_localize_name_of_a_rule() {
    String ruleName = "Tabulation characters should not be used";
    String locale = "en";
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    when(jRubyI18n.getRuleName(locale, "squid", "AvoidCycle")).thenReturn(ruleName);
    assertThat(JRubyRules.ruleName(locale, ruleKey)).isEqualTo(ruleName);
  }

  @Test
  public void should_get_english_translation_if_not_found_for_given_locale() {
    String englishRuleName = "Tabulation characters should not be used";
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    when(jRubyI18n.getRuleName("fr", "squid", "AvoidCycle")).thenReturn(null);
    when(jRubyI18n.getRuleName("en", "squid", "AvoidCycle")).thenReturn(englishRuleName);
    assertThat(JRubyRules.ruleName("fr", ruleKey)).isEqualTo(englishRuleName);
  }

}
