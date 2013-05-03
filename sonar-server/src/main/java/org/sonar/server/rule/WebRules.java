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

import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.rules.Rule;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.server.platform.UserSession;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 */
public class WebRules implements ServerComponent, Startable {

  private final RuleI18nManager i18n;

  public WebRules(RuleI18nManager i18n) {
    this.i18n = i18n;
  }

  public String ruleL10nName(Rule rule) {
    String name = i18n.getName(rule.getRepositoryKey(), rule.getKey(), UserSession.get().locale());
    if (name == null) {
      name = rule.getName();
    }
    return name;
  }

  public String ruleL10nDescription(Rule rule) {
    String desc = i18n.getDescription(rule.getRepositoryKey(), rule.getKey(), UserSession.get().locale());
    if (desc == null) {
      desc = rule.getDescription();
    }
    return desc;
  }

  @Override
  public void start() {
    // used to force pico to instantiate the singleton at startup
  }

  @Override
  public void stop() {
  }
}
