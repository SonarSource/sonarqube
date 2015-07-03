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

import com.google.common.base.Preconditions;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveRuleDoc extends BaseDoc implements ActiveRule {

  private final ActiveRuleKey key;

  public ActiveRuleDoc(Map<String, Object> fields) {
    super(fields);
    this.key = ActiveRuleKey.parse((String) getField(ActiveRuleNormalizer.ActiveRuleField.KEY.field()));
    Preconditions.checkArgument(key!=null, "Invalid ActiveRuleKey!");
  }

  @Override
  public Date createdAt() {
    return IndexUtils.parseDateTime((String) getNullableField(ActiveRuleNormalizer.ActiveRuleField.CREATED_AT.field()));
  }

  @Override
  public Date updatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(ActiveRuleNormalizer.ActiveRuleField.UPDATED_AT.field()));
  }

  @Override
  public ActiveRuleKey key() {
    return this.key;
  }

  @Override
  public String severity() {
    return (String) getNullableField(ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field());
  }

  @Override
  public ActiveRule.Inheritance inheritance() {
    String inheritance = getNullableField(ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field());
    if (inheritance == null || inheritance.isEmpty() ||
      inheritance.toLowerCase().contains("none")) {
      return Inheritance.NONE;
    } else if (inheritance.toLowerCase().contains("herit")) {
      return Inheritance.INHERITED;
    } else if (inheritance.toLowerCase().contains("over")) {
      return Inheritance.OVERRIDES;
    } else {
      throw new IllegalStateException("Value \"" + inheritance + "\" is not valid for rule's inheritance");
    }
  }

  @Override
  @CheckForNull
  public ActiveRuleKey parentKey() {
    String data = getNullableField(ActiveRuleNormalizer.ActiveRuleField.PARENT_KEY.field());
    if (data != null && !data.isEmpty()) {
      return ActiveRuleKey.parse(data);
    }
    return null;
  }

  @Override
  public Map<String, String> params() {
    Map<String, String> params = new HashMap<>();
    List<Map<String, String>> allParams = getNullableField(ActiveRuleNormalizer.ActiveRuleField.PARAMS.field());
    if (allParams != null) {
      for (Map<String, String> param : allParams) {
        params.put(param.get(ActiveRuleNormalizer.ActiveRuleParamField.NAME.field()),
          param.get(ActiveRuleNormalizer.ActiveRuleParamField.VALUE.field()));
      }
    }
    return params;
  }
}
