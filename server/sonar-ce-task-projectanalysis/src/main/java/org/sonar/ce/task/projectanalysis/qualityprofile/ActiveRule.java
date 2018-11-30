/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;

@Immutable
public class ActiveRule {
  private final RuleKey ruleKey;
  private final String severity;
  private final Map<String, String> params;
  private final String pluginKey;
  private final long updatedAt;
  private final String qProfileKey;

  public ActiveRule(RuleKey ruleKey, String severity, Map<String, String> params, long updatedAt, @Nullable String pluginKey, String qProfileKey) {
    this.ruleKey = ruleKey;
    this.severity = severity;
    this.pluginKey = pluginKey;
    this.params = ImmutableMap.copyOf(params);
    this.updatedAt = updatedAt;
    this.qProfileKey = qProfileKey;
  }

  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public String getSeverity() {
    return severity;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  @CheckForNull
  public String getPluginKey() {
    return pluginKey;
  }

  public String getQProfileKey() {
    return qProfileKey;
  }
}
