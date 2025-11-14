/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.io.Serializable;
import org.sonar.core.util.UuidFactory;

public final class NewCodeReferenceIssueDto implements Serializable {
  private String uuid;
  private String issueKey;

  // technical date
  private Long createdAt;

  public NewCodeReferenceIssueDto() {
    // nothing to do
  }

  public String getUuid() {
    return uuid;
  }

  public NewCodeReferenceIssueDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getIssueKey() {
    return issueKey;
  }

  public NewCodeReferenceIssueDto setIssueKey(String issueKey) {
    this.issueKey = issueKey;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public NewCodeReferenceIssueDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public static NewCodeReferenceIssueDto fromIssueDto(IssueDto issue, long now, UuidFactory uuidFactory) {
    return new NewCodeReferenceIssueDto()
      .setUuid(uuidFactory.create())
      .setIssueKey(issue.getKey())
      .setCreatedAt(now);
  }

  public static NewCodeReferenceIssueDto fromIssueKey(String issueKey, long now, UuidFactory uuidFactory) {
    return new NewCodeReferenceIssueDto()
      .setUuid(uuidFactory.create())
      .setIssueKey(issueKey)
      .setCreatedAt(now);
  }
}
