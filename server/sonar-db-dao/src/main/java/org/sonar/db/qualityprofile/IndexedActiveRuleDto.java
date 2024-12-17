/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.qualityprofile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class IndexedActiveRuleDto {
  private static final Gson GSON = new Gson();
  private static final TypeToken<Map<SoftwareQuality, Severity>> TYPE = new TypeToken<>() {
  };

  private String uuid;
  private String ruleUuid;
  private int severity;
  private String inheritance;
  private String repository;
  private String key;
  private String impactsString;
  private String ruleProfileUuid;
  private Boolean prioritizedRule;

  public IndexedActiveRuleDto() {
    // nothing to do here
  }

  public String getUuid() {
    return uuid;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  public int getSeverity() {
    return severity;
  }

  @CheckForNull
  public String getInheritance() {
    return inheritance;
  }

  public String getRepository() {
    return repository;
  }

  public String getKey() {
    return key;
  }

  public Map<SoftwareQuality, Severity> getImpacts() {
    return impactsString != null ? GSON.fromJson(impactsString, TYPE) : Map.of();
  }

  public String getRuleProfileUuid() {
    return ruleProfileUuid;
  }

  public Boolean getPrioritizedRule() {
    return prioritizedRule;
  }
}
