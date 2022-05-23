/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.report;

import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.SeverityUtil;

public class QualityProfileFindingDto {
  private String language = null;
  private String title = null;
  private String repository = null;
  private String ruleKey = null;
  private String status = null;
  private Integer type = null;
  private Integer severity = null;

  public String getLanguage() {
    return language;
  }

  public String getTitle() {
    return title;
  }

  public RuleKey getReferenceKey() {
    return RuleKey.of(repository, ruleKey);
  }

  public RuleStatus getStatus() {
    return RuleStatus.valueOf(status);
  }

  public RuleType getType() {
    return RuleType.valueOf(type);
  }

  @CheckForNull
  public String getSeverity() {
    return SeverityUtil.getSeverityFromOrdinal(severity);
  }
}
