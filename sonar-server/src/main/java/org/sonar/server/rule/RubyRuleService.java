/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import java.util.Map;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 */
public class RubyRuleService implements ServerComponent, Startable {

  private final RuleRegistry ruleRegistry;

  private static final String OPTIONS_STATUS = "status";
  private static final String OPTIONS_LANGUAGE = "language";

  public RubyRuleService(RuleRegistry ruleRegistry) {
    this.ruleRegistry = ruleRegistry;
  }

  public Integer[] findIds(Map<String, String> options) {
    Map<String, String> params = Maps.newHashMap();
    translateNonBlankKey(options, params, OPTIONS_STATUS, OPTIONS_STATUS);
    translateNonBlankKey(options, params, "repositories", "repositoryKey");
    translateNonBlankKey(options, params, OPTIONS_LANGUAGE, OPTIONS_LANGUAGE);
    translateNonBlankKey(options, params, "searchtext", "nameOrKey");
    return ruleRegistry.findIds(params).toArray(new Integer[0]);
  }

  public void saveOrUpdate(int ruleId) {
    ruleRegistry.saveOrUpdate(ruleId);
  }

  private static void translateNonBlankKey(Map<String, String> options, Map<String, String> params, String optionKey, String paramKey) {
    if (options.get(optionKey) != null && StringUtils.isNotBlank(options.get(optionKey).toString())) {
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
