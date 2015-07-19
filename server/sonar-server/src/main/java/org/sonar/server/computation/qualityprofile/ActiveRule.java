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
package org.sonar.server.computation.qualityprofile;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;

@Immutable
public class ActiveRule {
  private final RuleKey ruleKey;
  private final String severity;
  private final Map<String, String> params;

  public ActiveRule(RuleKey ruleKey, String severity, Map<String, String> params) {
    this.ruleKey = ruleKey;
    this.severity = severity;
    this.params = ImmutableMap.copyOf(params);
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
}
