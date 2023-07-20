/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.issue;

import javax.annotation.Nullable;
import org.sonar.core.issue.AnticipatedTransition;

public class AnticipatedTransitionDto {
  private String uuid;
  private String projectUuid;
  private String userUuid;
  private String transition;
  private String status;
  private String comment;
  private Integer line;
  private String message;
  private String lineHash;
  private String ruleKey;
  // TODO: private String filePath
  // TODO: private Instant createdAt


  public AnticipatedTransitionDto(
    String uuid,
    String projectUuid,
    String userUuid,
    String transition,
    String status,
    @Nullable String comment,
    @Nullable Integer line,
    @Nullable String message,
    @Nullable String lineHash,
    String ruleKey) {
    this.uuid = uuid;
    this.projectUuid = projectUuid;
    this.userUuid = userUuid;
    this.transition = transition;
    this.status = status;
    this.comment = comment;
    this.line = line;
    this.message = message;
    this.lineHash = lineHash;
    this.ruleKey = ruleKey;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public void setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public void setUserUuid(String userUuid) {
    this.userUuid = userUuid;
  }

  public String getTransition() {
    return transition;
  }

  public void setTransition(String transition) {
    this.transition = transition;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Integer getLine() {
    return line;
  }

  public void setLine(Integer line) {
    this.line = line;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getLineHash() {
    return lineHash;
  }

  public void setLineHash(String lineHash) {
    this.lineHash = lineHash;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
  }

  public static AnticipatedTransitionDto toDto(AnticipatedTransition anticipatedTransition, String uuid, String projectUuid) {
    return new AnticipatedTransitionDto(
      uuid,
      projectUuid,
      anticipatedTransition.getUserUuid(),
      anticipatedTransition.getTransition(),
      anticipatedTransition.getStatus(),
      anticipatedTransition.getComment(),
      anticipatedTransition.getLine(),
      anticipatedTransition.getMessage(),
      anticipatedTransition.getLineHash(),
      anticipatedTransition.getRuleKey().toString());
  }
}
