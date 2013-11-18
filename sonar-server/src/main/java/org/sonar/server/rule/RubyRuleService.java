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

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.rules.Rule;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.Map;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 */
public class RubyRuleService implements ServerComponent, Startable {

  private final RuleI18nManager i18n;
  private final RuleRegistry ruleRegistry;

  private static final String OPTIONS_STATUS = "status";
  private static final String OPTIONS_LANGUAGE = "language";

  public RubyRuleService(RuleI18nManager i18n, RuleRegistry ruleRegistry) {
    this.i18n = i18n;
    this.ruleRegistry = ruleRegistry;
  }

  @CheckForNull
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

  public Integer[] findIds(Map<String, String> options) {
    Map<String, String> params = Maps.newHashMap();
    translateNonBlankKey(options, params, OPTIONS_STATUS, OPTIONS_STATUS);
    translateNonBlankKey(options, params, "repositories", "repositoryKey");
    translateNonBlankKey(options, params, OPTIONS_LANGUAGE, OPTIONS_LANGUAGE);
    translateNonBlankKey(options, params, "searchtext", "nameOrKey");
    return ruleRegistry.findIds(params).toArray(new Integer[0]);
  }

  private static void translateNonBlankKey(Map<String, String> options, Map<String, String> params, String optionKey, String paramKey) {
    if(options.get(optionKey) != null && StringUtils.isNotBlank(options.get(optionKey).toString())) {
      params.put(paramKey, options.get(optionKey).toString());
    }
  }

  @Override
  public void start() {
    // used to force pico to instantiate the singleton at startup
  }

  @Override
  public void stop() {
    // implement startable
  }
}
