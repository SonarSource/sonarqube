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
package org.sonar.api.batch.rule.internal;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class NewModuleRule {
  final RuleKey ruleKey;
  String severity = Severity.defaultSeverity();
  Map<String, String> params = new HashMap<String, String>();
  String engineKey;

  NewModuleRule(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
  }

  public NewModuleRule setSeverity(@Nullable String severity) {
    this.severity = StringUtils.defaultIfBlank(severity, Severity.defaultSeverity());
    return this;
  }

  public NewModuleRule setEngineKey(@Nullable String engineKey) {
    this.engineKey = engineKey;
    return this;
  }

  public NewModuleRule setParam(String key, @Nullable String value) {
    // possible improvement : check that the param key exists in rule definition
    if (value == null) {
      params.remove(key);
    } else {
      params.put(key, value);
    }
    return this;
  }
}
