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

import org.sonar.api.rule.JRubyRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.ui.JRubyFacades;
import org.sonar.server.ui.JRubyI18n;

/**
 * Facade of rules components for JRuby on Rails webapp
 *
 * @since 3.6
 */
public class DefaultJRubyRules implements JRubyRules {

  private final JRubyI18n jRubyI18n;

  public DefaultJRubyRules(JRubyI18n jRubyI18n) {
    this.jRubyI18n = jRubyI18n;
    JRubyFacades.setRules(this);
  }

  public String ruleName(String rubyLocale, RuleKey ruleKey) {
    String l18n =  jRubyI18n.getRuleName(rubyLocale, ruleKey.repository(), ruleKey.rule());
    if (l18n != null) {
      return l18n;
    } else {
      return jRubyI18n.getRuleName("en", ruleKey.repository(), ruleKey.rule());
    }
  }

  public void start() {
    // used to force pico to instantiate the singleton at startup
  }
}
