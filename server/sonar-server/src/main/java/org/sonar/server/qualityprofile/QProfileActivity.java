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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Maps;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityIndexDefinition;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;

/**
 * @since 4.4
 */
public class QProfileActivity extends ActivityDoc {

  private static final String FIELD_SEVERITY = "severity";

  private String ruleName = null;
  private String authorName = null;

  public QProfileActivity(Map<String, Object> fields) {
    super(fields);
    Map<String, String> details = getField("details");
    for (Map.Entry detail : details.entrySet()) {
      fields.put((String) detail.getKey(), detail.getValue());
    }
    if (!fields.containsKey(FIELD_SEVERITY)) {
      fields.put(FIELD_SEVERITY, null);
    }
  }

  @CheckForNull
  public String ruleName() {
    return ruleName;
  }

  public void ruleName(@Nullable String ruleName) {
    this.ruleName = ruleName;
  }

  @CheckForNull
  public String authorName() {
    return authorName;
  }

  public void authorName(@Nullable String authorName) {
    this.authorName = authorName;
  }

  public String profileKey(){
    return getField("profileKey");
  }

  public RuleKey ruleKey(){
    return RuleKey.parse((String) getField("ruleKey"));
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return getNullableField(ActivityIndexDefinition.FIELD_LOGIN);
  }

  @CheckForNull
  public String severity(){
    return (String) getNullableField(FIELD_SEVERITY);
  }

  public Map<String, String> parameters() {
    Map<String, String> params = Maps.newHashMap();
    for (Map.Entry<String, Object> param : fields.entrySet()) {
      if (param.getKey().startsWith("param_")) {
        params.put(param.getKey().replace("param_", ""), (String) param.getValue());
      }
    }
    return params;
  }

}
