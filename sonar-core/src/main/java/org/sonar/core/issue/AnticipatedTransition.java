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
package org.sonar.core.issue;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.Trackable;

public class AnticipatedTransition implements Trackable {

  private final String uuid;
  private final String projectKey;
  private final String transition;
  private final String userUuid;
  private final String comment;
  private final String filePath;
  private final Integer line;
  private final String message;
  private final String lineHash;
  private final RuleKey ruleKey;

  public AnticipatedTransition(
    @Nullable String uuid,
    String projectKey,
    String userUuid,
    @Nullable RuleKey ruleKey,
    @Nullable String message,
    @Nullable String filePath,
    @Nullable Integer line,
    @Nullable String lineHash,
    String transition,
    @Nullable String comment) {
    this.uuid = uuid;
    this.projectKey = projectKey;
    this.transition = transition;
    this.userUuid = userUuid;
    this.comment = comment;
    this.filePath = filePath;
    this.line = line;
    this.message = message;
    this.lineHash = lineHash;
    this.ruleKey = ruleKey;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public String getTransition() {
    return transition;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public String getComment() {
    return comment;
  }

  public String getFilePath() {
    return filePath;
  }

  @Nullable
  @Override
  public Integer getLine() {
    return line;
  }

  @Nullable
  @Override
  public String getMessage() {
    return message;
  }

  @Nullable
  @Override
  public String getLineHash() {
    return lineHash;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  @Override
  public String getStatus() {
    // Since it's an anticipated transition, the issue will always be open upon the first analysis.
    return Issue.STATUS_OPEN;
  }

  @Override
  public Date getUpdateDate() {
    return Date.from(Instant.now());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnticipatedTransition that = (AnticipatedTransition) o;
    return Objects.equals(projectKey, that.projectKey)
      && Objects.equals(transition, that.transition)
      && Objects.equals(userUuid, that.userUuid)
      && Objects.equals(comment, that.comment)
      && Objects.equals(filePath, that.filePath)
      && Objects.equals(line, that.line)
      && Objects.equals(message, that.message)
      && Objects.equals(lineHash, that.lineHash)
      && Objects.equals(ruleKey, that.ruleKey);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public String getUuid() {
    return uuid;
  }
}
