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

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveRuleDoc implements ActiveRule {

  private final Map<String, Object> fields;
  private final ActiveRuleKey key;

  public ActiveRuleDoc(Map<String, Object> fields) {
    ActiveRuleKey key = ActiveRuleKey.parse((String)fields.get(ActiveRuleNormalizer.ActiveRuleField.KEY.key()));
    if(key == null){
      throw new IllegalStateException("Invalid ActiveRuleKey!");
    }
    this.fields = fields;
    this.key = key;
  }

  @Override
  public ActiveRuleKey key() {
    return this.key;
  }

  @Override
  public String severity() {
    return (String) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.key());
  }

  @Override
  public ActiveRule.Inheritance inheritance() {
    String inheritance = (String) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.key());
    if(inheritance == null || inheritance.isEmpty() ||
      inheritance.toLowerCase().contains("none")){
      return Inheritance.NONE;
    } else if(inheritance.toLowerCase().contains("herit")) {
      return Inheritance.INHERIT;
    } else if(inheritance.toLowerCase().contains("over")) {
      return Inheritance.OVERRIDE;
    } else {
      throw new IllegalStateException("Value \"" +inheritance+"\" is not valid for rule's inheritance");
    }
  }

  @Override
  @CheckForNull
  public ActiveRuleKey parentKey() {
    String data = (String) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.PARENT_KEY.key());
    if(data != null && !data.isEmpty()){
      return ActiveRuleKey.parse(data);
    }
    return null;
  }

  @Override
  public Map<String, String> params() {
    Map<String, String> params = new HashMap<String, String>();
    if (this.fields.containsKey(ActiveRuleNormalizer.ActiveRuleField.PARAMS.key())) {
      List<Map<String, String>> allParams = (List<Map<String, String>>) this.fields.get(ActiveRuleNormalizer.ActiveRuleField.PARAMS.key());
      for (Map<String, String> param : allParams) {
        params.put(param.get(ActiveRuleNormalizer.ActiveRuleParamField.NAME.key()),
          param.get(ActiveRuleNormalizer.ActiveRuleParamField.VALUE.key()));
      }
    }
    return params;
  }
}
