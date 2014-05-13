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
package org.sonar.server.qualityprofile.index;

import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.qualityprofile.ActiveRule;

import java.util.HashMap;
import java.util.Map;

public class ActiveRuleDoc implements ActiveRule {

  private final Map<String, Object> fields;
  private final ActiveRuleKey key;

  public ActiveRuleDoc(ActiveRuleKey key, Map<String, Object> fields) {
    this.fields = fields;
    this.key = key;
  }

  @Override
  public ActiveRuleKey key() {
    return this.key;
  }

  @Override
  public Boolean override() {
    return (Boolean) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.OVERRIDE);
  }

  @Override
  public String severity() {
    return (String) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.SEVERITY);
  }

  @Override
  public String inherit() {
    return (String) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE);
  }

  @Override
  public Map<String, String> params() {
    Map<String, String> params = new HashMap<String, String>();
    if (this.fields.containsKey(ActiveRuleNormalizer.ActiveRuleField.PARAMS)) {
      Map<String, Map<String, String>> allParams = (Map<String, Map<String, String>>) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.PARAMS);
      for (Map.Entry<String, Map<String, String>> param : allParams.entrySet()) {
        params.put(param.getKey(), param.getValue().get(ActiveRuleNormalizer.ActiveRuleParamField.VALUE.key()));
      }
    }
    return params;
  }
}
