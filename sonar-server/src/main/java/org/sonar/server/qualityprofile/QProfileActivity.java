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

import com.google.common.collect.ImmutableMap;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.activity.Activity;
import org.sonar.server.activity.index.ActivityDoc;

import java.util.Map;

/**
 * @since 4.4
 */
public class QProfileActivity extends ActivityDoc implements Activity {

  protected QProfileActivity(Map<String, Object> fields) {
    super(fields);
  }

  public String profileKey(){
    // TODO
    return null;
  }

  public RuleKey ruleKey(){
    return RuleKey.parse((String) getField("details.ruleKey"));
  }

  public String ruleName(){
    // TODO
    return null;
  }

  public String authorName(){
    // TODO
    return null;
  }

  public String severity(){
    return (String) getField("details.severity");
  }

  public Map<String, String> parameters(){
    // TODO
    return ImmutableMap.of();
  }

}
